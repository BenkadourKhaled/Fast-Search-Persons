package com.fastsearch.persons.search;

/** Niveau d'un rapprochement de nom. */
public enum NameMatchLevel {
    EXACT,
    TRANSLITTERE,
    PHONETIQUE_FINE,
    PHONETIQUE_GROSSIERE,
    FLOU,
    AUCUN
}
