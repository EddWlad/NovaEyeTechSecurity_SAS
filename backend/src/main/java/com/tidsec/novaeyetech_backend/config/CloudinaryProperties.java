package com.tidsec.novaeyetech_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales de Cloudinary.
 *
 * <p>Sin valores por defecto: son secretos y llegan del entorno o del archivo {@code .env} local.
 *
 * @param cloudName  nombre de la cuenta
 * @param apiKey     clave publica
 * @param apiSecret  clave privada, nunca sale de la aplicacion
 * @param rootFolder carpeta raiz bajo la que se agrupa todo lo que sube este backend
 */
@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String rootFolder
) {

    public boolean isConfigured() {
        return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
