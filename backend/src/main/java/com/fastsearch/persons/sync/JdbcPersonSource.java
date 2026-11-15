package com.fastsearch.persons.sync;

import com.fastsearch.persons.dates.DateFlag;
import com.fastsearch.persons.dates.DateParseResult;
import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.search.PersonRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Lecture JDBC par lots. La connexion et le ResultSet sont ouverts pendant
 * toute la synchronisation ; {@code setFetchSize} limite la mémoire côté
 * pilote. Les valeurs sont toujours liées par PreparedStatement.
 */
public final class JdbcPersonSource {

    private final DataSource dataSource;
    private final SyncTableSpec spec;
    private final DateParser dateParser;

    public JdbcPersonSource(DataSource dataSource, SyncTableSpec spec, DateParser dateParser) {
        this.dataSource = dataSource;
        this.spec = spec;
        this.dateParser = dateParser;
    }

    public SyncTableSpec spec() {
        return spec;
    }

    /**
     * Lit les lignes et appelle le consommateur à chaque lot plein, puis une
     * dernière fois pour le reliquat. {@code since} nul signifie lecture
     * complète.
     */
    public void readBatches(Instant since, int batchSize, BatchConsumer consumer) throws SQLException {
        int size = Math.max(1, batchSize);
        boolean incremental = since != null;
        String sql = spec.selectSql(incremental);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setFetchSize(size);
            if (incremental) {
                statement.setTimestamp(1, Timestamp.from(since));
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<PersonRecord> upserts = new ArrayList<>(size);
                List<String> deletes = new ArrayList<>();
                String lastId = "";
                int count = 0;
                while (rows.next()) {
                    String id = readId(rows);
                    lastId = id;
                    if (isDeleted(rows)) {
                        deletes.add(id);
                    } else {
                        upserts.add(map(rows, id));
                    }
                    count++;
                    if (count >= size) {
                        emit(consumer, upserts, deletes, lastId, count);
                        upserts = new ArrayList<>(size);
                        deletes = new ArrayList<>();
                        count = 0;
                    }
                }
                if (count > 0) {
                    emit(consumer, upserts, deletes, lastId, count);
                }
            }
        }
    }

    private static void emit(BatchConsumer consumer, List<PersonRecord> upserts,
                             List<String> deletes, String lastId, int count) throws SQLException {
        try {
            consumer.accept(new SourceBatch(upserts, deletes, lastId, count));
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SyncException("Traitement du lot source impossible", e);
        }
    }

    private PersonRecord map(ResultSet row, String id) throws SQLException {
        String lastName = readString(row, spec.lastNameColumn());
        String firstName = readString(row, spec.firstNameColumn());
        LocalDate birthDate = readDate(row);
        DatePrecision precision = readPrecision(row, birthDate);
        Set<DateFlag> flags = readFlags(row);
        String birthPlace = readString(row, spec.birthPlaceColumn());
        String fatherName = readString(row, spec.fatherNameColumn());
        String motherName = readString(row, spec.motherNameColumn());
        String ownerGroup = readString(row, spec.ownerGroupColumn());
        String lang = readString(row, spec.langColumn());
        return PersonRecord.builder()
                .id(id)
                .lastName(lastName)
                .firstName(firstName)
                .birthDate(birthDate)
                .birthPrecision(precision)
                .birthFlags(flags)
                .birthPlace(birthPlace)
                .fatherName(fatherName)
                .motherName(motherName)
                .ownerGroup(ownerGroup.isBlank() ? "DEFAULT" : ownerGroup)
                .lang(lang.isBlank() ? "fr" : lang)
                .build();
    }

    private String readId(ResultSet row) throws SQLException {
        String id = row.getString(spec.idColumn());
        if (id == null || id.isBlank()) {
            throw new SQLException("Identifiant source vide : synchronisation interrompue");
        }
        return id.trim();
    }

    private boolean isDeleted(ResultSet row) throws SQLException {
        if (!spec.supportsSoftDelete()) {
            return false;
        }
        return row.getBoolean(spec.deletedColumn());
    }

    private static String readString(ResultSet row, String column) throws SQLException {
        String value = row.getString(column);
        return value == null ? "" : value.trim();
    }

    private LocalDate readDate(ResultSet row) throws SQLException {
        Object value = row.getObject(spec.birthDateColumn());
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        DateParseResult parsed = dateParser.parse(text);
        return parsed.isValid() ? parsed.primary().date() : null;
    }

    private DatePrecision readPrecision(ResultSet row, LocalDate birthDate) throws SQLException {
        String value = readString(row, spec.birthPrecisionColumn());
        if (value.isBlank()) {
            return birthDate == null ? null : DatePrecision.JOUR;
        }
        try {
            return DatePrecision.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return birthDate == null ? null : DatePrecision.JOUR;
        }
    }

    private Set<DateFlag> readFlags(ResultSet row) throws SQLException {
        String value = readString(row, spec.birthFlagsColumn());
        if (value.isBlank()) {
            return Set.of();
        }
        EnumSet<DateFlag> flags = EnumSet.noneOf(DateFlag.class);
        for (String token : value.split("[,;|]")) {
            String flag = token.trim();
            if (flag.isEmpty()) {
                continue;
            }
            try {
                flags.add(DateFlag.valueOf(flag.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Un marqueur inconnu est ignoré ; la date reste utilisable.
            }
        }
        return flags;
    }
}
