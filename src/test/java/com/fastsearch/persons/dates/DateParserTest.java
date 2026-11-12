package com.fastsearch.persons.dates;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateParserTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
    private final DateParser parser = new DateParser(clock);

    private ParsedDate primary(String input) {
        DateParseResult result = parser.parse(input);
        assertTrue(result.isValid(), () -> "date rejetée : " + input + " " + result.errors());
        return result.primary();
    }

    @Test
    void toutes_les_formes_du_meme_jour() {
        for (String input : new String[]{
                "07/04/1992", "07-04-1992", "07.04.1992", "07041992",
                "7/4/1992", "٠٧/٠٤/١٩٩٢", "07 avril 1992", "1992-04-07",
                "7-4-92", "07/04-1992", "07 04 1992"}) {
            ParsedDate date = primary(input);
            assertEquals("1992-04-07", date.canonical(), input);
            assertEquals(DatePrecision.JOUR, date.precision(), input);
        }
    }

    @Test
    void compacts_et_pivot_de_siecle() {
        assertEquals("1992-04-07", primary("070492").canonical());
        assertEquals("1999-04-07", primary("070499").canonical());
        DateParseResult r = parser.parse("070412");
        assertEquals("2012-04-07", r.primary().canonical());
        assertTrue(r.hypotheses().stream().anyMatch(d -> d.canonical().equals("1912-04-07")));
        assertTrue(r.primary().hasFlag(DateFlag.SIECLE_DEDUIT));
        assertTrue(r.primary().hasFlag(DateFlag.ANNEE_DEUX_CHIFFRES));
    }

    @Test
    void compacts_invalides_et_lecture_iso_exclusive() {
        assertTrue(parser.parse("31021992").errors().stream().anyMatch(e -> e.contains("invalide")));
        assertTrue(parser.parse("07131992").errors().stream().anyMatch(e -> e.contains("invalide")));
        assertFalse(parser.parse("99999999").isValid());
        assertEquals("1992-04-07", primary("19920407").canonical());
    }

    @Test
    void zero_initial_perdu_et_poids_reduit() {
        ParsedDate date = primary("7041992");
        assertEquals("1992-04-07", date.canonical());
        assertTrue(date.hasFlag(DateFlag.ZERO_INITIAL_PERDU));
        assertTrue(date.weight() < 1.0);
        assertEquals("1992-04-07", primary("70492").canonical());
        assertTrue(primary("70492").hasFlag(DateFlag.ZERO_INITIAL_PERDU));
    }

    @Test
    void inversion_jour_mois() {
        DateParseResult result = parser.parse("07/04/1992");
        assertEquals("1992-04-07", result.primary().canonical());
        assertTrue(result.hypotheses().stream().anyMatch(d -> d.canonical().equals("1992-07-04")));
        DateParseResult nonAmbigu = parser.parse("25/04/1992");
        assertEquals(1, nonAmbigu.hypotheses().size());
        assertTrue(nonAmbigu.hypotheses().stream()
                .noneMatch(d -> d.hasFlag(DateFlag.JOUR_MOIS_HYPOTHESE)));
    }

    @Test
    void mois_en_lettres_arabe_et_francais() {
        assertEquals("1987-03-05", primary("5 mars 1987").canonical());
        assertEquals("1987-03-05", primary("5 مارس 1987").canonical());
        assertEquals("1987-03-05", primary("5 آذار 1987").canonical());
    }

    @Test
    void annee_seule_et_mois_annee() {
        ParsedDate year = primary("1992");
        assertEquals("1992", year.canonical());
        assertEquals(DatePrecision.ANNEE, year.precision());
        ParsedDate month = primary("04/1992");
        assertEquals("1992-04", month.canonical());
        assertEquals(DatePrecision.MOIS, month.precision());
    }

    @Test
    void calendrier_strict_et_dates_futures() {
        assertTrue(parser.parse("29/02/1992").isValid());
        assertFalse(parser.parse("29/02/1993").isValid());
        assertFalse(parser.parse("30/02/1992").isValid());
        assertFalse(parser.parse("00/04/1992").isValid());
        assertFalse(parser.parse("07/13/1992").isValid());
        assertFalse(parser.parse("07/04/2030").isValid());
    }

    @Test
    void dates_conventionnelles_ne_sont_pas_identifiantes() {
        ParsedDate jan1 = primary("01/01/1992");
        DateMatcher matcher = new DateMatcher();
        DateMatchResult result = matcher.match(jan1, jan1);
        assertNotNull(result);
        assertTrue(result.score() <= 0.5);
        assertEquals(DateMatchLevel.HYPOTHESE, result.level());
    }
}
