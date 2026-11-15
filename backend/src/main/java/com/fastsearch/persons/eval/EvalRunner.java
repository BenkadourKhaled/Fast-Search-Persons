package com.fastsearch.persons.eval;

import com.fastsearch.persons.search.SearchHit;
import com.fastsearch.persons.search.SearchRequest;
import com.fastsearch.persons.search.SearchResponse;
import com.fastsearch.persons.search.SearchService;

import java.util.ArrayList;
import java.util.List;

/** Exécute les cas synthétiques contre le moteur et calcule les métriques. */
public final class EvalRunner {

    public EvalMetrics evaluate(SyntheticDataset dataset, SearchService searchService, int topK) {
        int k = Math.max(1, topK);
        List<EvalOutcome> outcomes = new ArrayList<>(dataset.cases().size());
        for (EvalCase evalCase : dataset.cases()) {
            SearchRequest request = withSize(evalCase.request(), k);
            SearchResponse response = searchService.search(request);
            List<String> ids = new ArrayList<>();
            List<com.fastsearch.persons.search.Confidence> confidences = new ArrayList<>();
            for (SearchHit hit : response.hits()) {
                ids.add(hit.person().id());
                confidences.add(hit.confidence());
            }
            outcomes.add(new EvalOutcome(evalCase.id(), evalCase.category(),
                    evalCase.expectedIds(), ids, confidences));
        }
        return MetricsCalculator.compute(outcomes, k);
    }

    private static SearchRequest withSize(SearchRequest request, int size) {
        return new SearchRequest(request.lastName(), request.firstName(), request.birthDate(),
                request.birthPlace(), request.q(), request.ownerGroups(), request.page(), size,
                request.includeExplanation());
    }
}
