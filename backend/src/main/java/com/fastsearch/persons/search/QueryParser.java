package com.fastsearch.persons.search;

import com.fastsearch.persons.dates.DateNormalizer;
import com.fastsearch.persons.dates.DateParseResult;
import com.fastsearch.persons.dates.DateParser;
import com.fastsearch.persons.dates.ParsedDate;
import com.fastsearch.persons.names.FullNameParser;
import com.fastsearch.persons.names.NameHypothesis;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.names.ParsedName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Transforme une requête REST en requête interne.
 *
 * <p>Une suite de 5 à 8 chiffres en chaîne libre est ambiguë : elle est
 * interprétée à la fois comme date et comme identifiant, avec un avertissement.
 * Une date seule ne produit que des résultats FAIBLE (elle n'identifie pas).</p>
 */
public final class QueryParser {

    private final DateParser dateParser;
    private final FullNameParser nameParser;
    private final int maxPageSize;

    public QueryParser(DateParser dateParser, FullNameParser nameParser, int maxPageSize) {
        this.dateParser = dateParser;
        this.nameParser = nameParser;
        this.maxPageSize = Math.max(1, maxPageSize);
    }

    public ParsedSearchQuery parse(SearchRequest request) {
        if (request == null) {
            return ParsedSearchQuery.invalid("requête absente", Set.of());
        }
        Set<String> warnings = new LinkedHashSet<>();
        int size = clampSize(request.size(), warnings);
        int page = Math.max(0, request.page());
        if (request.q().isBlank()) {
            return parseStructured(request, warnings, page, size);
        }
        return parseFree(request, warnings, page, size);
    }

    private ParsedSearchQuery parseStructured(SearchRequest request, Set<String> warnings,
                                              int page, int size) {
        String lastName = request.lastName().trim();
        String firstName = request.firstName().trim();
        if (lastName.isBlank() && firstName.isBlank()) {
            return ParsedSearchQuery.invalid(
                    "au moins un nom ou un prénom est requis (une date ou un lieu seul est trop vague)",
                    warnings);
        }
        if (!NameNormalizer.hasMeaningfulToken(lastName + " " + firstName)) {
            return ParsedSearchQuery.invalid("nom trop court ou composé uniquement de particules", warnings);
        }
        List<NameHypothesis> hypotheses = new ArrayList<>();
        hypotheses.add(new NameHypothesis(lastName, firstName, false));
        if (!lastName.isBlank() && !firstName.isBlank()
                && !NameNormalizer.normalizeToken(lastName).equals(NameNormalizer.normalizeToken(firstName))) {
            hypotheses.add(new NameHypothesis(firstName, lastName, true));
        }

        List<ParsedDate> dates = List.of();
        if (!request.birthDate().isBlank()) {
            DateParseResult parsed = dateParser.parse(request.birthDate());
            if (parsed.isValid()) {
                dates = parsed.hypotheses();
                if (dates.size() > 1) {
                    warnings.add("date ambiguë : plusieurs hypothèses évaluées");
                }
            } else {
                warnings.add("date de naissance rejetée : " + String.join("; ", parsed.errors()));
            }
        }
        return new ParsedSearchQuery(hypotheses, dates, request.birthPlace(), "", "",
                request.ownerGroups(), page, size, request.includeExplanation(), true, "", warnings, false);
    }

    private ParsedSearchQuery parseFree(SearchRequest request, Set<String> warnings,
                                        int page, int size) {
        String q = request.q().trim();
        List<String> tokens = List.of(q.split("\\s+"));
        if (tokens.isEmpty()) {
            return ParsedSearchQuery.invalid("requête vide", warnings);
        }

        List<ParsedDate> dates = List.of();
        String idQuery = "";
        boolean numericAmbiguity = false;
        String namePart = q;

        if (tokens.size() == 1 && tokens.get(0).matches("\\d{5,8}")) {
            String numeric = tokens.get(0);
            DateParseResult parsed = dateParser.parse(numeric);
            if (parsed.isValid()) {
                dates = parsed.hypotheses();
                idQuery = numeric;
                numericAmbiguity = true;
                warnings.add("suite de chiffres ambiguë : recherche effectuée à la fois comme date et comme identifiant");
            } else {
                idQuery = numeric;
                warnings.add("suite de chiffres non valide comme date : traitée comme identifiant");
            }
            namePart = "";
        } else {
            // Cherche le plus long suffixe interprétable comme date (nom + date).
            int dateStart = -1;
            DateParseResult dateResult = null;
            for (int start = 1; start < tokens.size(); start++) {
                String candidate = String.join(" ", tokens.subList(start, tokens.size()));
                if (!looksLikeDate(candidate)) {
                    continue;
                }
                DateParseResult parsed = dateParser.parse(candidate);
                if (parsed.isValid()) {
                    dateStart = start;
                    dateResult = parsed;
                    break;
                }
            }
            if (dateStart < 0 && tokens.size() >= 1 && looksLikeDate(q)) {
                DateParseResult parsed = dateParser.parse(q);
                if (parsed.isValid()) {
                    dateStart = 0;
                    dateResult = parsed;
                }
            }
            if (dateResult != null) {
                dates = dateResult.hypotheses();
                namePart = dateStart == 0 ? "" : String.join(" ", tokens.subList(0, dateStart));
                String dateToken = String.join(" ", tokens.subList(dateStart, tokens.size()));
                if (dateToken.matches("\\d{5,8}")) {
                    idQuery = dateToken;
                    numericAmbiguity = true;
                    warnings.add("suffixe numérique ambigu : date et identifiant recherchés");
                }
                if (dates.size() > 1) {
                    warnings.add("date ambiguë : plusieurs hypothèses évaluées");
                }
            } else if (tokens.size() == 1 && tokens.get(0).matches("\\d{1,8}")) {
                idQuery = tokens.get(0);
                namePart = "";
                warnings.add("chiffres traités comme identifiant (date non reconnue)");
            }
        }

        List<NameHypothesis> names = List.of();
        if (!namePart.isBlank()) {
            ParsedName parsedName = nameParser.parse(namePart);
            if (parsedName.hasMeaningfulToken()) {
                names = parsedName.hypotheses();
            } else if (dates.isEmpty() && idQuery.isBlank()) {
                return ParsedSearchQuery.invalid("nom trop court ou trop vague", warnings);
            } else {
                warnings.add("partie nominale ignorée car trop vague");
            }
        }

        if (names.isEmpty() && dates.isEmpty() && idQuery.isBlank()) {
            return ParsedSearchQuery.invalid("aucun critère exploitable", warnings);
        }
        if (names.isEmpty() && dates.isEmpty() && !idQuery.isBlank()
                && !idQuery.matches("\\d{1,32}")) {
            return ParsedSearchQuery.invalid("identifiant invalide", warnings);
        }
        return new ParsedSearchQuery(names, dates, "", idQuery, q, request.ownerGroups(),
                page, size, request.includeExplanation(), true, "", warnings, numericAmbiguity);
    }

    private boolean looksLikeDate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String prepared = DateNormalizer.prepare(value);
        if (prepared.matches("\\d{5,8}")) {
            return true;
        }
        if (prepared.matches(".*\\d.*") && prepared.matches(".*[/.\\-\\s].*")) {
            return true;
        }
        if (prepared.matches(".*\\d.*")) {
            for (String token : prepared.split("\\s+")) {
                if (DateNormalizer.monthNumber(token).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int clampSize(int requested, Set<String> warnings) {
        int size = requested <= 0 ? 20 : requested;
        if (size > maxPageSize) {
            warnings.add("taille de page ramenée au maximum serveur de " + maxPageSize);
            size = maxPageSize;
        }
        return size;
    }
}
