package com.fastsearch.persons.search;

import java.util.List;
import java.util.Set;

/** Réponse de recherche. Le message distingue l'absence de résultat fiable des résultats faibles. */
public record SearchResponse(
        List<SearchHit> hits,
        long totalHits,
        int page,
        int size,
        boolean reliable,
        String message,
        Set<String> warnings
) {

    public SearchResponse {
        hits = List.copyOf(hits);
        warnings = warnings == null ? Set.of() : Set.copyOf(warnings);
    }

    public static SearchResponse empty(String message) {
        return new SearchResponse(List.of(), 0, 0, 0, false, message, Set.of());
    }
}
