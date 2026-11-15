package com.fastsearch.persons.dates;

/** Explication d'un score de date. */
public record DateMatchResult(
        double score,
        DateMatchLevel level,
        String detail,
        String queryCanonical,
        String candidateCanonical
) {

    public static DateMatchResult none(String detail) {
        return new DateMatchResult(0.0, DateMatchLevel.INCOMPATIBLE, detail, null, null);
    }
}
