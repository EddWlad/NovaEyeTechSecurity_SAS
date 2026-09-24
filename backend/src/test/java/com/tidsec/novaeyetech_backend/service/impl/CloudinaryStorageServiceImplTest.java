package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tidsec.novaeyetech_backend.config.CloudinaryProperties;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * deleteByUrl recibe URLs que pueden venir de una peticion del cliente (avatarDataUrl, imageUrl).
 * Solo debe borrar lo que este backend subio: de lo contrario alguien podria apuntar una URL a un
 * archivo ajeno de la cuenta compartida y eliminarlo al reemplazar su propia foto.
 */
@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceImplTest {

    private static final String OWN_URL =
            "https://res.cloudinary.com/mi-cuenta/image/upload/v1/novaeyetech/products/foto_ab.jpg";

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private CloudinaryStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryStorageServiceImpl(cloudinary,
                new CloudinaryProperties("mi-cuenta", "key", "secret", "novaeyetech"));
    }

    @Test
    @DisplayName("Borra un recurso propio deduciendo su public_id")
    void deletesOwnResource() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("novaeyetech/products/foto_ab"), any())).thenReturn(Map.of("result", "ok"));

        assertThat(service.deleteByUrl(OWN_URL)).isTrue();
    }

    @Test
    @DisplayName("No borra un recurso de otra cuenta de Cloudinary")
    void ignoresForeignAccount() throws IOException {
        String foreign = "https://res.cloudinary.com/otra-cuenta/image/upload/v1/novaeyetech/products/foto.jpg";

        assertThat(service.deleteByUrl(foreign)).isFalse();
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("No borra un recurso fuera de la carpeta raiz configurada")
    void ignoresOtherFolder() throws IOException {
        String otherApp = "https://res.cloudinary.com/mi-cuenta/image/upload/v1/sisgop/expedientes/foto.jpg";

        assertThat(service.deleteByUrl(otherApp)).isFalse();
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("Ignora data URLs heredados, rutas locales y valores vacios")
    void ignoresNonProviderValues() {
        assertThat(service.deleteByUrl("data:image/jpeg;base64,AAAA")).isFalse();
        assertThat(service.deleteByUrl("/uploads/viejo.png")).isFalse();
        assertThat(service.deleteByUrl(null)).isFalse();
        assertThat(service.deleteByUrl("  ")).isFalse();
    }
}
