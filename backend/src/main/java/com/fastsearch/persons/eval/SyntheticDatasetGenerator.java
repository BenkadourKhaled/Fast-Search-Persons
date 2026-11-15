package com.fastsearch.persons.eval;

import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.search.PersonRecord;
import com.fastsearch.persons.search.SearchRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Générateur déterministe de personnes et de requêtes synthétiques.
 *
 * <p>Aucune donnée réelle n'est utilisée. Les noms viennent du dictionnaire
 * embarqué ; les dates, lieux et groupes sont inventés. Les cas couvrent
 * l'exact, l'inversion, l'arabe, la translittération, la faute de frappe, la
 * date et les homonymes.</p>
 */
public final class SyntheticDatasetGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[][] FIRST_NAMES = {
            {"وليد", "Walid"},
            {"كريم", "Karim"},
            {"أمين", "Amine"},
            {"ياسين", "Yacine"},
            {"سفيان", "Sofiane"},
            {"نبيل", "Nabil"},
            {"رشيد", "Rachid"},
            {"سمير", "Samir"},
            {"ليلى", "Leila"},
            {"مريم", "Meriem"},
            {"فاطمة", "Fatima"},
            {"سارة", "Sara"}
    };

    private static final String[][] LAST_NAMES = {
            {"بن علي", "Benali"},
            {"بوزيد", "Bouzid"},
            {"لونيس", "Lounes"},
            {"حمادي", "Hamadi"},
            {"سعيد", "Said"},
            {"بلقاسم", "Belkacem"},
            {"مقران", "Mokrane"},
            {"بوعلام", "Boualem"},
            {"ناصر", "Nacer"},
            {"رشيد", "Rachid"},
            {"فؤاد", "Fouad"},
            {"سمير", "Samir"}
    };

    private static final String[] PLACES = {
            "Alger", "Oran", "Constantine", "Annaba", "Tlemcen", "Sétif"
    };

    private final NameDictionary dictionary;

    public SyntheticDatasetGenerator(NameDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public SyntheticDataset generate(int requestedCount) {
        int count = Math.max(12, Math.min(requestedCount, 120));
        List<PersonRecord> persons = new ArrayList<>();
        List<PersonRecord> bases = new ArrayList<>();
        Map<String, Set<String>> idsByName = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            int firstIndex = i % FIRST_NAMES.length;
            int lastIndex = (i / FIRST_NAMES.length) % LAST_NAMES.length;
            String arabicFirst = FIRST_NAMES[firstIndex][0];
            String latinFirst = FIRST_NAMES[firstIndex][1];
            String arabicLast = LAST_NAMES[lastIndex][0];
            String latinLast = LAST_NAMES[lastIndex][1];
            LocalDate birthDate = LocalDate.of(1980 + (i * 7) % 25,
                    1 + (i * 5) % 12, 1 + (i * 11) % 27);
            PersonRecord person = PersonRecord.builder()
                    .id(String.format("P%04d", i + 1))
                    .lastName(latinLast)
                    .firstName(latinFirst)
                    .birthDate(birthDate)
                    .birthPrecision(DatePrecision.JOUR)
                    .birthPlace(PLACES[i % PLACES.length])
                    .fatherName("")
                    .motherName("")
                    .ownerGroup(i % 6 == 0 ? "B" : "A")
                    .lang(i % 2 == 0 ? "fr" : "ar")
                    .build();
            persons.add(person);
            bases.add(person);
            idsByName.computeIfAbsent(nameKey(latinLast, latinFirst), k -> new LinkedHashSet<>())
                    .add(person.id());
        }

        // Trois homonymes synthétiques : même nom, date différente.
        for (int i = 0; i < 3 && i < bases.size(); i++) {
            PersonRecord base = bases.get(i);
            PersonRecord duplicate = PersonRecord.builder()
                    .id(base.id() + "D")
                    .lastName(base.lastName())
                    .firstName(base.firstName())
                    .birthDate(base.birthDate().plusDays(40))
                    .birthPrecision(DatePrecision.JOUR)
                    .birthPlace(base.birthPlace())
                    .ownerGroup(base.ownerGroup())
                    .lang(base.lang())
                    .build();
            persons.add(duplicate);
            idsByName.computeIfAbsent(nameKey(base.lastName(), base.firstName()),
                    k -> new LinkedHashSet<>()).add(duplicate.id());
        }

        List<EvalCase> cases = new ArrayList<>();
        int caseNumber = 1;
        for (int i = 0; i < bases.size(); i++) {
            PersonRecord person = bases.get(i);
            int firstIndex = i % FIRST_NAMES.length;
            int lastIndex = (i / FIRST_NAMES.length) % LAST_NAMES.length;
            String arabicFirst = FIRST_NAMES[firstIndex][0];
            String arabicLast = LAST_NAMES[lastIndex][0];
            Set<String> group = Set.of(person.ownerGroup());
            Set<String> expected = idsByName.get(nameKey(person.lastName(), person.firstName()));

            cases.add(new EvalCase("E" + caseNumber++, "exact",
                    SearchRequest.structured(person.lastName(), person.firstName(), "", "",
                            group, 0, 10), expected));
            cases.add(new EvalCase("E" + caseNumber++, "inversion",
                    SearchRequest.free(person.firstName() + " " + person.lastName(),
                            group, 0, 10, true), expected));
            cases.add(new EvalCase("E" + caseNumber++, "arabic",
                    SearchRequest.free(arabicFirst + " " + arabicLast, group, 0, 10, true),
                    expected));
            cases.add(new EvalCase("E" + caseNumber++, "transliteration",
                    SearchRequest.free(latinVariant(person.firstName()) + " "
                            + latinVariant(person.lastName()), group, 0, 10, true), expected));
            cases.add(new EvalCase("E" + caseNumber++, "typo",
                    SearchRequest.free(person.firstName() + " " + typo(person.lastName()),
                            group, 0, 10, true), expected));
            cases.add(new EvalCase("E" + caseNumber++, "date",
                    SearchRequest.structured(person.lastName(), person.firstName(),
                            person.birthDate().format(DATE_FORMAT), "", group, 0, 10),
                    Set.of(person.id())));
        }

        // Cas homonymes explicites : la date doit choisir le bon dossier.
        for (int i = 0; i < 3 && i < bases.size(); i++) {
            PersonRecord base = bases.get(i);
            cases.add(new EvalCase("E" + caseNumber++, "homonym",
                    SearchRequest.structured(base.lastName(), base.firstName(),
                            base.birthDate().format(DATE_FORMAT), "",
                            Set.of(base.ownerGroup()), 0, 10),
                    Set.of(base.id())));
        }

        return new SyntheticDataset(persons, cases);
    }

    private String latinVariant(String name) {
        Set<String> variants = dictionary.knownVariants(name);
        String normalized = NameNormalizer.normalizeToken(name);
        for (String variant : variants) {
            String candidate = variant.trim();
            if (candidate.isBlank() || NameNormalizer.containsArabic(candidate)) {
                continue;
            }
            if (NameNormalizer.normalizeToken(candidate).equals(normalized)) {
                continue;
            }
            if (candidate.length() >= Math.max(3, name.length() - 1)) {
                return candidate;
            }
        }
        return name;
    }

    private static String typo(String name) {
        String value = name.trim();
        if (value.length() <= 3) {
            return value + "e";
        }
        return value.substring(0, value.length() - 1);
    }

    private static String nameKey(String last, String first) {
        return NameNormalizer.normalizeToken(last) + "|" + NameNormalizer.normalizeToken(first);
    }
}
