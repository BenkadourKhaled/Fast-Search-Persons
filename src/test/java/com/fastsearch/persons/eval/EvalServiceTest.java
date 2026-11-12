package com.fastsearch.persons.eval;

import com.fastsearch.persons.names.NameDictionary;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalServiceTest {

    @Test
    void evaluation_synthetique_reproductible_et_comparable() {
        EvalService service = new EvalService(NameDictionary.loadDefault(),
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC),
                100, 500, true, null, "persons");
        EvalReport report = service.run(42, 24);

        assertEquals(42, report.seed());
        assertEquals(27, report.personCount());
        assertEquals(147, report.caseCount());
        assertEquals(10, report.topK());
        assertNotNull(report.lucene());
        assertTrue(report.lucene().recallAt10() >= 0.80,
                "Recall@10 trop faible : " + report.lucene().recallAt10());
        assertTrue(report.lucene().mrr() >= 0.70,
                "MRR trop faible : " + report.lucene().mrr());
        assertTrue(report.lucene().precisionAt1() >= 0.70,
                "Précision@1 trop faible : " + report.lucene().precisionAt1());
        assertEquals(0, report.lucene().falseStrong(),
                "Aucun faux positif FORT attendu sur le jeu synthétique");
        assertTrue(report.lucene().byCategory().containsKey("arabic"));
        assertTrue(report.lucene().byCategory().containsKey("typo"));
        assertTrue(report.lucene().byCategory().containsKey("homonym"));

        assertEquals(2, report.baselines().size());
        BaselineResult sql = report.baselines().get(0);
        assertTrue(sql.available());
        assertTrue(sql.metrics().mrr() <= report.lucene().mrr(),
                "La baseline SQL ne doit pas battre le moteur");
        BaselineResult openSearch = report.baselines().get(1);
        assertFalse(openSearch.available());
        assertNotNull(openSearch.note());
        assertFalse(report.suggestions().isEmpty(), "Les fautes de frappe doivent produire des suggestions");
    }
}
