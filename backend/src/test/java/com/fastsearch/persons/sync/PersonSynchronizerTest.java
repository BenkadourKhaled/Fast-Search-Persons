package com.fastsearch.persons.sync;

import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.search.IndexManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonSynchronizerTest {

    @TempDir
    Path tempDir;

    private JdbcDataSource dataSource;
    private IndexManager indexManager;
    private PersonSynchronizer synchronizer;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sync-" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE person_source (
                        id VARCHAR(32) PRIMARY KEY,
                        last_name VARCHAR(100),
                        first_name VARCHAR(100),
                        birth_date DATE,
                        birth_precision VARCHAR(16),
                        birth_flags VARCHAR(100),
                        birth_place VARCHAR(100),
                        father_name VARCHAR(100),
                        mother_name VARCHAR(100),
                        owner_group VARCHAR(32),
                        lang VARCHAR(8),
                        updated_at TIMESTAMP,
                        deleted BOOLEAN DEFAULT FALSE
                    )
                    """);
        }
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        NameDictionary dictionary = NameDictionary.loadDefault();
        indexManager = new IndexManager(tempDir.resolve("index"), dictionary);
        JdbcPersonSource source = new JdbcPersonSource(dataSource,
                SyncTableSpec.standard("person_source"), new DateParser(clock));
        synchronizer = new PersonSynchronizer(source, new LuceneIndexSink(indexManager), clock);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (indexManager != null) {
            indexManager.close();
        }
    }

    @Test
    void synchronisation_complete_par_lots() throws Exception {
        insert("1", "Benali", "Walid", "1992-04-07", "A", "2026-01-01 10:00:00", false);
        insert("2", "Bouzid", "Amine", "1985-01-01", "A", "2026-01-01 10:00:00", false);
        insert("3", "Lounes", "Mokrane", "1990-01-01", "B", "2026-01-01 10:00:00", false);
        insert("4", "O'Neil", "Sean", "1988-03-12", "A", "2026-01-01 10:00:00", false);
        insert("5", "Benali", "Karim", "1992-04-07", "A", "2026-01-01 10:00:00", false);

        SyncReport report = synchronizer.synchronize(SyncRequest.full(2));

        assertEquals("FULL", report.mode());
        assertEquals(5, report.rowsRead());
        assertEquals(5, report.indexed());
        assertEquals(0, report.deleted());
        assertEquals(3, report.batches());
        assertEquals(5, indexManager.stats().numDocs());
        assertEquals("O'Neil", document("4").get("last_name_raw"));
    }

    @Test
    void incremental_ne_prend_que_les_lignes_modifiees_et_supprime_les_logiques() throws Exception {
        insert("1", "Benali", "Walid", "1992-04-07", "A", "2026-01-01 10:00:00", false);
        insert("2", "Bouzid", "Amine", "1985-01-01", "A", "2026-01-01 10:00:00", false);
        insert("3", "Lounes", "Mokrane", "1990-01-01", "B", "2026-01-01 10:00:00", false);
        synchronizer.synchronize(SyncRequest.full(10));

        update("1", "Walid", "2026-02-01 10:00:00", false);
        update("2", "Amine", "2026-02-01 10:00:00", true);

        SyncReport report = synchronizer.synchronize(
                SyncRequest.incremental(Instant.parse("2026-01-15T00:00:00Z"), 10));

        assertEquals("INCREMENTAL", report.mode());
        assertEquals(2, report.rowsRead());
        assertEquals(1, report.indexed());
        assertEquals(1, report.deleted());
        assertEquals(2, indexManager.stats().numDocs());
        assertEquals("Walid", document("1").get("first_name_raw"));
    }

    @Test
    void identifiants_sql_invalides_rejetes() {
        assertThrows(IllegalArgumentException.class,
                () -> SyncTableSpec.standard("person_source; DROP TABLE person_source"));
        assertThrows(IllegalArgumentException.class,
                () -> new SyncTableSpec("person_source", "id; --", "last_name", "first_name",
                        "birth_date", "birth_precision", "birth_flags", "birth_place",
                        "father_name", "mother_name", "owner_group", "lang", "updated_at", "deleted"));
    }

    @Test
    void incrémental_sans_colonne_de_mise_a_jour_refuse() {
        SyncTableSpec spec = SyncTableSpec.standard("person_source")
                .withOptionalColumns("", "deleted");
        JdbcPersonSource source = new JdbcPersonSource(dataSource, spec,
                new DateParser(Clock.systemUTC()));
        PersonSynchronizer sansDate = new PersonSynchronizer(source,
                new LuceneIndexSink(indexManager), Clock.systemUTC());
        assertThrows(SyncException.class,
                () -> sansDate.synchronize(SyncRequest.incremental(Instant.EPOCH, 10)));
    }

    private void insert(String id, String last, String first, String date, String group,
                        String updatedAt, boolean deleted) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO person_source
                     (id, last_name, first_name, birth_date, birth_precision, birth_flags,
                      birth_place, father_name, mother_name, owner_group, lang, updated_at, deleted)
                     VALUES (?, ?, ?, ?, 'JOUR', '', 'Alger', '', '', ?, 'fr', ?, ?)
                     """)) {
            statement.setString(1, id);
            statement.setString(2, last);
            statement.setString(3, first);
            statement.setDate(4, java.sql.Date.valueOf(date));
            statement.setString(5, group);
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.parse(updatedAt.replace(' ', 'T'))));
            statement.setBoolean(7, deleted);
            statement.executeUpdate();
        }
    }

    private void update(String id, String first, String updatedAt, boolean deleted) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE person_source SET first_name = ?, updated_at = ?, deleted = ? WHERE id = ?")) {
            statement.setString(1, first);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.parse(updatedAt.replace(' ', 'T'))));
            statement.setBoolean(3, deleted);
            statement.setString(4, id);
            statement.executeUpdate();
        }
    }

    private Document document(String id) throws Exception {
        var result = indexManager.search(new TermQuery(
                new Term(com.fastsearch.persons.search.IndexFields.SOURCE_ID, id)), 1);
        assertTrue(!result.candidates().isEmpty());
        return result.candidates().get(0).document();
    }
}
