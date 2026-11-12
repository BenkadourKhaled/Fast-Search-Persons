package com.fastsearch.persons.config;

import com.fastsearch.persons.dates.DateMatcher;
import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.names.FullNameParser;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameVariantGenerator;
import com.fastsearch.persons.search.CandidateScorer;
import com.fastsearch.persons.search.IndexManager;
import com.fastsearch.persons.search.LuceneQueryBuilder;
import com.fastsearch.persons.search.QueryParser;
import com.fastsearch.persons.search.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/** Câblage des composants autonomes de recherche. */
@Configuration
public class SearchConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public NameDictionary nameDictionary(FastSearchProperties properties) {
        if (properties.getDictionaryPath().isBlank()) {
            return NameDictionary.loadDefault();
        }
        return NameDictionary.load(Path.of(properties.getDictionaryPath()));
    }

    @Bean
    public DateParser dateParser(Clock clock) {
        return new DateParser(clock);
    }

    @Bean
    public FullNameParser fullNameParser() {
        return new FullNameParser();
    }

    @Bean
    public QueryParser queryParser(DateParser dateParser, FullNameParser fullNameParser,
                                   FastSearchProperties properties) {
        return new QueryParser(dateParser, fullNameParser, properties.getSearch().getMaxPageSize());
    }

    @Bean
    public NameVariantGenerator nameVariantGenerator(NameDictionary dictionary) {
        return new NameVariantGenerator(dictionary);
    }

    @Bean
    public LuceneQueryBuilder luceneQueryBuilder(NameVariantGenerator variants,
                                                 FastSearchProperties properties) {
        return new LuceneQueryBuilder(variants, properties.getSearch().isEnableCoarse());
    }

    @Bean
    public DateMatcher dateMatcher() {
        return new DateMatcher();
    }

    @Bean
    public CandidateScorer candidateScorer(NameDictionary dictionary, DateMatcher dateMatcher,
                                           FastSearchProperties properties) {
        return new CandidateScorer(dictionary, dateMatcher,
                properties.getSearch().isEnableCoarse());
    }

    @Bean(destroyMethod = "close")
    public IndexManager indexManager(FastSearchProperties properties,
                                     NameDictionary dictionary) throws IOException {
        return new IndexManager(Path.of(properties.getIndex().getPath()), dictionary);
    }

    @Bean
    public SearchService searchService(IndexManager indexManager, QueryParser queryParser,
                                       LuceneQueryBuilder queryBuilder, CandidateScorer scorer,
                                       FastSearchProperties properties) {
        return new SearchService(indexManager, queryParser, queryBuilder, scorer,
                properties.getSearch().getCandidateLimit());
    }
}
