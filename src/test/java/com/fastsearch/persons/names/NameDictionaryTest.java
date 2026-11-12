package com.fastsearch.persons.names;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameDictionaryTest {

    private final NameDictionary dictionary = NameDictionary.loadDefault();

    @Test
    void ponts_dictionnaire_demandes() {
        assertTrue(dictionary.sameEntry("وليد", "Walid"));
        assertTrue(dictionary.sameEntry("محمد", "Mohamed"));
        assertTrue(dictionary.sameEntry("عبد القادر", "Abdelkader"));
        assertTrue(dictionary.sameEntry("ياسين", "Yacine"));
        assertTrue(dictionary.sameEntry("فاطمة", "Fatima"));
        assertTrue(dictionary.sameEntry("خديجة", "Khadidja"));
        assertTrue(dictionary.sameEntry("بوعلام", "Boualem"));
        assertTrue(dictionary.sameEntry("عبد الرحمان", "Abderrahmane"));
    }

    @Test
    void variantes_par_mot_sans_produit_cartesien() {
        NameVariantGenerator generator = new NameVariantGenerator(dictionary);
        assertTrue(generator.variantsForToken("وليد").contains("oualid"));
        assertTrue(generator.variantsForToken("Walid").contains("وليد"));
        assertTrue(generator.variantsForName("Walid Benali").size() < 96);
    }

    @Test
    void amazigh_latin_sans_equivalent_arabe_force() {
        assertTrue(dictionary.sameEntry("Lounes", "Lounès"));
        assertTrue(dictionary.sameEntry("Lounes", "Lounnes"));
        assertTrue(dictionary.sameEntry("Mokrane", "Mokran"));
        assertFalse(dictionary.sameEntry("Lounes", "Walid"));
    }

    @Test
    void parser_produit_les_deux_ordres() {
        ParsedName parsed = new FullNameParser().parse("BENALI Karim");
        assertEquals(2, parsed.hypotheses().size());
        assertTrue(parsed.hypotheses().stream().anyMatch(h -> h.lastName().equals("BENALI")));
        assertTrue(parsed.hypotheses().stream().anyMatch(h -> h.lastName().equals("Karim")));
    }

    @Test
    void parser_nom_compose() {
        ParsedName parsed = new FullNameParser().parse("Abd El Kader");
        assertTrue(parsed.hypotheses().stream().anyMatch(h -> h.fullName().contains("Abd")));
        assertEquals(List.of("Abd", "El", "Kader"), parsed.tokens());
    }
}
