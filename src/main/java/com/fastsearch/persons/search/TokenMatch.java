package com.fastsearch.persons.search;

/** Correspondance d'un jeton de requête avec un jeton candidat. */
public record TokenMatch(
        String queryToken,
        String candidateToken,
        NameMatchLevel level,
        double score,
        boolean dictionaryConfirmed,
        boolean crossScript
) {
}
