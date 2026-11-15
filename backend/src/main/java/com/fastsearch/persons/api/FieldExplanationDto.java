package com.fastsearch.persons.api;

import com.fastsearch.persons.search.FieldExplanation;

public record FieldExplanationDto(String field, String level, String detail, double score) {

    public static FieldExplanationDto from(FieldExplanation explanation) {
        return new FieldExplanationDto(explanation.field(), explanation.level(),
                explanation.detail(), explanation.score());
    }
}
