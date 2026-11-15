package com.fastsearch.persons.search;

import java.util.Set;

/** Requête brute, avant parsing. Les groupes autorisés viennent du serveur, jamais du client. */
public record SearchRequest(
        String lastName,
        String firstName,
        String birthDate,
        String birthPlace,
        String q,
        Set<String> ownerGroups,
        int page,
        int size,
        boolean includeExplanation
) {

    public SearchRequest {
        lastName = lastName == null ? "" : lastName.trim();
        firstName = firstName == null ? "" : firstName.trim();
        birthDate = birthDate == null ? "" : birthDate.trim();
        birthPlace = birthPlace == null ? "" : birthPlace.trim();
        q = q == null ? "" : q.trim();
        ownerGroups = ownerGroups == null ? Set.of() : Set.copyOf(ownerGroups);
        page = Math.max(page, 0);
        size = size <= 0 ? 20 : size;
    }

    public static SearchRequest structured(String lastName, String firstName, String birthDate,
                                           String birthPlace, Set<String> groups, int page, int size) {
        return new SearchRequest(lastName, firstName, birthDate, birthPlace, "", groups, page, size, false);
    }

    public static SearchRequest free(String q, Set<String> groups, int page, int size, boolean explain) {
        return new SearchRequest("", "", "", "", q, groups, page, size, explain);
    }
}
