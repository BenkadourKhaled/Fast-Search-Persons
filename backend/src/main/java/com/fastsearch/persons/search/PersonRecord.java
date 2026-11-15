package com.fastsearch.persons.search;

import com.fastsearch.persons.dates.DateFlag;
import com.fastsearch.persons.dates.DatePrecision;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Enregistrement source, indépendant de Spring et de Lucene.
 * Les champs bruts sont conservés tels quels ; les champs dérivés servent à l'index.
 */
public record PersonRecord(
        String id,
        String lastName,
        String firstName,
        LocalDate birthDate,
        DatePrecision birthPrecision,
        Set<DateFlag> birthFlags,
        String birthPlace,
        String fatherName,
        String motherName,
        String ownerGroup,
        String lang
) {

    public PersonRecord {
        id = id == null ? "" : id.trim();
        lastName = nullToEmpty(lastName);
        firstName = nullToEmpty(firstName);
        birthFlags = birthFlags == null || birthFlags.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(birthFlags));
        birthPlace = nullToEmpty(birthPlace);
        fatherName = nullToEmpty(fatherName);
        motherName = nullToEmpty(motherName);
        ownerGroup = ownerGroup == null ? "DEFAULT" : ownerGroup.trim();
        lang = lang == null ? "" : lang.trim();
        if (birthDate != null && birthPrecision == null) {
            birthPrecision = DatePrecision.JOUR;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public String fullName() {
        if (lastName.isBlank()) {
            return firstName;
        }
        if (firstName.isBlank()) {
            return lastName;
        }
        return lastName + " " + firstName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id = "";
        private String lastName = "";
        private String firstName = "";
        private LocalDate birthDate;
        private DatePrecision birthPrecision;
        private Set<DateFlag> birthFlags = Set.of();
        private String birthPlace = "";
        private String fatherName = "";
        private String motherName = "";
        private String ownerGroup = "DEFAULT";
        private String lang = "fr";

        public Builder id(String value) { this.id = value; return this; }
        public Builder lastName(String value) { this.lastName = value; return this; }
        public Builder firstName(String value) { this.firstName = value; return this; }
        public Builder birthDate(LocalDate value) { this.birthDate = value; return this; }
        public Builder birthPrecision(DatePrecision value) { this.birthPrecision = value; return this; }
        public Builder birthFlags(Set<DateFlag> value) { this.birthFlags = value; return this; }
        public Builder birthPlace(String value) { this.birthPlace = value; return this; }
        public Builder fatherName(String value) { this.fatherName = value; return this; }
        public Builder motherName(String value) { this.motherName = value; return this; }
        public Builder ownerGroup(String value) { this.ownerGroup = value; return this; }
        public Builder lang(String value) { this.lang = value; return this; }

        public PersonRecord build() {
            return new PersonRecord(id, lastName, firstName, birthDate, birthPrecision,
                    birthFlags, birthPlace, fatherName, motherName, ownerGroup, lang);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PersonRecord p && Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
