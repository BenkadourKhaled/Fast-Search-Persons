package com.fastsearch.persons.api;

import com.fastsearch.persons.audit.AuditEvent;
import com.fastsearch.persons.audit.AuditEventDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record AuditPageDto(
        List<AuditEventDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static AuditPageDto from(Page<AuditEvent> page) {
        return new AuditPageDto(page.getContent().stream().map(AuditEventDto::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
