package com.fastsearch.persons.eval;

import com.fastsearch.persons.search.Confidence;

import java.util.List;
import java.util.Set;

/** Résultat observé pour un cas : identifiants classés et confiances associées. */
public record EvalOutcome(
        String caseId,
        String category,
        Set<String> expectedIds,
        List<String> returnedIds,
        List<Confidence> confidences
) {

    public EvalOutcome {
        expectedIds = Set.copyOf(expectedIds);
        returnedIds = List.copyOf(returnedIds);
        confidences = List.copyOf(confidences);
        if (returnedIds.size() != confidences.size()) {
            throw new IllegalArgumentException("Identifiants et confiances de tailles différentes");
        }
    }
}
