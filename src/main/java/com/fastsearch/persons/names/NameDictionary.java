package com.fastsearch.persons.names;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dictionnaire de graphies, chargé depuis le classpath ou depuis un fichier externe.
 *
 * <p>Format : une ligne par nom, séparateur point-virgule, première graphie
 * considérée comme canonique. Les lignes vides et commençant par # sont ignorées.
 * Aucun produit cartésien n'est fait : les variantes sont développées par mot
 * (ou par expression explicitement présente dans le dictionnaire).</p>
 */
public final class NameDictionary {

    private static final String DEFAULT_RESOURCE = "/names/name-variants.csv";

    private final List<NameEntry> entries;
    private final Map<String, List<NameEntry>> byVariant;

    private NameDictionary(List<NameEntry> entries) {
        this.entries = List.copyOf(entries);
        Map<String, List<NameEntry>> index = new LinkedHashMap<>();
        for (NameEntry entry : entries) {
            for (String graphy : entry.graphies()) {
                index.computeIfAbsent(NameNormalizer.normalizeToken(graphy), k -> new ArrayList<>()).add(entry);
            }
        }
        this.byVariant = Map.copyOf(index);
    }

    public static NameDictionary loadDefault() {
        try (InputStream in = NameDictionary.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Dictionnaire introuvable dans le classpath : " + DEFAULT_RESOURCE);
            }
            return load(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du dictionnaire impossible", e);
        }
    }

    public static NameDictionary load(Path path) {
        if (path == null) {
            return loadDefault();
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Dictionnaire externe introuvable : " + path);
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du dictionnaire externe impossible : " + path, e);
        }
    }

    /** Chemin externe si fourni, sinon ressource embarquée. */
    public static NameDictionary load(Path externalPath, boolean ignored) {
        return externalPath == null ? loadDefault() : load(externalPath);
    }

    private static NameDictionary load(BufferedReader reader) throws IOException {
        List<NameEntry> entries = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(";", -1);
            List<String> graphies = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (String part : parts) {
                String graphy = part.trim();
                if (!graphy.isEmpty() && seen.add(NameNormalizer.normalizeToken(graphy))) {
                    graphies.add(graphy);
                }
            }
            if (!graphies.isEmpty()) {
                entries.add(new NameEntry(graphies));
            }
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("Dictionnaire vide");
        }
        return new NameDictionary(entries);
    }

    public List<NameEntry> entries() {
        return entries;
    }

    public Optional<NameEntry> lookup(String graphy) {
        String normalized = NameNormalizer.normalizeToken(graphy);
        List<NameEntry> found = byVariant.get(normalized);
        if (found == null || found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    /** Variantes connues d'un mot, sans produit cartésien. */
    public Set<String> variantsForToken(String token) {
        String normalized = NameNormalizer.normalizeToken(token);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Set<String> variants = new LinkedHashSet<>();
        variants.add(normalized);
        String stripped = NameNormalizer.stripParticlePrefix(normalized);
        if (!stripped.isBlank()) {
            variants.add(stripped);
            addVariantsOf(stripped, variants);
        }
        addVariantsOf(normalized, variants);
        return Collections.unmodifiableSet(variants);
    }

    private void addVariantsOf(String normalized, Set<String> target) {
        List<NameEntry> found = byVariant.get(normalized);
        if (found == null) {
            return;
        }
        for (NameEntry entry : found) {
            for (String graphy : entry.graphies()) {
                String variant = NameNormalizer.normalizeToken(graphy);
                if (!variant.isBlank()) {
                    target.add(variant);
                }
            }
        }
    }

    /**
     * Variantes d'un nom complet : union des variantes de chaque mot et, si une
     * expression complète existe dans le dictionnaire, de ses variantes.
     * Aucun produit cartésien : le nombre de variantes reste borné par le CSV.
     */
    public Set<String> variantsForName(String fullName) {
        Set<String> variants = new LinkedHashSet<>();
        String normalized = NameNormalizer.normalizeToken(fullName);
        if (normalized.isBlank()) {
            return variants;
        }
        variants.add(normalized);
        addVariantsOf(normalized, variants);
        for (String token : NameNormalizer.tokens(fullName)) {
            if (!NameNormalizer.isParticle(token)) {
                variants.addAll(variantsForToken(token));
            }
        }
        return Collections.unmodifiableSet(variants);
    }

    /** Vrai si les deux graphies (ou deux de leurs mots) partagent une entrée. */
    public boolean sameEntry(String left, String right) {
        String a = NameNormalizer.normalizeToken(left);
        String b = NameNormalizer.normalizeToken(right);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        if (shareEntry(a, b)) {
            return true;
        }
        for (String ta : NameNormalizer.tokens(left)) {
            for (String tb : NameNormalizer.tokens(right)) {
                if (NameNormalizer.isParticle(ta) || NameNormalizer.isParticle(tb)) {
                    continue;
                }
                if (shareEntry(NameNormalizer.normalizeToken(ta), NameNormalizer.normalizeToken(tb))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shareEntry(String a, String b) {
        List<NameEntry> leftEntries = byVariant.get(a);
        if (leftEntries == null) {
            leftEntries = byVariant.get(NameNormalizer.stripParticlePrefix(a));
        }
        if (leftEntries == null) {
            return false;
        }
        for (NameEntry entry : leftEntries) {
            for (String graphy : entry.graphies()) {
                if (NameNormalizer.normalizeToken(graphy).equals(b)
                        || NameNormalizer.normalizeToken(graphy).equals(NameNormalizer.stripParticlePrefix(b))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Vrai si les deux graphies complètes (pas seulement un mot) partagent une entrée. */
    public boolean samePhraseEntry(String left, String right) {
        String a = NameNormalizer.normalizeToken(left);
        String b = NameNormalizer.normalizeToken(right);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        List<NameEntry> candidates = byVariant.get(a);
        if (candidates == null) {
            candidates = byVariant.get(NameNormalizer.stripParticlePrefix(a));
        }
        if (candidates == null) {
            return false;
        }
        for (NameEntry entry : candidates) {
            for (String graphy : entry.graphies()) {
                String normalized = NameNormalizer.normalizeToken(graphy);
                if (normalized.equals(b) || normalized.equals(NameNormalizer.stripParticlePrefix(b))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Suggestion de variantes pour une graphie inconnue (utilisée par eval). */
    public Set<String> knownVariants(String graphy) {
        return variantsForToken(graphy);
    }
}
