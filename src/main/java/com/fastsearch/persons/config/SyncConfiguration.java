package com.fastsearch.persons.config;

import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.search.IndexManager;
import com.fastsearch.persons.sync.IndexSink;
import com.fastsearch.persons.sync.JdbcPersonSource;
import com.fastsearch.persons.sync.LuceneIndexSink;
import com.fastsearch.persons.sync.PersonSynchronizer;
import com.fastsearch.persons.sync.SyncTableSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/** Câblage de la synchronisation JDBC vers Lucene. */
@Configuration
public class SyncConfiguration {

    @Bean
    public SyncTableSpec syncTableSpec(FastSearchProperties properties) {
        FastSearchProperties.Sync sync = properties.getSync();
        return new SyncTableSpec(sync.getTable(), sync.getIdColumn(), sync.getLastNameColumn(),
                sync.getFirstNameColumn(), sync.getBirthDateColumn(),
                sync.getBirthPrecisionColumn(), sync.getBirthFlagsColumn(),
                sync.getBirthPlaceColumn(), sync.getFatherNameColumn(),
                sync.getMotherNameColumn(), sync.getOwnerGroupColumn(), sync.getLangColumn(),
                sync.getUpdatedAtColumn(), sync.getDeletedColumn());
    }

    @Bean
    public JdbcPersonSource jdbcPersonSource(DataSource dataSource, SyncTableSpec spec,
                                             DateParser dateParser) {
        return new JdbcPersonSource(dataSource, spec, dateParser);
    }

    @Bean
    public IndexSink indexSink(IndexManager indexManager) {
        return new LuceneIndexSink(indexManager);
    }

    @Bean
    public PersonSynchronizer personSynchronizer(JdbcPersonSource source, IndexSink sink,
                                                 Clock clock) {
        return new PersonSynchronizer(source, sink, clock);
    }
}
