package com.fastsearch.persons.search;

import com.fastsearch.persons.dates.DateMatcher;
import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.names.FullNameParser;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameVariantGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {

    @TempDir
    Path tempDir;

    private IndexManager indexManager;
    private SearchService searchService;
    private NameDictionary dictionary;

    @BeforeEach
    void setUp() throws Exception {
        dictionary = NameDictionary.loadDefault();
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        DateParser dateParser = new DateParser(clock);
        indexManager = new IndexManager(tempDir.resolve("index"), dictionary);
        List<PersonRecord> people = List.of(
                person("1", "Benali", "Walid", "1992-04-07", "A"),
                person("2", "بن علي", "وليد", "1992-04-07", "A"),
                person("3", "Ould", "Walid", "1980-01-01", "A"),
                person("4", "Benali", "Walid", "1995-09-20", "A"),
                person("5", "Benali", "Karim", "1992-04-07", "A"),
                person("6", "Protégé", "Secret", "1990-01-01", "B"),
                person("7", "Bouzid", "Amine", "1985-01-01", "A"),
                person("8", "Benali", "Walid", "1992-04-17", "A"),
                person("9", "Lounes", "Mokrane", "1990-01-01", "A"),
                person("10", "Benali", "Walid", "1993-04-07", "A"),
                person("11", "بن علي", "كريم", "1992-04-07", "A")
        );
        indexManager.indexAll(people);
        indexManager.commit();
        QueryParser queryParser = new QueryParser(dateParser, new FullNameParser(), 100);
        LuceneQueryBuilder builder = new LuceneQueryBuilder(new NameVariantGenerator(dictionary), true);
        CandidateScorer scorer = new CandidateScorer(dictionary, new DateMatcher(), true);
        searchService = new SearchService(indexManager, queryParser, builder, scorer, 500);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (indexManager != null) {
            indexManager.close();
        }
    }

    private PersonRecord person(String id, String last, String first, String date, String group) {
        return PersonRecord.builder()
                .id(id).lastName(last).firstName(first)
                .birthDate(LocalDate.parse(date)).birthPrecision(DatePrecision.JOUR)
                .birthFlags(Set.of()).ownerGroup(group).lang("fr")
                .build();
    }

    @Test
    void oualid_benali_trouve_latin_et_arabe() {
        SearchResponse response = searchService.search(SearchRequest.free(
                "Oualid Benali", Set.of("A"), 0, 20, true));
        assertFalse(response.hits().isEmpty());
        assertEquals("1", response.hits().get(0).person().id());
        assertTrue(response.hits().stream().anyMatch(h -> h.person().id().equals("2")));
        assertEquals(Confidence.FORT, response.hits().get(0).confidence());
    }

    @Test
    void walid_ne_classe_pas_ould_au_meme_niveau() {
        SearchResponse response = searchService.search(SearchRequest.free(
                "Walid", Set.of("A"), 0, 20, true));
        SearchHit walid = response.hits().stream().filter(h -> h.person().id().equals("1"))
                .findFirst().orElseThrow();
        SearchHit ould = response.hits().stream().filter(h -> h.person().id().equals("3"))
                .findFirst().orElseThrow();
        assertTrue(walid.score() > ould.score());
        assertTrue(walid.confidence().ordinal() < ould.confidence().ordinal(),
                "FORT/MOYEN/FAIBLE : Walid doit être strictement mieux classé que Ould");
    }

    @Test
    void date_exacte_classe_avant_date_differente() {
        SearchRequest request = SearchRequest.structured("Benali", "Walid", "07/04/1992", "",
                Set.of("A"), 0, 20);
        SearchResponse response = searchService.search(request);
        assertEquals("1", response.hits().get(0).person().id());
        SearchHit different = response.hits().stream().filter(h -> h.person().id().equals("4"))
                .findFirst().orElseThrow();
        assertTrue(response.hits().get(0).score() > different.score());
        assertFalse(different.confidence() == Confidence.FORT);
    }

    @Test
    void date_compacte_et_separee_donnent_le_meme_premier_resultat() {
        String topCompact = searchService.search(SearchRequest.free(
                "07041992", Set.of("A"), 0, 20, true)).hits().get(0).person().id();
        String topSeparated = searchService.search(SearchRequest.free(
                "07/04/1992", Set.of("A"), 0, 20, true)).hits().get(0).person().id();
        assertEquals(topCompact, topSeparated);
    }

    @Test
    void homonyme_date_tres_differente_jamais_fort() {
        SearchRequest request = SearchRequest.structured("Benali", "Walid", "07/04/1992", "",
                Set.of("A"), 0, 20);
        SearchResponse response = searchService.search(request);
        SearchHit homonym = response.hits().stream().filter(h -> h.person().id().equals("4"))
                .findFirst().orElseThrow();
        assertFalse(homonym.confidence() == Confidence.FORT);
    }

    @Test
    void faute_de_frappe_toleree() {
        SearchResponse response = searchService.search(SearchRequest.free(
                "Walid Benaly", Set.of("A"), 0, 20, true));
        assertFalse(response.hits().isEmpty());
        assertEquals("1", response.hits().get(0).person().id());
    }

    @Test
    void inversion_et_arabe_retrouvent_la_meme_personne() {
        SearchResponse latin = searchService.search(SearchRequest.structured(
                "BENALI", "Karim", "", "", Set.of("A"), 0, 20));
        SearchResponse arabe = searchService.search(SearchRequest.free(
                "كريم بن علي", Set.of("A"), 0, 20, true));
        assertEquals("5", latin.hits().get(0).person().id());
        assertEquals("5", arabe.hits().get(0).person().id());
        assertTrue(latin.hits().stream().anyMatch(h -> h.person().id().equals("11")));
        assertTrue(arabe.hits().stream().anyMatch(h -> h.person().id().equals("11")));
    }

    @Test
    void groupe_non_autorise_invisible() {
        SearchResponse ownGroup = searchService.search(SearchRequest.free(
                "Protégé Secret", Set.of("A"), 0, 20, true));
        assertTrue(ownGroup.hits().stream().noneMatch(h -> h.person().id().equals("6")));
        SearchResponse otherGroup = searchService.search(SearchRequest.free(
                "Protégé Secret", Set.of("B"), 0, 20, true));
        assertTrue(otherGroup.hits().stream().anyMatch(h -> h.person().id().equals("6")));
    }

    @Test
    void requete_vide_ou_trop_vague_ne_renvoie_pas_la_base() {
        SearchResponse empty = searchService.search(SearchRequest.free("", Set.of("A"), 0, 20, false));
        SearchResponse vague = searchService.search(SearchRequest.free("a", Set.of("A"), 0, 20, false));
        assertTrue(empty.hits().isEmpty());
        assertTrue(vague.hits().isEmpty());
        assertFalse(empty.reliable());
    }

    @Test
    void identifiant_exact() {
        SearchResponse response = searchService.search(SearchRequest.free("1", Set.of("A"), 0, 20, false));
        assertFalse(response.hits().isEmpty());
        assertEquals("1", response.hits().get(0).person().id());
        assertEquals(Confidence.FORT, response.hits().get(0).confidence());
    }

    @Test
    void statistiques_index() {
        IndexManager.IndexStats stats = searchService.stats();
        assertTrue(stats.numDocs() >= 11);
        assertTrue(stats.open());
        assertNotNull(stats.path());
    }
}
