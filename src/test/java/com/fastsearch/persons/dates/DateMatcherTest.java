package com.fastsearch.persons.dates;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateMatcherTest {

    private final DateParser parser = new DateParser(Clock.fixed(
            Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC));
    private final DateMatcher matcher = new DateMatcher();

    private DateMatchResult match(String left, String right) {
        return matcher.match(parser.parse(left).primary(), parser.parse(right).primary());
    }

    @Test
    void exact_inverse_erreur_annee_partielle() {
        assertEquals(DateMatchLevel.EXACTE, match("1992-04-07", "07/04/1992").level());
        assertEquals(DateMatchLevel.JOUR_MOIS_INVERSE, match("1992-04-07", "1992-07-04").level());
        assertEquals(DateMatchLevel.TOLEREE, match("1992-04-07", "1992-04-17").level());
        assertEquals(DateMatchLevel.TOLEREE, match("1992-04-07", "1993-04-07").level());
        assertEquals(DateMatchLevel.PARTIELLE, match("1992-04-07", "1992").level());
    }

    @Test
    void poids_decroissants() {
        double exact = match("1992-04-07", "1992-04-07").score();
        double inverted = match("1992-04-07", "1992-07-04").score();
        double oneDigit = match("1992-04-07", "1992-04-17").score();
        double year = match("1992-04-07", "1993-04-07").score();
        assertTrue(exact > inverted);
        assertTrue(inverted > oneDigit);
        assertTrue(oneDigit > year);
    }

    @Test
    void dates_incompatibles() {
        assertEquals(DateMatchLevel.INCOMPATIBLE, match("1992-04-07", "1998-09-23").level());
        assertEquals(0.0, match("1992-04-07", "1998-09-23").score());
    }
}
