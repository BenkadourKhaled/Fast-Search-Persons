package com.fastsearch.persons.dates;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyseur de dates de naissance.
 *
 * <p>Aucune correction silencieuse : une lecture invalide est rejetée avec un
 * motif ; les lectures ambiguës produisent plusieurs hypothèses pondérées.
 * L'heure et le pivot de siècle passent par une {@link Clock} injectée.</p>
 *
 * <p>Formats couverts : séparés jj/mm/aaaa, jj-mm-aa, séparateurs mélangés,
 * ISO, compacts 8/6/5/7 chiffres, mois en lettres français/arabes, année seule,
 * mois/année. Le calendrier grégorien est vérifié strictement.</p>
 */
public final class DateParser {

    private static final Pattern MONTH_WORD = Pattern.compile("^(\\d{1,2})\\s+([^\\d]+?)\\s+(\\d{2,4})$");
    private static final Pattern ISO = Pattern.compile("^(\\d{4})[\\s./-]+(\\d{1,2})[\\s./-]+(\\d{1,2})$");
    private static final Pattern DMY = Pattern.compile("^(\\d{1,2})[\\s./-]+(\\d{1,2})[\\s./-]+(\\d{2,4})$");
    private static final Pattern MONTH_YEAR = Pattern.compile("^(\\d{1,2})[\\s./-]+(\\d{4})$");
    private static final Pattern ISO_MONTH = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
    private static final Pattern YEAR = Pattern.compile("^(\\d{4})$");
    private static final Pattern COMPACT_8 = Pattern.compile("^(\\d{8})$");
    private static final Pattern COMPACT_6 = Pattern.compile("^(\\d{6})$");
    private static final Pattern COMPACT_7 = Pattern.compile("^(\\d{7})$");
    private static final Pattern COMPACT_5 = Pattern.compile("^(\\d{5})$");

    private final Clock clock;
    private final DateParserPolicy policy;

    public DateParser(Clock clock) {
        this(clock, DateParserPolicy.defaults());
    }

    public DateParser(Clock clock, DateParserPolicy policy) {
        this.clock = clock;
        this.policy = policy == null ? DateParserPolicy.defaults() : policy;
    }

    public DateParserPolicy policy() {
        return policy;
    }

    public DateParseResult parse(String raw) {
        String input = raw == null ? "" : raw.trim();
        if (input.isEmpty()) {
            return new DateParseResult(input, List.of(), List.of("date vide"));
        }
        String normalized = DateNormalizer.prepare(input);
        List<ParsedDate> hypotheses = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (parseMonthWord(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseIso(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseSeparated(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseMonthYear(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseYear(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseCompact8(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseCompact6(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseCompact7(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        if (parseCompact5(normalized, input, hypotheses, errors)) {
            return finish(input, hypotheses, errors);
        }
        errors.add("format de date non reconnu");
        return finish(input, hypotheses, errors);
    }

    private boolean parseMonthWord(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = MONTH_WORD.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int day = Integer.parseInt(m.group(1));
        OptionalInt month = DateNormalizer.monthNumber(m.group(2));
        if (month.isEmpty()) {
            errors.add("mois en lettres inconnu : " + m.group(2));
            return true;
        }
        int year = Integer.parseInt(m.group(3));
        if (m.group(3).length() == 2) {
            addTwoDigitYear(day, month.getAsInt(), year, EnumSet.of(DateFlag.MOIS_TEXTE),
                    input, out, errors, false);
        } else {
            addDmy(day, month.getAsInt(), year, EnumSet.of(DateFlag.MOIS_TEXTE), 1.0,
                    input, out, errors, false);
        }
        return true;
    }

    private boolean parseIso(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = ISO.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day = Integer.parseInt(m.group(3));
        addDmy(day, month, year, EnumSet.noneOf(DateFlag.class), 1.0,
                input, out, errors, false);
        return true;
    }

    private boolean parseSeparated(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = DMY.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int first = Integer.parseInt(m.group(1));
        int second = Integer.parseInt(m.group(2));
        int year = Integer.parseInt(m.group(3));
        int day = policy.dayFirst() ? first : second;
        int month = policy.dayFirst() ? second : first;
        boolean twoDigitYear = m.group(3).length() == 2;

        if (twoDigitYear) {
            addTwoDigitYear(day, month, year, EnumSet.noneOf(DateFlag.class),
                    input, out, errors, true);
        } else {
            addDmy(day, month, year, EnumSet.noneOf(DateFlag.class), 1.0,
                    input, out, errors, true);
        }
        return true;
    }

    private boolean parseMonthYear(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = MONTH_YEAR.matcher(value);
        if (m.matches()) {
            int month = Integer.parseInt(m.group(1));
            int year = Integer.parseInt(m.group(2));
            addMonth(month, year, input, out, errors);
            return true;
        }
        Matcher iso = ISO_MONTH.matcher(value);
        if (iso.matches()) {
            addMonth(Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(1)),
                    input, out, errors);
            return true;
        }
        return false;
    }

    private boolean parseYear(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = YEAR.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int year = Integer.parseInt(m.group(1));
        if (!plausibleYear(year)) {
            errors.add("année hors plage plausible : " + year);
            return true;
        }
        LocalDate date = LocalDate.of(year, 1, 1);
        addValidated(date, DatePrecision.ANNEE, EnumSet.noneOf(DateFlag.class), 1.0, input, out, errors);
        return true;
    }

    private boolean parseCompact8(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = COMPACT_8.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int day = Integer.parseInt(value.substring(0, 2));
        int month = Integer.parseInt(value.substring(2, 4));
        int year = Integer.parseInt(value.substring(4, 8));
        EnumSet<DateFlag> flags = EnumSet.of(DateFlag.COMPACT);
        List<ParsedDate> dayFirst = new ArrayList<>();
        List<String> dayFirstErrors = new ArrayList<>();
        addDmy(day, month, year, flags, 1.0, input, dayFirst, dayFirstErrors, true);

        if (!dayFirst.isEmpty()) {
            out.addAll(dayFirst);
            return true;
        }
        // Règle d'exclusion : yyyyMMdd seulement si ddMMyyyy est invalide et
        // que l'année commence par 19 ou 20.
        String yearPart = value.substring(0, 4);
        if (yearPart.startsWith("19") || yearPart.startsWith("20")) {
            int isoYear = Integer.parseInt(yearPart);
            int isoMonth = Integer.parseInt(value.substring(4, 6));
            int isoDay = Integer.parseInt(value.substring(6, 8));
            addDmy(isoDay, isoMonth, isoYear, flags, 1.0, input, out, errors, false);
            if (!out.isEmpty()) {
                return true;
            }
        }
        errors.addAll(dayFirstErrors);
        if (errors.isEmpty()) {
            errors.add("date compacte invalide");
        }
        return true;
    }

    private boolean parseCompact6(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = COMPACT_6.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int day = Integer.parseInt(value.substring(0, 2));
        int month = Integer.parseInt(value.substring(2, 4));
        int year = Integer.parseInt(value.substring(4, 6));
        addTwoDigitYear(day, month, year, EnumSet.of(DateFlag.COMPACT),
                input, out, errors, true);
        return true;
    }

    private boolean parseCompact7(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = COMPACT_7.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int day = Integer.parseInt(value.substring(0, 1));
        int month = Integer.parseInt(value.substring(1, 3));
        int year = Integer.parseInt(value.substring(3, 7));
        EnumSet<DateFlag> flags = EnumSet.of(DateFlag.COMPACT, DateFlag.ZERO_INITIAL_PERDU);
        addDmy(day, month, year, flags, policy.zeroInitialWeight(),
                input, out, errors, true);
        return true;
    }

    private boolean parseCompact5(String value, String input, List<ParsedDate> out, List<String> errors) {
        Matcher m = COMPACT_5.matcher(value);
        if (!m.matches()) {
            return false;
        }
        int day = Integer.parseInt(value.substring(0, 1));
        int month = Integer.parseInt(value.substring(1, 3));
        int year = Integer.parseInt(value.substring(3, 5));
        addTwoDigitYear(day, month, year,
                EnumSet.of(DateFlag.COMPACT, DateFlag.ZERO_INITIAL_PERDU),
                input, out, errors, true);
        return true;
    }

    private void addTwoDigitYear(int day, int month, int yy, Set<DateFlag> baseFlags,
                                 String input, List<ParsedDate> out, List<String> errors,
                                 boolean allowInverted) {
        LocalDate today = LocalDate.now(clock);
        int pivot = policy.pivotYear() != null ? policy.pivotYear() : today.getYear() % 100;
        int primaryYear = yy <= pivot ? 2000 + yy : 1900 + yy;
        int secondaryYear = primaryYear >= 2000 ? primaryYear - 100 : primaryYear + 100;

        List<Integer> years = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        boolean primaryValid = validDate(primaryYear, month, day) && plausible(primaryYear, month, day, today);
        boolean secondaryValid = validDate(secondaryYear, month, day) && plausible(secondaryYear, month, day, today);
        if (primaryValid) {
            years.add(primaryYear);
            weights.add(1.0);
        }
        if (secondaryValid) {
            years.add(secondaryYear);
            weights.add(policy.secondaryCenturyWeight());
        }
        if (!primaryValid && secondaryValid) {
            // Le siècle "principal" est futur ou invalide : l'autre devient primaire.
            weights.set(0, 1.0);
        }
        if (years.isEmpty()) {
            errors.add("date à deux chiffres invalide ou future");
            return;
        }
        for (int i = 0; i < years.size(); i++) {
            EnumSet<DateFlag> flags = baseFlags.isEmpty()
                    ? EnumSet.noneOf(DateFlag.class)
                    : EnumSet.copyOf(baseFlags);
            flags.add(DateFlag.SIECLE_DEDUIT);
            flags.add(DateFlag.ANNEE_DEUX_CHIFFRES);
            addDmy(day, month, years.get(i), flags, weights.get(i), input, out, errors, allowInverted);
        }
    }

    private void addDmy(int day, int month, int year, Set<DateFlag> baseFlags, double baseWeight,
                        String input, List<ParsedDate> out, List<String> errors,
                        boolean allowInverted) {
        if (!validDate(year, month, day)) {
            errors.add("date invalide : " + day + "/" + month + "/" + year);
            return;
        }
        LocalDate primary = LocalDate.of(year, month, day);
        addValidated(primary, DatePrecision.JOUR, baseFlags, baseWeight, input, out, errors);

        if (allowInverted && policy.generateInverted() && day <= 12 && month <= 12 && day != month) {
            LocalDate inverted = LocalDate.of(year, day, month);
            EnumSet<DateFlag> invertedFlags = baseFlags.isEmpty()
                    ? EnumSet.noneOf(DateFlag.class)
                    : EnumSet.copyOf(baseFlags);
            invertedFlags.add(DateFlag.JOUR_MOIS_HYPOTHESE);
            addValidated(inverted, DatePrecision.JOUR, invertedFlags,
                    baseWeight * policy.invertedWeight(), input, out, errors);
        }
    }

    private void addMonth(int month, int year, String input, List<ParsedDate> out, List<String> errors) {
        if (month < 1 || month > 12 || !plausibleYear(year)) {
            errors.add("mois/année invalide : " + month + "/" + year);
            return;
        }
        LocalDate date = LocalDate.of(year, month, 1);
        addValidated(date, DatePrecision.MOIS, EnumSet.noneOf(DateFlag.class), 1.0, input, out, errors);
    }

    private void addValidated(LocalDate date, DatePrecision precision, Set<DateFlag> flags,
                              double weight, String input, List<ParsedDate> out, List<String> errors) {
        LocalDate today = LocalDate.now(clock);
        if (date.getYear() < 1800 || date.getYear() > today.getYear()) {
            errors.add("date future ou hors plage : " + date);
            return;
        }
        if (precision == DatePrecision.JOUR && date.isAfter(today)) {
            errors.add("date future : " + date);
            return;
        }
        if (precision == DatePrecision.MOIS && date.isAfter(today.withDayOfMonth(1))) {
            errors.add("mois futur : " + date);
            return;
        }
        if (precision == DatePrecision.ANNEE && date.getYear() > today.getYear()) {
            errors.add("année future : " + date);
            return;
        }
        ParsedDate parsed = new ParsedDate(date, precision, flags, weight, input);
        // Déduplication : même forme canonique, même précision.
        for (ParsedDate existing : out) {
            if (existing.canonical().equals(parsed.canonical()) && existing.precision() == precision) {
                return;
            }
        }
        out.add(parsed);
    }

    private boolean validDate(int year, int month, int day) {
        if (year < 1 || month < 1 || month > 12 || day < 1) {
            return false;
        }
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean plausibleYear(int year) {
        int current = LocalDate.now(clock).getYear();
        return year >= 1800 && year <= current;
    }

    private boolean plausible(int year, int month, int day, LocalDate today) {
        if (year < 1800 || year > today.getYear()) {
            return false;
        }
        if (year < today.getYear() - policy.maxAgeYears()) {
            return false;
        }
        return !LocalDate.of(year, month, day).isAfter(today);
    }

    private DateParseResult finish(String input, List<ParsedDate> hypotheses, List<String> errors) {
        List<ParsedDate> sorted = hypotheses.stream()
                .sorted(Comparator.comparingDouble(ParsedDate::weight).reversed())
                .toList();
        if (sorted.isEmpty() && errors.isEmpty()) {
            errors = List.of("date invalide");
        }
        return new DateParseResult(input, sorted, errors);
    }

    /** Forme canonique d'une date stockée, utilisée par le matcher. */
    public static ParsedDate fromStored(String canonical, DatePrecision precision,
                                        Set<DateFlag> flags, double weight) {
        return DateNormalizer.fromCanonical(canonical, precision, flags, weight);
    }

    /** Analyse une date de type DATE lue en JDBC, sans passer par une chaîne. */
    public DateParseResult parse(LocalDate date, DatePrecision precision) {
        if (date == null) {
            return new DateParseResult("", List.of(), List.of("date nulle"));
        }
        List<ParsedDate> out = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        addValidated(date, precision, EnumSet.noneOf(DateFlag.class), 1.0, date.toString(), out, errors);
        return finish(date.toString(), out, errors);
    }
}
