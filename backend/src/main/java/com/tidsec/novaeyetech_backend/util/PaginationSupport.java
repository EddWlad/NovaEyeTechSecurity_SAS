package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Traduce {@link PaginationQuery} a {@link Pageable}, acotando limites igual que el backend original. */
public final class PaginationSupport {

    public static final int DEFAULT_LIMIT = 10;
    /** La auditoria pagina de 20 en 20 por defecto. */
    public static final int AUDIT_DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 100;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private PaginationSupport() {
    }

    public static Pageable toPageable(PaginationQuery query, int defaultLimit) {
        int page = Math.max(1, query.getPage() == null ? 1 : query.getPage());
        int limit = clampLimit(query.getLimit() == null ? defaultLimit : query.getLimit());

        return PageRequest.of(page - 1, limit, NEWEST_FIRST);
    }

    public static Sort newestFirst() {
        return NEWEST_FIRST;
    }

    public static int clampLimit(int limit) {
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }
}
