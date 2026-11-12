package com.fastsearch.persons.sync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Description validée d'une table source JDBC.
 *
 * <p>Les noms de table et de colonnes ne viennent jamais d'une requête HTTP :
 * ils sont fixés par la configuration du serveur. Ils sont malgré tout
 * strictement validés (identifiant SQL simple, longueur PostgreSQL) puis
 * entourés de guillemets, afin qu'aucune concaténation ne puisse injecter de
 * SQL. Les valeurs, elles, passent toujours par {@link java.sql.PreparedStatement}.</p>
 */
public record SyncTableSpec(
        String table,
        String idColumn,
        String lastNameColumn,
        String firstNameColumn,
        String birthDateColumn,
        String birthPrecisionColumn,
        String birthFlagsColumn,
        String birthPlaceColumn,
        String fatherNameColumn,
        String motherNameColumn,
        String ownerGroupColumn,
        String langColumn,
        String updatedAtColumn,
        String deletedColumn
) {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    public SyncTableSpec {
        table = requireIdentifier(table, "table");
        idColumn = requireIdentifier(idColumn, "colonne identifiant");
        lastNameColumn = requireIdentifier(lastNameColumn, "colonne nom");
        firstNameColumn = requireIdentifier(firstNameColumn, "colonne prénom");
        birthDateColumn = requireIdentifier(birthDateColumn, "colonne date de naissance");
        birthPrecisionColumn = requireIdentifier(birthPrecisionColumn, "colonne précision de date");
        birthFlagsColumn = requireIdentifier(birthFlagsColumn, "colonne marqueurs de date");
        birthPlaceColumn = requireIdentifier(birthPlaceColumn, "colonne lieu de naissance");
        fatherNameColumn = requireIdentifier(fatherNameColumn, "colonne nom du père");
        motherNameColumn = requireIdentifier(motherNameColumn, "colonne nom de la mère");
        ownerGroupColumn = requireIdentifier(ownerGroupColumn, "colonne groupe propriétaire");
        langColumn = requireIdentifier(langColumn, "colonne langue");
        updatedAtColumn = optionalIdentifier(updatedAtColumn, "colonne de mise à jour");
        deletedColumn = optionalIdentifier(deletedColumn, "colonne de suppression logique");
        Set<String> all = new HashSet<>(List.of(table, idColumn, lastNameColumn, firstNameColumn,
                birthDateColumn, birthPrecisionColumn, birthFlagsColumn, birthPlaceColumn,
                fatherNameColumn, motherNameColumn, ownerGroupColumn, langColumn));
        if (!updatedAtColumn.isBlank()) {
            all.add(updatedAtColumn);
        }
        if (!deletedColumn.isBlank()) {
            all.add(deletedColumn);
        }
        if (all.size() != 12 + (updatedAtColumn.isBlank() ? 0 : 1) + (deletedColumn.isBlank() ? 0 : 1)) {
            throw new IllegalArgumentException("Les noms de table/colonnes doivent être distincts");
        }
    }

    /** Configuration standard utilisée par la démonstration et les tests. */
    public static SyncTableSpec standard(String table) {
        return new SyncTableSpec(table, "id", "last_name", "first_name", "birth_date",
                "birth_precision", "birth_flags", "birth_place", "father_name", "mother_name",
                "owner_group", "lang", "updated_at", "deleted");
    }

    public SyncTableSpec withOptionalColumns(String updatedAt, String deleted) {
        return new SyncTableSpec(table, idColumn, lastNameColumn, firstNameColumn, birthDateColumn,
                birthPrecisionColumn, birthFlagsColumn, birthPlaceColumn, fatherNameColumn,
                motherNameColumn, ownerGroupColumn, langColumn, updatedAt, deleted);
    }

    public boolean supportsIncremental() {
        return !updatedAtColumn.isBlank();
    }

    public boolean supportsSoftDelete() {
        return !deletedColumn.isBlank();
    }

    /** Liste SELECT complète, avec NULL pour les colonnes optionnelles absentes. */
    public String selectList() {
        StringBuilder sql = new StringBuilder();
        sql.append(q(idColumn)).append(", ")
                .append(q(lastNameColumn)).append(", ")
                .append(q(firstNameColumn)).append(", ")
                .append(q(birthDateColumn)).append(", ")
                .append(q(birthPrecisionColumn)).append(", ")
                .append(q(birthFlagsColumn)).append(", ")
                .append(q(birthPlaceColumn)).append(", ")
                .append(q(fatherNameColumn)).append(", ")
                .append(q(motherNameColumn)).append(", ")
                .append(q(ownerGroupColumn)).append(", ")
                .append(q(langColumn));
        if (supportsSoftDelete()) {
            sql.append(", ").append(q(deletedColumn));
        }
        return sql.toString();
    }

    /**
     * Requête de lecture. {@code since} non nul active le filtre incrémental sur
     * la colonne de mise à jour. Aucune valeur n'est concaténée : le paramètre
     * est fourni par le PreparedStatement.
     */
    public String selectSql(boolean incremental) {
        StringBuilder sql = new StringBuilder("SELECT ").append(selectList())
                .append(" FROM ").append(q(table));
        if (incremental) {
            if (!supportsIncremental()) {
                throw new IllegalStateException(
                        "Synchro incrémentale impossible sans colonne de mise à jour configurée");
            }
            sql.append(" WHERE ").append(q(updatedAtColumn)).append(" > ?");
        }
        sql.append(" ORDER BY ").append(q(idColumn));
        return sql.toString();
    }

    public static String quote(String identifier) {
        return "\"" + requireIdentifier(identifier, "identifiant SQL") + "\"";
    }

    private static String q(String identifier) {
        return quote(identifier);
    }

    private static String requireIdentifier(String value, String label) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Identifiant SQL invalide pour " + label + " : " + value);
        }
        return value;
    }

    private static String optionalIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return requireIdentifier(value.trim(), label);
    }
}
