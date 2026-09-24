package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;

/**
 * Impide que una imagen entre a la base como data URL.
 *
 * <p>Las imagenes van al almacenamiento externo y en la base solo queda su URL. Un base64 pesa
 * decenas de KB por fila y viaja completo en cada listado: con 653 productos ya eran 17 MB.
 */
public final class InlineImageGuard {

    private static final String DATA_URL_PREFIX = "data:";

    private InlineImageGuard() {
    }

    public static void reject(String value) {
        if (value != null && value.regionMatches(true, 0, DATA_URL_PREFIX, 0, DATA_URL_PREFIX.length())) {
            throw new BusinessRuleException(
                    "No se aceptan imagenes en base64: suba el archivo con el endpoint de imagen.");
        }
    }
}
