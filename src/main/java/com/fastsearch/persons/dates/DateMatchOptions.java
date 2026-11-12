package com.fastsearch.persons.dates;

/**
 * Poids structurés du matcher de dates. Aucune recherche textuelle floue
 * n'est utilisée sur les dates.
 */
public record DateMatchOptions(
        double exactWeight,
        double invertedWeight,
        double oneDigitWeight,
        double yearDeltaWeight,
        double partialWeight,
        double conventionalCap,
        boolean penalizeConventionalDates
) {

    public static DateMatchOptions defaults() {
        return new DateMatchOptions(1.0, 0.70, 0.60, 0.45, 0.65, 0.50, true);
    }

    public DateMatchOptions {
        for (double value : new double[]{exactWeight, invertedWeight, oneDigitWeight,
                yearDeltaWeight, partialWeight, conventionalCap}) {
            if (value < 0 || value > 1) {
                throw new IllegalArgumentException("Poids de date hors de [0,1]");
            }
        }
    }
}
