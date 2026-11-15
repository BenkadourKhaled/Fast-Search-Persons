package com.fastsearch.persons.search;

import java.util.List;

/** Score final et explications d'un candidat. */
public record ScoreResult(
        double score,
        Confidence confidence,
        List<FieldExplanation> explanations,
        double nameScore,
        double dateScore,
        double placeScore,
        boolean coarseUsed,
        boolean dictionaryConfirmed,
        boolean crossScript,
        boolean idMatch
) {

    public ScoreResult {
        explanations = List.copyOf(explanations);
    }
}
