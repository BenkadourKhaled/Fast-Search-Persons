package com.fastsearch.persons.dates;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Date interprétée, avec précision, drapeaux et poids de tolérance. */
public record ParsedDate(
        LocalDate date,
        DatePrecision precision,
        Set<DateFlag> flags,
        double weight,
        String input
) {

    public ParsedDate {
        if (date == null || precision == null) {
            throw new IllegalArgumentException("Date et précision obligatoires");
        }
        flags = flags == null || flags.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(flags));
        if (weight < 0 || weight > 1.0) {
            throw new IllegalArgumentException("Poids hors de [0,1] : " + weight);
        }
        input = input == null ? "" : input;
    }

    public String canonical() {
        return switch (precision) {
            case JOUR -> date.toString();
            case MOIS -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            case ANNEE -> String.format("%04d", date.getYear());
        };
    }

    public boolean hasFlag(DateFlag flag) {
        return flags.contains(flag);
    }

    public boolean isConventionalDate() {
        return precision == DatePrecision.JOUR
                && date.getMonthValue() == 1
                && date.getDayOfMonth() == 1;
    }
}
