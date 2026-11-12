package com.fastsearch.persons.names;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deux clés phonétiques par script.
 *
 * <p><b>Constat arabe.</b> L'écriture arabe ne note pas les voyelles brèves :
 * وليد s'écrit w-l-y-d, tandis que « Walid » note le a. Les clés fines arabe
 * et latine ne peuvent donc pas être identiques en général. Le dictionnaire
 * fournit le pont fiable ; la clé grossière commune sert de repli, avec un
 * plafond de confiance MOYEN.</p>
 *
 * <p><b>Clé fine</b> : voyelles normalisées conservées. <b>Clé grossière</b> :
 * squelette consonantique, plus agressif, volontairement collisionnel.</p>
 *
 * <p>Règles latines, dans l'ordre : suppression de la finale muette e ;
 * digrammes ; « ou » contextuel ; voyelles ; consonnes ; réduction des doublons.
 * La règle « ou » suit le principe demandé : w en début devant voyelle ou après
 * une consonne devant voyelle, sinon u. En clé <i>grossière</i> seulement, un
 * « ou » initial est conservé comme w afin de couvrir Ould/Walid et de rendre la
 * collision explicitement mesurable.</p>
 *
 * <p>Écart documenté : ai/ay/ei/ey sont ramenés à i, et non à ay, pour que
 * Zineb = Zeinab = Zaynab et Hussein = Hocine = Houcine. Ce choix est testé ;
 * il augmente les collisions et est assumé.</p>
 */
public record PhoneticKey(String fine, String coarse) {

    public PhoneticKey {
        fine = fine == null ? "" : fine;
        coarse = coarse == null ? "" : coarse;
    }

    /** Encode un jeton isolé. Les particules ne sont pas retirées ici. */
    public static PhoneticKey of(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new PhoneticKey("", "");
        }
        String light = NameNormalizer.lightNormalize(rawToken);
        if (light.isBlank()) {
            return new PhoneticKey("", "");
        }
        if (light.indexOf(' ') >= 0) {
            return ofName(rawToken);
        }
        String token = light;
        token = NameNormalizer.stripParticlePrefix(token);
        if (token.isBlank()) {
            return new PhoneticKey("", "");
        }
        if (containsArabicLetter(token)) {
            return arabic(token);
        }
        return latin(token);
    }

    /**
     * Encode un nom complet : une clé par jeton significatif, particules retirées.
     * L'ordre des jetons est conservé ; le moteur compare aussi les ensembles.
     */
    public static PhoneticKey ofName(String fullName) {
        List<String> fines = new ArrayList<>();
        List<String> coarses = new ArrayList<>();
        for (String token : NameNormalizer.tokensForKeys(fullName)) {
            PhoneticKey key = of(token);
            if (!key.fine.isBlank()) {
                fines.add(key.fine);
                coarses.add(key.coarse);
            }
        }
        return new PhoneticKey(String.join(" ", fines), String.join(" ", coarses));
    }

    public static boolean isParticleToken(String token) {
        return NameNormalizer.isParticle(token);
    }

    public boolean isEmpty() {
        return fine.isEmpty() && coarse.isEmpty();
    }

    private static boolean containsArabicLetter(String value) {
        return value.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.ARABIC);
    }

    private static PhoneticKey latin(String input) {
        String token = prepareLatin(input);
        return new PhoneticKey(latinFine(token), latinCoarse(token));
    }

    private static String prepareLatin(String input) {
        String token = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace("œ", "oe")
                .replace("æ", "ae")
                .replace("ß", "ss")
                .replace("ø", "o")
                .replaceAll("[^\\p{L}\\p{N}]", "");
        if (token.isBlank()) {
            return "";
        }
        // Abdelkader / Abd El Kader : le compound est ramené à abd + attribut.
        if (token.startsWith("abd") && token.length() > 5) {
            String rest = token.substring(3);
            if ((rest.startsWith("el") || rest.startsWith("al")) && rest.length() > 2) {
                token = "abd" + rest.substring(2);
            } else if (rest.length() > 2
                    && (rest.charAt(0) == 'e' || rest.charAt(0) == 'a')
                    && !isLatinVowel(rest.charAt(1))) {
                token = "abd" + rest.substring(1);
            }
        }
        // e final muet : Walide -> walid, Mourade -> mourad.
        if (token.length() > 3 && token.endsWith("e") && !isLatinVowel(token.charAt(token.length() - 2))) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }

    private static String latinFine(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); ) {
            char c = token.charAt(i);
            char next = i + 1 < token.length() ? token.charAt(i + 1) : 0;

            if (c == 'o' && next == 'u') {
                char after = i + 2 < token.length() ? token.charAt(i + 2) : 0;
                boolean initial = i == 0;
                boolean afterConsonant = i > 0 && !isLatinVowel(token.charAt(i - 1));
                if ((initial || afterConsonant) && after != 0 && isLatinVowel(after)) {
                    out.append('w');
                } else {
                    out.append('u');
                }
                i += 2;
                continue;
            }
            if (c == 'a' && next == 'i' || c == 'a' && next == 'y'
                    || c == 'e' && next == 'i' || c == 'e' && next == 'y') {
                out.append('i');
                i += 2;
                continue;
            }
            if (c == 'a' && next == 'u') {
                out.append('u');
                i += 2;
                continue;
            }

            String digraph = "" + c + next;
            switch (digraph) {
                case "kh" -> { out.append('k'); i += 2; continue; }
                case "gh" -> { out.append('g'); i += 2; continue; }
                case "sh", "ch" -> { out.append('c'); i += 2; continue; }
                case "th" -> { out.append('t'); i += 2; continue; }
                case "dh" -> { out.append('d'); i += 2; continue; }
                case "dj" -> { out.append('j'); i += 2; continue; }
                case "ph" -> { out.append('f'); i += 2; continue; }
                case "ts" -> { out.append('t'); i += 2; continue; }
                default -> { }
            }

            switch (c) {
                case 'a', 'â', 'à', 'e', 'è', 'ê' -> out.append('a');
                case 'é', 'i', 'y', 'ï', 'î' -> out.append('i');
                case 'o', 'u', 'ô', 'û', 'ù' -> out.append('u');
                case 'ç' -> out.append('s');
                case 'c' -> out.append(next == 'e' || next == 'i' || next == 'y' ? 's' : 'k');
                case 'q' -> out.append('k');
                case 'g' -> out.append(isLatinVowel(next) ? 'j' : 'g');
                case 'x' -> out.append("ks");
                case '3', '7', '9', '5', '2' -> out.append(c);
                case 'h' -> {
                    boolean finalH = i == token.length() - 1 && i > 1 && isLatinVowel(token.charAt(i - 1));
                    if (!finalH) {
                        out.append('h');
                    }
                }
                default -> {
                    if (Character.isLetterOrDigit(c)) {
                        out.append(c);
                    }
                }
            }
            i++;
        }
        return collapseDuplicates(out.toString());
    }

    private static String latinCoarse(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); ) {
            char c = token.charAt(i);
            char next = i + 1 < token.length() ? token.charAt(i + 1) : 0;

            if (c == 'o' && next == 'u') {
                if (i == 0) {
                    out.append('w');
                }
                i += 2;
                continue;
            }
            if (isLatinVowel(c)) {
                i++;
                continue;
            }

            String digraph = "" + c + next;
            switch (digraph) {
                case "kh" -> { out.append('k'); i += 2; continue; }
                case "gh" -> { out.append('g'); i += 2; continue; }
                case "sh", "ch" -> { out.append('c'); i += 2; continue; }
                case "th" -> { out.append('t'); i += 2; continue; }
                case "dh" -> { out.append('d'); i += 2; continue; }
                case "dj" -> { out.append('j'); i += 2; continue; }
                case "ph" -> { out.append('f'); i += 2; continue; }
                case "ts" -> { out.append('s'); i += 2; continue; }
                default -> { }
            }

            switch (c) {
                case 'ç' -> out.append('s');
                case 'c' -> out.append(next == 'e' || next == 'i' || next == 'y' ? 's' : 'k');
                case 'q' -> out.append('k');
                case 'g' -> out.append(isLatinVowel(next) ? 'j' : 'g');
                case 'x' -> out.append("ks");
                case '3', '2' -> { }
                case '7' -> out.append('h');
                case '5', '9' -> out.append('k');
                case 'h' -> {
                    boolean finalH = i == token.length() - 1 && i > 1;
                    if (!finalH) {
                        out.append('h');
                    }
                }
                default -> {
                    if (Character.isLetter(c)) {
                        out.append(c);
                    }
                }
            }
            i++;
        }
        return collapseDuplicates(out.toString());
    }

    private static PhoneticKey arabic(String input) {
        String token = NameNormalizer.normalizeArabic(input).replaceAll("[^\\p{IsArabic}\\p{N}]", "");
        if (token.isBlank()) {
            return new PhoneticKey("", "");
        }
        // L'article ال est une particule de nom : retiré de la clé, conservé au brut.
        if (token.startsWith("\u0627\u0644") && token.length() > 3 && !token.equals("\u0627\u0644\u0644\u0647")) {
            token = token.substring(2);
        }
        return new PhoneticKey(arabicFine(token), arabicCoarse(token));
    }

    private static String arabicFine(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            char prev = i > 0 ? token.charAt(i - 1) : 0;
            char next = i + 1 < token.length() ? token.charAt(i + 1) : 0;
            switch (c) {
                case '\u0627' -> out.append('a');                    // ا
                case '\u0648' -> {                                   // و
                    if (i == 0) {
                        out.append('w');
                    } else if (isArabicConsonant(prev) && isArabicConsonant(next)) {
                        out.append('u');
                    } else if (isArabicVowel(prev)) {
                        out.append('u');
                    } else {
                        out.append('w');
                    }
                }
                case '\u064A' -> out.append('i');                    // ي
                case '\u0629', '\u0647' -> out.append('h');           // ة / ه
                case '\u062D' -> out.append('7');                    // ح
                case '\u062E' -> out.append('5');                    // خ
                case '\u0639' -> out.append('3');                    // ع
                case '\u0621' -> out.append('2');                    // ء
                case '\u0642' -> out.append('q');                    // ق
                case '\u063A' -> out.append('g');                    // غ
                case '\u062B' -> out.append('t');                    // ث
                case '\u0630' -> out.append('d');                    // ذ
                case '\u0635' -> out.append('s');                    // ص
                case '\u0636' -> out.append('d');                    // ض
                case '\u0637' -> out.append('t');                    // ط
                case '\u0638' -> out.append('d');                    // ظ
                case '\u0634' -> out.append('c');                    // ش
                case '\u062C' -> out.append('j');                    // ج
                case '\u0628' -> out.append('b');
                case '\u062A' -> out.append('t');
                case '\u062F' -> out.append('d');
                case '\u0631' -> out.append('r');
                case '\u0632' -> out.append('z');
                case '\u0633' -> out.append('s');
                case '\u0641' -> out.append('f');
                case '\u0643' -> out.append('k');
                case '\u0644' -> out.append('l');
                case '\u0645' -> out.append('m');
                case '\u0646' -> out.append('n');
                default -> {
                    if (Character.isDigit(c)) {
                        out.append(c);
                    }
                }
            }
        }
        return collapseDuplicates(out.toString());
    }

    private static String arabicCoarse(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            switch (c) {
                case '\u0627', '\u064A' -> { }                       // ا، ي voyelles longues
                case '\u0648' -> {                                   // و : w seulement initial
                    if (i == 0) {
                        out.append('w');
                    }
                }
                case '\u0629' -> { }                                 // ة finale
                case '\u0647' -> {
                    boolean finalH = i == token.length() - 1 && i > 1;
                    if (!finalH) {
                        out.append('h');
                    }
                }
                case '\u062D' -> out.append('h');                    // ح
                case '\u062E' -> out.append('k');                    // خ
                case '\u0639', '\u0621' -> { }                       // ع، ء marqueurs faibles
                case '\u0642' -> out.append('k');                    // ق
                case '\u063A' -> out.append('g');
                case '\u062B' -> out.append('t');
                case '\u0630' -> out.append('d');
                case '\u0635' -> out.append('s');
                case '\u0636' -> out.append('d');
                case '\u0637' -> out.append('t');
                case '\u0638' -> out.append('d');
                case '\u0634' -> out.append('c');
                case '\u062C' -> out.append('j');
                case '\u0628' -> out.append('b');
                case '\u062A' -> out.append('t');
                case '\u062F' -> out.append('d');
                case '\u0631' -> out.append('r');
                case '\u0632' -> out.append('z');
                case '\u0633' -> out.append('s');
                case '\u0641' -> out.append('f');
                case '\u0643' -> out.append('k');
                case '\u0644' -> out.append('l');
                case '\u0645' -> out.append('m');
                case '\u0646' -> out.append('n');
                default -> {
                    if (Character.isDigit(c)) {
                        out.append(c);
                    }
                }
            }
        }
        return collapseDuplicates(out.toString());
    }

    private static boolean isLatinVowel(char c) {
        return "aeiouyàâäéèêëîïôöùûüÿœæ".indexOf(Character.toLowerCase(c)) >= 0;
    }

    private static boolean isArabicVowel(char c) {
        return c == '\u0627' || c == '\u0648' || c == '\u064A'
                || (c >= 0x064B && c <= 0x065F);
    }

    private static boolean isArabicConsonant(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC && !isArabicVowel(c);
    }

    private static String collapseDuplicates(String value) {
        if (value.length() < 2) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        char previous = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != previous) {
                out.append(c);
            }
            previous = c;
        }
        return out.toString();
    }
}
