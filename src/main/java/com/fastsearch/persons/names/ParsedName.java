package com.fastsearch.persons.names;

import java.util.List;

/** Résultat du découpage d'une chaîne libre. */
public record ParsedName(String raw, List<String> tokens, List<NameHypothesis> hypotheses) {

    public ParsedName {
        tokens = List.copyOf(tokens);
        hypotheses = List.copyOf(hypotheses);
    }

    public boolean ambiguous() {
        return hypotheses.size() > 1;
    }

    public boolean hasMeaningfulToken() {
        return tokens.stream().anyMatch(t -> !NameNormalizer.isParticle(t) && NameNormalizer.normalizeToken(t).length() >= 2);
    }
}
