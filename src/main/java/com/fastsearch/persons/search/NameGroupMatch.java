package com.fastsearch.persons.search;

import java.util.List;

/** Résultat de comparaison d'un groupe de noms (nom, prénom ou nom complet). */
public record NameGroupMatch(
        double score,
        NameMatchLevel level,
        boolean strong,
        boolean coarseUsed,
        boolean dictionaryConfirmed,
        boolean crossScript,
        List<TokenMatch> tokenMatches
) {

    public NameGroupMatch {
        tokenMatches = List.copyOf(tokenMatches);
    }

    public static NameGroupMatch none() {
        return new NameGroupMatch(0, NameMatchLevel.AUCUN, false, false, false, false, List.of());
    }
}
