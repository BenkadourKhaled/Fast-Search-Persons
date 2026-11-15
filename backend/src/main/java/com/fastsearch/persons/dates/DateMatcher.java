package com.fastsearch.persons.dates;

import java.time.LocalDate;
import java.util.Set;

/**
 * Matcher structuré de dates : exact, jour/mois inversé, erreur d'un chiffre,
 * année ±1, date partielle. Les dates conventionnelles 01/01 sont plafonnées
 * par défaut : une date seule ne doit jamais suffire à créer une confiance FORT.
 */
public final class DateMatcher {

    private final DateMatchOptions options;

    public DateMatcher() {
        this(DateMatchOptions.defaults());
    }

    public DateMatcher(DateMatchOptions options) {
        this.options = options == null ? DateMatchOptions.defaults() : options;
    }

    public DateMatchResult match(ParsedDate query, ParsedDate candidate) {
        if (query == null || candidate == null) {
            return DateMatchResult.none("date absente");
        }
        DateMatchResult result = compare(query, candidate);
        if (options.penalizeConventionalDates()
                && (query.isConventionalDate() || candidate.isConventionalDate())
                && query.precision() == DatePrecision.JOUR
                && candidate.precision() == DatePrecision.JOUR) {
            double capped = Math.min(result.score(), options.conventionalCap());
            if (capped < result.score()) {
                return new DateMatchResult(capped, DateMatchLevel.HYPOTHESE,
                        result.detail() + " ; date 01/01 conventionnelle plafonnée",
                        query.canonical(), candidate.canonical());
            }
        }
        return result;
    }

    private DateMatchResult compare(ParsedDate query, ParsedDate candidate) {
        LocalDate q = query.date();
        LocalDate c = candidate.date();

        if (query.precision() == DatePrecision.JOUR && candidate.precision() == DatePrecision.JOUR) {
            if (q.equals(c)) {
                return result(options.exactWeight(), DateMatchLevel.EXACTE,
                        "date exacte", query, candidate);
            }
            if (q.getYear() == c.getYear()
                    && q.getMonthValue() == c.getDayOfMonth()
                    && q.getDayOfMonth() == c.getMonthValue()) {
                return result(options.invertedWeight(), DateMatchLevel.JOUR_MOIS_INVERSE,
                        "jour et mois inversés", query, candidate);
            }
            if (q.getYear() == c.getYear() + 1 || q.getYear() == c.getYear() - 1) {
                if (q.getMonthValue() == c.getMonthValue() && q.getDayOfMonth() == c.getDayOfMonth()) {
                    return result(options.yearDeltaWeight(), DateMatchLevel.TOLEREE,
                            "année à ±1", query, candidate);
                }
            }
            if (levenshtein(query.canonical(), candidate.canonical()) <= 1) {
                return result(options.oneDigitWeight(), DateMatchLevel.TOLEREE,
                        "une erreur de chiffre tolérée", query, candidate);
            }
            return DateMatchResult.none("dates incompatibles");
        }

        if (query.precision() == DatePrecision.ANNEE || candidate.precision() == DatePrecision.ANNEE) {
            if (q.getYear() == c.getYear()) {
                return result(options.partialWeight(), DateMatchLevel.PARTIELLE,
                        "année compatible (date partielle)", query, candidate);
            }
            return DateMatchResult.none("années différentes");
        }

        if (query.precision() == DatePrecision.MOIS && candidate.precision() == DatePrecision.MOIS) {
            if (q.getYear() == c.getYear() && q.getMonthValue() == c.getMonthValue()) {
                return result(options.exactWeight(), DateMatchLevel.EXACTE,
                        "mois exact", query, candidate);
            }
            if (q.getYear() == c.getYear()) {
                return result(options.partialWeight() * 0.65, DateMatchLevel.PARTIELLE,
                        "même année, mois différent", query, candidate);
            }
            return DateMatchResult.none("mois incompatibles");
        }

        // Une date au jour près contre une date au mois près.
        if (q.getYear() == c.getYear() && q.getMonthValue() == c.getMonthValue()) {
            return result(options.partialWeight(), DateMatchLevel.PARTIELLE,
                    "mois compatible (date partielle)", query, candidate);
        }
        return DateMatchResult.none("dates partielles incompatibles");
    }

    private DateMatchResult result(double score, DateMatchLevel level, String detail,
                                   ParsedDate query, ParsedDate candidate) {
        return new DateMatchResult(score, level, detail, query.canonical(), candidate.canonical());
    }

    /** Distance d'édition bornée, suffisante pour détecter une différence d'un chiffre. */
    private static int levenshtein(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) {
            return 2;
        }
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    public static Set<DateFlag> flags(ParsedDate date) {
        return date == null ? Set.of() : date.flags();
    }
}
