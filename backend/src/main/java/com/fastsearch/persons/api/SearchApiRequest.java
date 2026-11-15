package com.fastsearch.persons.api;

import com.fastsearch.persons.search.SearchRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Corps de recherche. Les groupes autorisés ne font pas partie du contrat client. */
public record SearchApiRequest(
        @Size(max = 200) String q,
        @Size(max = 100) String lastName,
        @Size(max = 100) String firstName,
        @Size(max = 32) String birthDate,
        @Size(max = 100) String birthPlace,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        Boolean includeExplanation
) {

    public SearchRequest toSearchRequest(Set<String> ownerGroups) {
        return new SearchRequest(
                trim(lastName), trim(firstName), trim(birthDate), trim(birthPlace), trim(q),
                ownerGroups, page == null ? 0 : page, size == null ? 20 : size,
                includeExplanation == null || includeExplanation);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
