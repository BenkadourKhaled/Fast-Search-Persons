package com.fastsearch.persons.search;

import java.util.List;

/** Résultat proposé à l'utilisateur : toujours un candidat. */
public record SearchHit(
        PersonRecord person,
        double score,
        Confidence confidence,
        List<FieldExplanation> explanations,
        float luceneScore
) {

    public SearchHit {
        explanations = List.copyOf(explanations);
    }
}
