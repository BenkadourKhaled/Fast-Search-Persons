package com.fastsearch.persons.api;

import com.fastsearch.persons.search.SearchResponse;

import java.util.List;
import java.util.Set;

public record SearchApiResponse(
        List<SearchHitDto> hits,
        long total,
        int page,
        int size,
        boolean reliable,
        String message,
        Set<String> warnings
) {

    public static SearchApiResponse from(SearchResponse response, boolean includeExplanation) {
        return new SearchApiResponse(
                response.hits().stream().map(hit -> SearchHitDto.from(hit, includeExplanation)).toList(),
                response.totalHits(), response.page(), response.size(), response.reliable(),
                response.message(), response.warnings());
    }
}
