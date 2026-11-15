package com.fastsearch.persons.names;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalisation des noms, indépendante de Lucene et de Spring.
 *
 * <p>Deux niveaux sont volontairement distingués :</p>
 * <ul>
 *   <li>{@link #lightNormalize(String)} conserve les accents, utiles aux règles
 *       phonétiques (é ≠ e pour la voyelle cible) ;</li>
 *   <li>{@link #normalizeToken(String)} supprime les accents pour la comparaison
 *       exacte et les clés de dictionnaire.</li>
 * </ul>
 *
 * <p>Les particules (ben, ould, el, ال...) sont conservées dans le texte brut,
 * mais exclues des jetons de clé. Le préfixe de certains patronymes maghrébins
 * (BENALI, Bensalem) est retiré uniquement pour la clé, jamais pour l'affichage.</p>
 */
public final class NameNormalizer {

    public static final Set<String> PARTICLES = Set.of(
            "ben", "bin", "bent", "bint", "ould", "ouled", "ibn",
            "el", "al", "bel",
            "\u0628\u0646",       // بن
            "\u0628\u0646\u062A", // بنت
            "\u0648\u0644\u062F", // ولد
            "\u0627\u0628\u0646", // ابن
            "\u0627\u0644"        // ال
    );

    private static final Set<String> LATIN_PREFIXES = Set.of("ben", "bin", "bent", "ould");

    private NameNormalizer() {
    }

    /** Normalisation légère : NFKC, minuscules, ponctuation -> espace, arabe unifié. */
    public static String lightNormalize(String input) {
        if (input == null) {
            return "";
        }
        String value = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('\u2019', ' ')
                .replace('\u2018', ' ')
                .replace('`', ' ')
                .replace('\'', ' ')
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('.', ' ')
                .replace(',', ' ')
                .replace(';', ' ')
                .replace(':', ' ')
                .replace('/', ' ')
                .replace('\\', ' ')
                .replace('\u00A0', ' ');
        value = normalizeArabic(value);
        return value.replaceAll("\\s+", " ").trim();
    }

    /** Normalisation de comparaison : légère + suppression des accents latins. */
    public static String normalizeToken(String input) {
        String light = lightNormalize(input);
        if (light.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(light, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                out.append(c);
            }
        }
        return out.toString()
                .replace("œ", "oe")
                .replace("æ", "ae")
                .replace("ß", "ss")
                .replace("ø", "o")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalisation arabe minimale, dupliquée volontairement dans names pour que
     * ce package reste extractible sans dépendre de analysis (dépendances vers le bas).
     */
    public static String normalizeArabic(String input) {
        if (input == null) {
            return "";
        }
        String nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC);
        StringBuilder out = new StringBuilder(nfkc.length());
        for (int i = 0; i < nfkc.length(); ) {
            int cp = nfkc.codePointAt(i);
            i += Character.charCount(cp);
            if (cp >= 0x0660 && cp <= 0x0669) {
                out.append((char) ('0' + (cp - 0x0660)));
                continue;
            }
            if (cp >= 0x06F0 && cp <= 0x06F9) {
                out.append((char) ('0' + (cp - 0x06F0)));
                continue;
            }
            if ((cp >= 0x0610 && cp <= 0x061A)
                    || (cp >= 0x064B && cp <= 0x065F)
                    || cp == 0x0670
                    || (cp >= 0x06D6 && cp <= 0x06ED)
                    || cp == 0x0640
                    || cp == 0x200C || cp == 0x200D || cp == 0xFEFF) {
                continue;
            }
            switch (cp) {
                case 0x0622, 0x0623, 0x0625, 0x0671 -> out.append('\u0627');
                case 0x0649, 0x06CC -> out.append('\u064A');
                case 0x06A9 -> out.append('\u0643');
                case 0x06C1 -> out.append('\u0647');
                case 0x0629 -> out.append('\u0647');
                default -> out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    public static List<String> tokens(String input) {
        String normalized = lightNormalize(input);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }

    /** Jetons bruts (casse et graphie conservées) pour l'affichage et le parsing. */
    public static List<String> rawTokens(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        String value = input.trim()
                .replace('\u2019', ' ')
                .replace('\u2018', ' ')
                .replace('\'', ' ')
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('\u00A0', ' ');
        List<String> result = new ArrayList<>();
        for (String token : value.split("\\s+")) {
            String cleaned = token.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
            if (!cleaned.isBlank()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /** Jetons utilisés pour les clés : particules retirées et préfixes de patronyme retirés. */
    public static List<String> tokensForKeys(String input) {
        List<String> result = new ArrayList<>();
        for (String token : tokens(input)) {
            if (isParticle(token)) {
                continue;
            }
            String stripped = stripParticlePrefix(token);
            if (!stripped.isBlank() && !isParticle(stripped)) {
                result.add(stripped);
            }
        }
        return result;
    }

    public static boolean isParticle(String token) {
        String normalized = normalizeToken(token);
        return PARTICLES.contains(normalized);
    }

    /**
     * Retire un préfixe ben/bin/bent/ould uniquement s'il reste au moins deux lettres.
     * Exemple : BENALI -> ali ; Bentley -> ley (collision assumée, documentée).
     */
    public static String stripParticlePrefix(String token) {
        String normalized = normalizeToken(token);
        for (String prefix : LATIN_PREFIXES) {
            if (normalized.startsWith(prefix) && normalized.length() >= prefix.length() + 2) {
                String rest = normalized.substring(prefix.length());
                // « Benal » (troncature de Benali) ne doit pas devenir « al »,
                // puis être supprimé comme article : le nom disparaîtrait.
                if (!rest.isBlank() && !isParticle(rest)) {
                    return rest;
                }
            }
        }
        if (normalized.startsWith("\u0628\u0646") && normalized.length() >= 4) {
            return normalized.substring(2);
        }
        return normalized;
    }

    public static boolean containsArabic(String input) {
        if (input == null) {
            return false;
        }
        return input.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC);
    }

    public static boolean hasMeaningfulToken(String input) {
        for (String token : tokens(input)) {
            if (!isParticle(token) && normalizeToken(token).length() >= 2) {
                return true;
            }
        }
        return false;
    }

    public static String normalizedName(String input) {
        return String.join(" ", tokens(input));
    }

    public static Set<String> uniqueNormalizedTokens(String input) {
        Set<String> result = new LinkedHashSet<>();
        for (String token : tokens(input)) {
            result.add(normalizeToken(token));
        }
        return result;
    }
}
