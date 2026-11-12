package com.fastsearch.persons.eval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Métriques globales et par catégorie. */
public record EvalMetrics(
        int topK,
        int caseCount,
        double recallAt10,
        double mrr,
        double ndcgAt10,
        double precisionAt1,
        int strongHits,
        int falseStrong,
        Map<String, CategoryMetrics> byCategory
) {

    public EvalMetrics {
        byCategory = Collections.unmodifiableMap(new LinkedHashMap<>(byCategory));
    }

    public double falseStrongRate() {
        return strongHits == 0 ? 0.0 : (double) falseStrong / strongHits;
    }

    public record CategoryMetrics(
            int caseCount,
            double recallAt10,
            double mrr,
            double ndcgAt10,
            double precisionAt1,
            int strongHits,
            int falseStrong
    ) {
        public double falseStrongRate() {
            return strongHits == 0 ? 0.0 : (double) falseStrong / strongHits;
        }
    }
}
