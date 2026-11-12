package com.fastsearch.persons.dates;

/**
 * Politique de date configurable par source de données.
 *
 * @param dayFirst              vrai pour jj/mm (convention algérienne)
 * @param pivotYear             année pivot sur 2 chiffres ; null = année courante % 100
 * @param invertedWeight        poids de l'hypothèse jour/mois inversée
 * @param zeroInitialWeight     poids quand un zéro initial a été perdu
 * @param secondaryCenturyWeight poids de l'autre siècle plausible
 * @param maxAgeYears           âge maximal plausible pour conserver l'autre siècle
 * @param generateInverted      générer l'hypothèse jour/mois inversée quand ambigu
 */
public record DateParserPolicy(
        boolean dayFirst,
        Integer pivotYear,
        double invertedWeight,
        double zeroInitialWeight,
        double secondaryCenturyWeight,
        int maxAgeYears,
        boolean generateInverted
) {

    public static DateParserPolicy defaults() {
        return new DateParserPolicy(true, null, 0.55, 0.80, 0.60, 120, true);
    }

    public DateParserPolicy {
        if (invertedWeight < 0 || invertedWeight > 1
                || zeroInitialWeight < 0 || zeroInitialWeight > 1
                || secondaryCenturyWeight < 0 || secondaryCenturyWeight > 1) {
            throw new IllegalArgumentException("Poids de date hors de [0,1]");
        }
        if (maxAgeYears < 0) {
            throw new IllegalArgumentException("Âge maximal invalide");
        }
    }
}
