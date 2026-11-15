package com.fastsearch.persons.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameAnalyzerTest {

    @Test
    void particules_retirees_mais_pas_les_lettres_des_noms() throws IOException {
        assertEquals(List.of("benali", "karim"), tokens("BENALI Karim"));
        assertEquals(List.of("abd", "kader"), tokens("Abd El Kader"));
        assertEquals(List.of("ali"), tokens("Ben Ali"));
    }

    @Test
    void arabe_normalise_sans_stemming() throws IOException {
        assertEquals(List.of("محمد"), tokens("مُحَمَّد"));
        assertEquals(List.of("كريم", "علي"), tokens("كريم بن علي"));
    }

    @Test
    void accents_latins_replies() throws IOException {
        assertEquals(List.of("lounes"), tokens("Lounès"));
        assertEquals(List.of("fatima"), tokens("Fatimá"));
    }

    private List<String> tokens(String text) throws IOException {
        List<String> result = new ArrayList<>();
        try (Analyzer analyzer = new NameAnalyzer();
             TokenStream stream = analyzer.tokenStream("name", text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                result.add(term.toString());
            }
            stream.end();
        }
        return result;
    }
}
