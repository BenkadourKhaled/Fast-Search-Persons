package com.fastsearch.persons.eval;

import java.time.Instant;
import java.util.List;

/** Rapport d'évaluation complet, reproductible par graine. */
public record EvalReport(
        Instant generatedAt,
        long seed,
        int personCount,
        int caseCount,
        int topK,
        EvalMetrics lucene,
        List<BaselineResult> baselines,
        List<DictionarySuggestion> suggestions
) {

    public EvalReport {
        baselines = List.copyOf(baselines);
        suggestions = List.copyOf(suggestions);
    }
}
