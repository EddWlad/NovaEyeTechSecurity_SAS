package com.tidsec.novaeyetech_backend.dto;

import java.util.List;

/**
 * Vista previa de un PDF: una imagen por pagina como data URL PNG, lista para el {@code src} de un
 * {@code <img>}. Va en una sola respuesta JSON para que el frontend la pida con su cliente HTTP
 * autenticado (una etiqueta img no envia el token).
 */
public record PdfPreviewDTO(List<String> pages) {
}
