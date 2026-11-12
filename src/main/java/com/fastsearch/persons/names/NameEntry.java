package com.fastsearch.persons.names;

import java.util.List;

/** Une entrée du dictionnaire : toutes les graphies connues d'un même nom. */
public record NameEntry(List<String> graphies) {

    public NameEntry {
        graphies = List.copyOf(graphies);
        if (graphies.isEmpty()) {
            throw new IllegalArgumentException("Une entrée de dictionnaire doit avoir au moins une graphie");
        }
    }

    public String canonical() {
        return graphies.get(0);
    }
}
