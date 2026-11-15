package com.fastsearch.persons.search;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestration : parsing, candidats Lucene, reranking explicable, pagination.
 * Le score final n'est pas le score Lucene brut : il est recalculé champ par champ.
 */
public final class SearchService {

    private final IndexManager indexManager;
    private final QueryParser queryParser;
    private final LuceneQueryBuilder queryBuilder;
    private final CandidateScorer scorer;
    private final int candidateLimit;

    public SearchService(IndexManager indexManager, QueryParser queryParser,
                         LuceneQueryBuilder queryBuilder, CandidateScorer scorer,
                         int candidateLimit) {
        this.indexManager = indexManager;
        this.queryParser = queryParser;
        this.queryBuilder = queryBuilder;
        this.scorer = scorer;
        this.candidateLimit = Math.max(50, candidateLimit);
    }

    public SearchResponse search(SearchRequest request) {
        ParsedSearchQuery parsed = queryParser.parse(request);
        int page = parsed.page();
        int size = parsed.size();
        if (!parsed.valid()) {
            return new SearchResponse(List.of(), 0, page, size, false,
                    parsed.invalidReason(), parsed.warnings());
        }
        if (parsed.ownerGroups().isEmpty()) {
            return new SearchResponse(List.of(), 0, page, size, false,
                    "aucun groupe de lecture autorisé", parsed.warnings());
        }
        try {
            var query = queryBuilder.build(parsed);
            if (query == null) {
                return new SearchResponse(List.of(), 0, page, size, false,
                        "requête trop vague : aucun critère exploitable", parsed.warnings());
            }
            IndexManager.SearchCandidates candidates = indexManager.search(query, candidateLimit);
            List<Scored> scored = new ArrayList<>();
            for (IndexManager.Candidate candidate : candidates.candidates()) {
                PersonRecord person = indexManager.mapper().fromDocument(candidate.document());
                // Défense en profondeur : le filtre Lucene est déjà appliqué.
                if (!parsed.ownerGroups().contains(person.ownerGroup())) {
                    continue;
                }
                ScoreResult score = scorer.score(parsed, person, candidate.luceneScore());
                scored.add(new Scored(person, score, candidate.luceneScore()));
            }
            // Le score final fait foi. À score égal, l'identifiant source donne un
            // ordre stable et reproductible ; le score Lucene n'est qu'un rappel.
            scored.sort(Comparator
                    .comparingDouble((Scored s) -> s.score().score()).reversed()
                    .thenComparing((Scored s) -> s.person().id(), SearchService::compareIds)
                    .thenComparing(Comparator.comparingDouble((Scored s) -> s.luceneScore()).reversed()));

            List<SearchHit> allHits = new ArrayList<>(scored.size());
            for (Scored s : scored) {
                allHits.add(new SearchHit(s.person(), round(s.score().score()),
                        s.score().confidence(), s.score().explanations(), s.luceneScore()));
            }
            boolean reliable = !allHits.isEmpty()
                    && allHits.get(0).confidence() != Confidence.FAIBLE
                    && allHits.get(0).score() >= 0.60;
            String message;
            if (allHits.isEmpty()) {
                message = "Aucun candidat ne correspond aux critères.";
            } else if (reliable) {
                message = "Candidats proposés ; aucun résultat ne constitue une identification.";
            } else {
                message = "Résultats faibles : aucun candidat fiable, vérification manuelle requise.";
            }
            Set<String> warnings = new LinkedHashSet<>(parsed.warnings());
            if (candidates.totalHits() > candidateLimit) {
                warnings.add("recherche limitée aux " + candidateLimit
                        + " meilleurs candidats Lucene avant reranking");
            }
            if (parsed.numericAmbiguity()) {
                warnings.add("ambiguïté numérique : la date et l'identifiant ont tous deux été recherchés");
            }
            // Seuls les candidats effectivement rerankés sont paginables. Utiliser
            // totalHits Lucene exposerait des pages vides au-delà de candidateLimit.
            int total = allHits.size();
            int totalPages = total == 0 ? 0 : (total + size - 1) / size;
            int effectivePage = totalPages == 0 ? 0 : Math.min(page, totalPages - 1);
            int start = effectivePage * size;
            List<SearchHit> pageHits;
            if (start >= total) {
                pageHits = List.of();
            } else {
                pageHits = allHits.subList(start, Math.min(start + size, total));
            }
            return new SearchResponse(pageHits, total, effectivePage, size, reliable, message, warnings);
        } catch (IOException e) {
            throw new SearchException("Erreur d'accès à l'index Lucene", e);
        }
    }

    public IndexManager.IndexStats stats() {
        try {
            return indexManager.stats();
        } catch (IOException e) {
            throw new SearchException("Erreur de lecture des statistiques d'index", e);
        }
    }

    public void commit() {
        try {
            indexManager.commit();
        } catch (IOException e) {
            throw new SearchException("Erreur de commit de l'index", e);
        }
    }

    public IndexManager indexManager() {
        return indexManager;
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private static int compareIds(String left, String right) {
        if (left.matches("\\d+") && right.matches("\\d+")) {
            int numeric = new BigInteger(left).compareTo(new BigInteger(right));
            if (numeric != 0) {
                return numeric;
            }
        }
        return left.compareTo(right);
    }

    private record Scored(PersonRecord person, ScoreResult score, float luceneScore) {
    }

    public static final class SearchException extends RuntimeException {
        public SearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
