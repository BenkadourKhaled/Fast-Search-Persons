package com.fastsearch.persons.search;

/** Explication d'un champ, sans conclusion d'identité. */
public record FieldExplanation(String field, String level, String detail, double score) {
}
