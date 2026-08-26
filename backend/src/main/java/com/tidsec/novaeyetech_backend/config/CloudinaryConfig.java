package com.tidsec.novaeyetech_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente de Cloudinary.
 *
 * <p>El bean se crea siempre, incluso sin credenciales: asi la aplicacion arranca en un entorno
 * donde todavia no se configuro el almacenamiento y solo falla, con un mensaje claro, cuando alguien
 * intenta subir un archivo. Quien valida es {@code CloudinaryStorageServiceImpl}.
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true
        ));
    }
}
