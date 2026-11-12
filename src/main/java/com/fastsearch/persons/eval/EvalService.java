package com.fastsearch.persons.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastsearch.persons.dates.DateMatcher;
import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.names.FullNameParser;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameVariantGenerator;
import com.fastsearch.persons.search.CandidateScorer;
import com.fastsearch.persons.search.IndexManager;
import com.fastsearch.persons.search.LuceneQueryBuilder;
import com.fastsearch.persons.search.PersonRecord;
import com.fastsearch.persons.search.QueryParser;
import com.fastsearch.persons.search.SearchService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Évaluation hors production : construit un index temporaire à partir du jeu
 * synthétique, l'évalue, puis le supprime. L'index de production n'est jamais
 * touché.
 */
public final class EvalService {

    private static final int TOP_K = 10;

    private final NameDictionary dictionary;
    private final Clock clock;
    private final int maxPageSize;
    private final int candidateLimit;
    private final boolean enableCoarse;
    private final URI openSearchEndpoint;
    private final String openSearchIndex;

    public EvalService(NameDictionary dictionary, Clock clock, int maxPageSize,
                       int candidateLimit, boolean enableCoarse,
                       URI openSearchEndpoint, String openSearchIndex) {
        this.dictionary = dictionary;
        this.clock = clock;
        this.maxPageSize = Math.max(10, maxPageSize);
        this.candidateLimit = Math.max(50, candidateLimit);
        this.enableCoarse = enableCoarse;
        this.openSearchEndpoint = openSearchEndpoint;
        this.openSearchIndex = openSearchIndex;
    }

    public EvalReport run(long seed, int requestedCount) {
        SyntheticDataset generated = new SyntheticDatasetGenerator(dictionary)
                .generate(requestedCount);
        List<PersonRecord> persons = new ArrayList<>(generated.persons());
        List<EvalCase> cases = new ArrayList<>(generated.cases());
        Collections.shuffle(persons, new Random(seed));
        Collections.shuffle(cases, new Random(seed));
        SyntheticDataset dataset = new SyntheticDataset(persons, cases);

        Path directory = null;
        IndexManager indexManager = null;
        try {
            directory = Files.createTempDirectory("fastsearch-eval-");
            indexManager = new IndexManager(directory.resolve("index"), dictionary);
            indexManager.indexAll(dataset.persons());
            indexManager.commit();

            DateParser dateParser = new DateParser(clock);
            QueryParser queryParser = new QueryParser(dateParser, new FullNameParser(), maxPageSize);
            LuceneQueryBuilder queryBuilder = new LuceneQueryBuilder(
                    new NameVariantGenerator(dictionary), enableCoarse);
            CandidateScorer scorer = new CandidateScorer(dictionary, new DateMatcher(), enableCoarse);
            SearchService searchService = new SearchService(indexManager, queryParser,
                    queryBuilder, scorer, candidateLimit);

            EvalMetrics lucene = new EvalRunner().evaluate(dataset, searchService, TOP_K);
            List<BaselineResult> baselines = List.of(
                    new SqlLikeBaseline(dataset.persons()).evaluate(dataset, TOP_K),
                    new OpenSearchBaseline(openSearchEndpoint, openSearchIndex,
                            Duration.ofSeconds(5), new ObjectMapper()).evaluate(dataset, TOP_K));
            List<DictionarySuggestion> suggestions =
                    new DictionarySuggestionEngine(dictionary).suggest(dataset);
            return new EvalReport(clock.instant(), seed, dataset.persons().size(),
                    dataset.cases().size(), TOP_K, lucene, baselines, suggestions);
        } catch (IOException e) {
            throw new IllegalStateException("Évaluation impossible : accès à l'index temporaire", e);
        } finally {
            if (indexManager != null) {
                try {
                    indexManager.close();
                } catch (IOException ignored) {
                    // Nettoyage best effort.
                }
            }
            if (directory != null) {
                deleteRecursively(directory);
            }
        }
    }

    private static void deleteRecursively(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Un fichier temporaire verrouillé ne doit pas masquer le rapport.
                }
            });
        } catch (IOException ignored) {
            // Nettoyage best effort.
        }
    }
}
