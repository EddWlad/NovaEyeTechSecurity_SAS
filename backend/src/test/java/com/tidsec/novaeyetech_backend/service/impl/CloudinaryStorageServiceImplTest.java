package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tidsec.novaeyetech_backend.config.CloudinaryProperties;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * download y deleteByUrl solo actuan sobre lo que este backend subio (host de Cloudinary, cuenta y
 * carpeta raiz propias). Descargar una URL cualquiera permitiria al servidor leer direcciones internas
 * (SSRF); borrarla, eliminar archivos ajenos de la cuenta compartida.
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
    @DisplayName("No borra una URL de otro dominio aunque su ruta imite la de la cuenta")
    void ignoresLookalikeHost() throws IOException {
        String lookalike = "https://evil.example/mi-cuenta/image/upload/v1/novaeyetech/products/foto.jpg";

        assertThat(service.deleteByUrl(lookalike)).isFalse();
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("La descarga rechaza direcciones internas y hosts ajenos sin hacer la peticion")
    void downloadRejectsForeignUrls() {
        for (String url : new String[] {
                "http://127.0.0.1:8080/api/users",
                "http://169.254.169.254/latest/meta-data/",
                "https://evil.example/mi-cuenta/image/upload/v1/novaeyetech/x.jpg",
                "https://res.cloudinary.com/otra-cuenta/image/upload/v1/novaeyetech/x.jpg",
                "https://res.cloudinary.com/mi-cuenta/image/upload/v1/sisgop/x.jpg",
                "/uploads/viejo.png"}) {
            assertThatThrownBy(() -> service.download(url))
                    .as(url)
                    .isInstanceOf(ResourceNotFoundException.class);
        }
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
