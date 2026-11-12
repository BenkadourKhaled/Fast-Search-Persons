package com.fastsearch.persons.analysis;

import java.text.Normalizer;

/**
 * Normalisation déterministe du texte arabe, sans dépendance à Lucene ni à Spring.
 *
 * <p>Cette classe ne translittère pas : elle unifie seulement les variantes de saisie.
 * Les voyelles brèves ne sont pas inventées. Une conséquence assumée est qu'une clé
 * phonétique arabe et une clé phonétique latine ne peuvent pas être identiques en général :
 * l'arabe écrit rarement les voyelles brèves (voir README).</p>
 */
public final class ArabicTextNormalizer {

    private ArabicTextNormalizer() {
    }

    public static String normalize(String input) {
        return normalize(input, true);
    }

    /**
     * @param input texte éventuellement arabe
     * @param taMarbutaToHa si vrai, ة devient ه. Option désactivable car elle peut
     *                      fusionner deux noms distincts dans de rares cas.
     */
    public static String normalize(String input, boolean taMarbutaToHa) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
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
            if (isArabicMark(cp) || cp == 0x0640 || cp == 0x200C || cp == 0x200D || cp == 0xFEFF) {
                continue;
            }
            switch (cp) {
                case 0x0622, 0x0623, 0x0625, 0x0671 -> out.append('\u0627'); // آ أ إ ٱ -> ا
                case 0x0649 -> out.append('\u064A');                           // ى -> ي
                case 0x06CC -> out.append('\u064A');                           // ی persan -> ي
                case 0x06A9 -> out.append('\u0643');                           // ک -> ك
                case 0x06C1 -> out.append('\u0647');                           // ہ -> ه
                case 0x0629 -> out.append(taMarbutaToHa ? '\u0647' : '\u0629'); // ة -> ه
                default -> out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    private static boolean isArabicMark(int cp) {
        return (cp >= 0x0610 && cp <= 0x061A)
                || (cp >= 0x064B && cp <= 0x065F)
                || cp == 0x0670
                || (cp >= 0x06D6 && cp <= 0x06ED)
                || (cp >= 0x08D3 && cp <= 0x08FF);
    }

    public static boolean containsArabic(String value) {
        if (value == null) {
            return false;
        }
        return value.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC);
    }

    /** Convertit uniquement les chiffres arabo-indiens/persans, sans toucher au reste. */
    public static String asciiDigits(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            if (cp >= 0x0660 && cp <= 0x0669) {
                out.append((char) ('0' + (cp - 0x0660)));
            } else if (cp >= 0x06F0 && cp <= 0x06F9) {
                out.append((char) ('0' + (cp - 0x06F0)));
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }
}
