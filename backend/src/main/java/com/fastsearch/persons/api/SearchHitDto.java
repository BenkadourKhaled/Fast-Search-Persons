package com.fastsearch.persons.api;

import com.fastsearch.persons.search.Confidence;
import com.fastsearch.persons.search.SearchHit;

import java.util.List;

/** Candidat exposé par l'API : jamais présenté comme une identification. */
public record SearchHitDto(
        String id,
        String lastName,
        String firstName,
        String fullName,
        String birthDate,
        String birthPrecision,
        String birthPlace,
        String fatherName,
        String motherName,
        String ownerGroup,
        String lang,
        double score,
        Confidence confidence,
        List<FieldExplanationDto> explanations
) {

    public static SearchHitDto from(SearchHit hit, boolean includeExplanation) {
        var person = hit.person();
        return new SearchHitDto(person.id(), person.lastName(), person.firstName(),
                person.fullName(),
                person.birthDate() == null ? null : person.birthDate().toString(),
                person.birthPrecision() == null ? null : person.birthPrecision().name(),
                person.birthPlace(), person.fatherName(), person.motherName(),
                person.ownerGroup(), person.lang(),
                hit.score(), hit.confidence(),
                includeExplanation
                        ? hit.explanations().stream().map(FieldExplanationDto::from).toList()
                        : List.of());
    }
}
