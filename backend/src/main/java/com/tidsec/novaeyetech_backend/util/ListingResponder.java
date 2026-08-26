package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.dto.common.PageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Resuelve la paginacion de doble modo en un unico lugar.
 *
 * <p>Sin {@code ?page} el endpoint devuelve un array plano; con {@code ?page} devuelve un
 * {@link PageResponse}. El frontend depende de esa dualidad: los selects y lookups consumen el array
 * y las tablas consumen el objeto paginado, asi que romper cualquiera de los dos modos rompe
 * pantallas distintas.
 */
@Component
@RequiredArgsConstructor
public class ListingResponder {

    private final DtoMapper dtoMapper;

    public <E, D> ResponseEntity<Object> respond(PaginationQuery query,
                                                 int defaultLimit,
                                                 Supplier<List<E>> findAll,
                                                 Function<Pageable, Page<E>> findPage,
                                                 Class<D> dtoType) {
        if (!query.isPaginated()) {
            return ResponseEntity.ok(dtoMapper.mapList(findAll.get(), dtoType));
        }

        Page<E> page = findPage.apply(PaginationSupport.toPageable(query, defaultLimit));

        return ResponseEntity.ok(PageResponse.from(page, dtoMapper.mapList(page.getContent(), dtoType)));
    }
}
