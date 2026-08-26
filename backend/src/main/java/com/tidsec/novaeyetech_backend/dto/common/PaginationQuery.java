package com.tidsec.novaeyetech_backend.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Query string de paginacion de doble modo.
 *
 * <p>Regla transversal heredada del backend NestJS: la paginacion se activa <b>solo</b> si llega
 * {@code ?page}. Sin ese parametro el endpoint devuelve un array plano, que es lo que consumen los
 * selects y lookups del frontend.
 */
@Getter
@Setter
public class PaginationQuery {

    @Min(value = 1, message = "page debe ser mayor o igual a 1")
    private Integer page;

    @Min(value = 1, message = "limit debe ser mayor o igual a 1")
    @Max(value = 100, message = "limit no puede superar 100")
    private Integer limit;

    /** Verdadero solo cuando el cliente pidio explicitamente una pagina. */
    public boolean isPaginated() {
        return page != null;
    }
}
