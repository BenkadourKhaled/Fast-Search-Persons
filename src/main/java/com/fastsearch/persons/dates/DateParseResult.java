package com.fastsearch.persons.dates;

import java.util.List;

/** Résultat d'analyse d'une date : hypothèses ordonnées + motifs de rejet. */
public record DateParseResult(String input, List<ParsedDate> hypotheses, List<String> errors) {

    public DateParseResult {
        hypotheses = List.copyOf(hypotheses);
        errors = List.copyOf(errors);
    }

    public boolean isValid() {
        return !hypotheses.isEmpty();
    }

    public ParsedDate primary() {
        return hypotheses.isEmpty() ? null : hypotheses.get(0);
    }
}
