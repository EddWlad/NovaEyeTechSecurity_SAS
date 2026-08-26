package com.tidsec.novaeyetech_backend.dto.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Contrato de respuesta paginada. Los nombres de campo son parte del contrato publico:
 * el frontend Angular (`ApiService.listPaginated`) los consume tal cual.
 */
public record PageResponse<T>(
        int page,
        int limit,
        long total,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        List<T> items
) {

    /** Construye la respuesta a partir de una pagina de Spring Data, mapeando cada elemento a su DTO. */
    public static <E, D> PageResponse<D> from(Page<E> source, List<D> items) {
        int totalPages = Math.max(1, source.getTotalPages());
        int page = source.getNumber() + 1;

        return new PageResponse<>(
                page,
                source.getSize(),
                source.getTotalElements(),
                totalPages,
                page < totalPages,
                page > 1,
                items);
    }
}
