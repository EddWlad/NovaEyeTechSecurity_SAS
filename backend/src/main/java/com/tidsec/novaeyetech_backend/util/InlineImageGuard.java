package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;

/**
 * Las imagenes solo entran por los endpoints de subida, nunca por el cuerpo de un POST/PATCH.
 *
 * <p>Un base64 en la peticion metia decenas de KB por fila en la base y viajaba completo en cada
 * listado: con 653 productos ya eran 17 MB. Cualquier otra URL enviada en el cuerpo se ignora
 * (el servicio la pone en null antes de mapear), porque una URL ajena guardada en la entidad se
 * borraria del almacenamiento al reemplazar la imagen.
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
