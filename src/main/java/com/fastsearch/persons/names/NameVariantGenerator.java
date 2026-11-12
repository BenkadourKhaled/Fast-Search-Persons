package com.fastsearch.persons.names;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Génère les graphies à indexer ou à interroger, mot par mot.
 * Pas de produit cartésien : si un nom a 10 variantes par mot et 3 mots,
 * on indexe 30 graphies, pas 1000.
 */
public final class NameVariantGenerator {

    public static final int MAX_VARIANTS_PER_NAME = 96;

    private final NameDictionary dictionary;

    public NameVariantGenerator(NameDictionary dictionary) {
        this.dictionary = dictionary == null ? NameDictionary.loadDefault() : dictionary;
    }

    public Set<String> variantsForToken(String token) {
        return dictionary.variantsForToken(token);
    }

    public Set<String> variantsForName(String fullName) {
        Set<String> variants = new LinkedHashSet<>();
        for (String variant : dictionary.variantsForName(fullName)) {
            if (variants.size() >= MAX_VARIANTS_PER_NAME) {
                break;
            }
            variants.add(variant);
        }
        return Collections.unmodifiableSet(variants);
    }
}
