package com.fastsearch.persons.dates;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/** Utilitaires de normalisation et de forme canonique des dates. */
public final class DateNormalizer {

    private static final Map<String, Integer> MONTHS = new HashMap<>();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    static {
        put(1, "janvier", "janv", "jan");
        put(2, "fevrier", "fevr", "fev");
        put(3, "mars");
        put(4, "avril", "avr");
        put(5, "mai");
        put(6, "juin");
        put(7, "juillet", "juil");
        put(8, "aout");
        put(9, "septembre", "sept");
        put(10, "octobre", "oct");
        put(11, "novembre", "nov");
        put(12, "decembre", "dec");

        // Algérie / Maghreb
        put(1, "جانفي", "جانفي");
        put(2, "فيفري");
        put(3, "مارس");
        put(4, "افريل", "أفريل");
        put(5, "ماي");
        put(6, "جوان");
        put(7, "جويلية");
        put(8, "اوت", "أوت");
        put(9, "سبتمبر");
        put(10, "اكتوبر", "أكتوبر");
        put(11, "نوفمبر");
        put(12, "ديسمبر");

        // Machrek
        put(1, "كانون الثاني");
        put(2, "شباط");
        put(3, "اذار", "آذار");
        put(4, "نيسان");
        put(5, "ايار", "أيار");
        put(6, "حزيران");
        put(7, "تموز");
        put(8, "اب", "آب");
        put(9, "ايلول", "أيلول");
        put(10, "تشرين الاول", "تشرين الأول");
        put(11, "تشرين الثاني");
        put(12, "كانون الاول", "كانون الأول");
    }

    private DateNormalizer() {
    }

    private static void put(int month, String... names) {
        for (String name : names) {
            MONTHS.put(normalizeMonth(name), month);
        }
    }

    /** Prépare une chaîne de date : chiffres, accents, arabe, espaces. */
    public static String prepare(String input) {
        if (input == null) {
            return "";
        }
        String value = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder digits = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            if (cp >= 0x0660 && cp <= 0x0669) {
                digits.append((char) ('0' + (cp - 0x0660)));
            } else if (cp >= 0x06F0 && cp <= 0x06F9) {
                digits.append((char) ('0' + (cp - 0x06F0)));
            } else {
                digits.appendCodePoint(cp);
            }
        });
        value = digits.toString();
        value = normalizeArabicLetters(value);
        // Enlève les accents latins et les marques combinantes restantes.
        value = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        value = value.replace(',', ' ')
                .replace(';', ' ')
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return value;
    }

    public static String normalizeMonth(String input) {
        String value = prepare(input);
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeArabicLetters(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);
            switch (cp) {
                case 0x0622, 0x0623, 0x0625, 0x0671 -> out.append('\u0627');
                case 0x0649, 0x06CC -> out.append('\u064A');
                case 0x0629 -> out.append('\u0647');
                default -> out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    public static OptionalInt monthNumber(String token) {
        Integer month = MONTHS.get(normalizeMonth(token));
        return month == null ? OptionalInt.empty() : OptionalInt.of(month);
    }

    public static String canonical(LocalDate date, DatePrecision precision) {
        if (date == null || precision == null) {
            return null;
        }
        return switch (precision) {
            case JOUR -> date.format(ISO);
            case MOIS -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            case ANNEE -> String.format("%04d", date.getYear());
        };
    }

    public static ParsedDate fromCanonical(String canonical, DatePrecision precision,
                                           java.util.Set<DateFlag> flags, double weight) {
        if (canonical == null || canonical.isBlank()) {
            return null;
        }
        try {
            String value = canonical.trim();
            LocalDate date = switch (precision) {
                case JOUR -> LocalDate.parse(value, ISO);
                case MOIS -> {
                    String[] parts = value.split("-");
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Mois invalide : " + value);
                    }
                    yield LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
                }
                case ANNEE -> LocalDate.of(Integer.parseInt(value), 1, 1);
            };
            return new ParsedDate(date, precision, flags, weight, canonical);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
