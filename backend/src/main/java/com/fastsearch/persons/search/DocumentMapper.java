package com.fastsearch.persons.search;

import com.fastsearch.persons.analysis.NgramAnalyzer;
import com.fastsearch.persons.dates.DateFlag;
import com.fastsearch.persons.dates.DateNormalizer;
import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.names.NameVariantGenerator;
import com.fastsearch.persons.names.PhoneticKey;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduit un {@link PersonRecord} en document Lucene.
 *
 * <p>L'original n'est jamais écrasé : les champs *_raw sont stockés tels quels.
 * Les variantes du dictionnaire, les deux clés phonétiques et les trigrammes
 * sont des champs séparés. Un seul document par source_id.</p>
 */
public final class DocumentMapper {

    private final NameVariantGenerator variants;
    private final NameAnalyzerHolder analyzers;

    public DocumentMapper(NameDictionary dictionary) {
        this.variants = new NameVariantGenerator(dictionary);
        this.analyzers = new NameAnalyzerHolder();
    }

    public Document toDocument(PersonRecord person) {
        Document doc = new Document();
        String fullName = person.fullName();

        doc.add(new StringField(IndexFields.SOURCE_ID, person.id(), Field.Store.YES));
        doc.add(new StringField(IndexFields.LAST_NAME_RAW, person.lastName(), Field.Store.YES));
        doc.add(new StringField(IndexFields.FIRST_NAME_RAW, person.firstName(), Field.Store.YES));
        doc.add(new StringField(IndexFields.FULL_NAME_RAW, fullName, Field.Store.YES));
        doc.add(new StringField(IndexFields.OWNER_GROUP, person.ownerGroup(), Field.Store.YES));
        doc.add(new StringField(IndexFields.LANG, person.lang(), Field.Store.YES));

        doc.add(new TextField(IndexFields.FULL_NAME_TRANSLIT, fullName, Field.Store.NO));
        doc.add(new TextField(IndexFields.NAME_NGRAM, NameNormalizer.normalizedName(fullName), Field.Store.NO));

        addExactNames(doc, fullName);
        addVariants(doc, fullName, person.lastName(), person.firstName());
        addPhoneticKeys(doc, fullName, person.lastName(), person.firstName());

        if (!person.birthPlace().isBlank()) {
            doc.add(new TextField(IndexFields.BIRTH_PLACE, person.birthPlace(), Field.Store.YES));
            doc.add(new StringField(IndexFields.BIRTH_PLACE_EXACT,
                    NameNormalizer.normalizeToken(person.birthPlace()), Field.Store.NO));
        }
        if (!person.fatherName().isBlank()) {
            doc.add(new TextField(IndexFields.FATHER_NAME, person.fatherName(), Field.Store.YES));
        }
        if (!person.motherName().isBlank()) {
            doc.add(new TextField(IndexFields.MOTHER_NAME, person.motherName(), Field.Store.YES));
        }

        if (person.birthDate() != null) {
            DatePrecision precision = person.birthPrecision() == null
                    ? DatePrecision.JOUR : person.birthPrecision();
            String canonical = DateNormalizer.canonical(person.birthDate(), precision);
            doc.add(new StringField(IndexFields.BIRTH_DATE, canonical, Field.Store.YES));
            doc.add(new StringField(IndexFields.BIRTH_PRECISION, precision.name(), Field.Store.YES));
            String flags = person.birthFlags().stream().map(Enum::name).sorted()
                    .collect(Collectors.joining(","));
            doc.add(new StringField(IndexFields.BIRTH_FLAGS, flags, Field.Store.YES));
            doc.add(new LongPoint(IndexFields.BIRTH_EPOCH_DAY, person.birthDate().toEpochDay()));
            doc.add(new IntPoint(IndexFields.BIRTH_YEAR, person.birthDate().getYear()));
            if (precision != DatePrecision.ANNEE) {
                doc.add(new IntPoint(IndexFields.BIRTH_MONTH, person.birthDate().getMonthValue()));
            }
            if (precision == DatePrecision.JOUR) {
                doc.add(new IntPoint(IndexFields.BIRTH_DAY, person.birthDate().getDayOfMonth()));
            }
        }
        return doc;
    }

    private void addExactNames(Document doc, String fullName) {
        Set<String> exact = new LinkedHashSet<>();
        exact.add(NameNormalizer.normalizedName(fullName));
        exact.add(sorted(NameNormalizer.tokens(fullName)));
        exact.add(sorted(NameNormalizer.tokensForKeys(fullName)));
        for (String value : exact) {
            if (!value.isBlank()) {
                doc.add(new StringField(IndexFields.NAME_EXACT, value, Field.Store.NO));
            }
        }
    }

    private void addVariants(Document doc, String fullName, String lastName, String firstName) {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(variants.variantsForName(fullName));
        all.addAll(variants.variantsForName(lastName));
        all.addAll(variants.variantsForName(firstName));
        for (String token : NameNormalizer.tokens(fullName)) {
            if (!NameNormalizer.isParticle(token)) {
                all.addAll(variants.variantsForToken(token));
            }
        }
        // Variantes triées des mots de clé, pour couvrir l'inversion.
        Set<String> keyTokens = new LinkedHashSet<>();
        for (String token : NameNormalizer.tokensForKeys(fullName)) {
            keyTokens.addAll(variants.variantsForToken(token));
        }
        all.add(sorted(keyTokens));
        for (String value : all) {
            if (!value.isBlank()) {
                doc.add(new StringField(IndexFields.NAME_VARIANTS, value, Field.Store.NO));
            }
        }
    }

    private void addPhoneticKeys(Document doc, String fullName, String lastName, String firstName) {
        Set<String> fines = new LinkedHashSet<>();
        Set<String> coarses = new LinkedHashSet<>();
        for (String name : List.of(fullName, lastName, firstName)) {
            for (String token : NameNormalizer.tokensForKeys(name)) {
                PhoneticKey key = PhoneticKey.of(token);
                if (!key.fine().isBlank()) {
                    fines.add(key.fine());
                    coarses.add(key.coarse());
                }
            }
        }
        for (String key : fines) {
            doc.add(new StringField(IndexFields.NAME_PHONETIC_FINE, key, Field.Store.NO));
        }
        for (String key : coarses) {
            doc.add(new StringField(IndexFields.NAME_PHONETIC_COARSE, key, Field.Store.NO));
        }
    }

    private static String sorted(Collection<String> tokens) {
        return tokens.stream()
                .map(NameNormalizer::normalizeToken)
                .filter(t -> !t.isBlank())
                .sorted()
                .collect(Collectors.joining(" "));
    }

    public PersonRecord fromDocument(Document doc) {
        String id = text(doc, IndexFields.SOURCE_ID);
        String lastName = text(doc, IndexFields.LAST_NAME_RAW);
        String firstName = text(doc, IndexFields.FIRST_NAME_RAW);
        String birthPlace = text(doc, IndexFields.BIRTH_PLACE);
        String fatherName = text(doc, IndexFields.FATHER_NAME);
        String motherName = text(doc, IndexFields.MOTHER_NAME);
        String ownerGroup = text(doc, IndexFields.OWNER_GROUP);
        String lang = text(doc, IndexFields.LANG);
        String canonical = text(doc, IndexFields.BIRTH_DATE);
        String precisionText = text(doc, IndexFields.BIRTH_PRECISION);
        String flagsText = text(doc, IndexFields.BIRTH_FLAGS);

        LocalDate birthDate = null;
        DatePrecision precision = null;
        Set<DateFlag> flags = Set.of();
        if (!canonical.isBlank() && !precisionText.isBlank()) {
            try {
                precision = DatePrecision.valueOf(precisionText);
                if (!flagsText.isBlank()) {
                    EnumSet<DateFlag> parsedFlags = EnumSet.noneOf(DateFlag.class);
                    for (String flag : flagsText.split(",")) {
                        if (!flag.isBlank()) {
                            parsedFlags.add(DateFlag.valueOf(flag.trim()));
                        }
                    }
                    flags = parsedFlags;
                }
                var parsed = DateNormalizer.fromCanonical(canonical, precision, flags, 1.0);
                if (parsed != null) {
                    birthDate = parsed.date();
                }
            } catch (RuntimeException ignored) {
                // Un document corrompu ne doit pas faire tomber toute la recherche.
                birthDate = null;
                precision = null;
                flags = Set.of();
            }
        }
        return new PersonRecord(id, lastName, firstName, birthDate, precision, flags,
                birthPlace, fatherName, motherName, ownerGroup, lang);
    }

    private static String text(Document doc, String field) {
        IndexableField value = doc.getField(field);
        return value == null || value.stringValue() == null ? "" : value.stringValue();
    }

    /** Conteneur d'analyseurs réutilisables (thread-safe, non stateful). */
    public static final class NameAnalyzerHolder implements AutoCloseable {
        public final com.fastsearch.persons.analysis.NameAnalyzer name = new com.fastsearch.persons.analysis.NameAnalyzer();
        public final NgramAnalyzer ngram = new NgramAnalyzer();

        @Override
        public void close() {
            name.close();
            ngram.close();
        }
    }

    public NameAnalyzerHolder analyzers() {
        return analyzers;
    }
}
