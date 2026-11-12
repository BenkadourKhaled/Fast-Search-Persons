package com.fastsearch.persons.config;

import com.fastsearch.persons.eval.EvalService;
import com.fastsearch.persons.names.NameDictionary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Clock;

/** Câblage de l'évaluation synthétique. */
@Configuration
public class EvalConfiguration {

    @Bean
    public EvalService evalService(NameDictionary dictionary, Clock clock,
                                   FastSearchProperties properties) {
        FastSearchProperties.Eval eval = properties.getEval();
        URI endpoint = eval.getOpenSearchUrl().isBlank()
                ? null : URI.create(eval.getOpenSearchUrl());
        return new EvalService(dictionary, clock, properties.getSearch().getMaxPageSize(),
                properties.getSearch().getCandidateLimit(),
                properties.getSearch().isEnableCoarse(), endpoint, eval.getOpenSearchIndex());
    }
}
