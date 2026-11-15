package com.fastsearch.persons.eval;

import com.fastsearch.persons.search.SearchRequest;

import java.util.Set;

/** Un cas d'évaluation : requête, catégorie et identifiants attendus. */
public record EvalCase(
        String id,
        String category,
        SearchRequest request,
        Set<String> expectedIds
) {

    public EvalCase {
        expectedIds = Set.copyOf(expectedIds);
    }
}
