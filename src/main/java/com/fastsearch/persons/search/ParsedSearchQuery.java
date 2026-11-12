package com.fastsearch.persons.search;

import com.fastsearch.persons.dates.ParsedDate;
import com.fastsearch.persons.names.NameHypothesis;

import java.util.List;
import java.util.Set;

/** Requête analysée, prête pour la construction Lucene et le reranking. */
public record ParsedSearchQuery(
        List<NameHypothesis> names,
        List<ParsedDate> dates,
        String birthPlace,
        String idQuery,
        String rawQ,
        Set<String> ownerGroups,
        int page,
        int size,
        boolean includeExplanation,
        boolean valid,
        String invalidReason,
        Set<String> warnings,
        boolean numericAmbiguity
) {

    public ParsedSearchQuery {
        names = List.copyOf(names);
        dates = List.copyOf(dates);
        ownerGroups = Set.copyOf(ownerGroups);
        warnings = warnings == null ? Set.of() : Set.copyOf(warnings);
    }

    public boolean hasNames() {
        return !names.isEmpty();
    }

    public boolean hasDates() {
        return !dates.isEmpty();
    }

    public boolean hasId() {
        return idQuery != null && !idQuery.isBlank();
    }

    public boolean dateOnly() {
        return !hasNames() && hasDates() && !hasId();
    }

    public static ParsedSearchQuery invalid(String reason, Set<String> warnings) {
        return new ParsedSearchQuery(List.of(), List.of(), "", "", "", Set.of(), 0, 0,
                false, false, reason, warnings, false);
    }
}
