package com.fastsearch.persons.eval;

import com.fastsearch.persons.search.Confidence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Calculs Recall@K, MRR, nDCG@K, précision@1 et faux positifs FORT. */
final class MetricsCalculator {

    private MetricsCalculator() {
    }

    static EvalMetrics compute(List<EvalOutcome> outcomes, int topK) {
        int k = Math.max(1, topK);
        List<CaseScore> scores = new ArrayList<>(outcomes.size());
        for (EvalOutcome outcome : outcomes) {
            scores.add(score(outcome, k));
        }
        Map<String, List<CaseScore>> byCategory = new LinkedHashMap<>();
        for (CaseScore score : scores) {
            byCategory.computeIfAbsent(score.category(), key -> new ArrayList<>()).add(score);
        }
        Map<String, EvalMetrics.CategoryMetrics> categories = new LinkedHashMap<>();
        for (Map.Entry<String, List<CaseScore>> entry : byCategory.entrySet()) {
            List<CaseScore> values = entry.getValue();
            categories.put(entry.getKey(), new EvalMetrics.CategoryMetrics(
                    values.size(), average(values, CaseScore::recall), average(values, CaseScore::mrr),
                    average(values, CaseScore::ndcg), average(values, CaseScore::precisionAt1),
                    sum(values, CaseScore::strongHits), sum(values, CaseScore::falseStrong)));
        }
        return new EvalMetrics(k, scores.size(),
                average(scores, CaseScore::recall),
                average(scores, CaseScore::mrr),
                average(scores, CaseScore::ndcg),
                average(scores, CaseScore::precisionAt1),
                sum(scores, CaseScore::strongHits),
                sum(scores, CaseScore::falseStrong),
                categories);
    }

    private static CaseScore score(EvalOutcome outcome, int k) {
        Set<String> expected = outcome.expectedIds();
        List<String> returned = outcome.returnedIds();
        int limit = Math.min(k, returned.size());
        int firstRelevant = -1;
        int found = 0;
        double dcg = 0.0;
        for (int i = 0; i < limit; i++) {
            boolean relevant = expected.contains(returned.get(i));
            if (relevant) {
                found++;
                if (firstRelevant < 0) {
                    firstRelevant = i + 1;
                }
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2));
            }
        }
        int idealCount = Math.min(k, expected.size());
        double idcg = 0.0;
        for (int i = 1; i <= idealCount; i++) {
            idcg += 1.0 / (Math.log(i + 1) / Math.log(2));
        }
        double recall = expected.isEmpty() ? 0.0 : (double) found / expected.size();
        double mrr = firstRelevant < 0 ? 0.0 : 1.0 / firstRelevant;
        double ndcg = idcg == 0.0 ? 0.0 : dcg / idcg;
        double precisionAt1 = !returned.isEmpty() && expected.contains(returned.get(0)) ? 1.0 : 0.0;
        int strongHits = 0;
        int falseStrong = 0;
        for (int i = 0; i < limit; i++) {
            if (outcome.confidences().get(i) == Confidence.FORT) {
                strongHits++;
                if (!expected.contains(returned.get(i))) {
                    falseStrong++;
                }
            }
        }
        return new CaseScore(outcome.category(), recall, mrr, ndcg, precisionAt1,
                strongHits, falseStrong);
    }

    private static double average(List<CaseScore> scores,
                                  java.util.function.ToDoubleFunction<CaseScore> extractor) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (CaseScore score : scores) {
            total += extractor.applyAsDouble(score);
        }
        return total / scores.size();
    }

    private static int sum(List<CaseScore> scores,
                           java.util.function.ToIntFunction<CaseScore> extractor) {
        int total = 0;
        for (CaseScore score : scores) {
            total += extractor.applyAsInt(score);
        }
        return total;
    }

    private record CaseScore(
            String category,
            double recall,
            double mrr,
            double ndcg,
            double precisionAt1,
            int strongHits,
            int falseStrong
    ) {
    }
}
