package com.tidsec.novaeyetech_backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tidsec.novaeyetech_backend.config.CloudinaryProperties;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.service.IStorageService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Almacenamiento en Cloudinary.
 *
 * <p>Dos detalles del proveedor condicionan la implementacion:
 * <ul>
 *   <li>Las imagenes se suben como {@code resource_type = image} y todo lo demas como {@code raw}.
 *       Cloudinary bloquea con 401 la entrega de un PDF subido como imagen; como {@code raw} se
 *       descarga sin restriccion.</li>
 *   <li>En {@code raw} Cloudinary no agrega la extension por su cuenta, asi que se envia el nombre
 *       original con extension via {@code filename_override}. Sin ella el archivo baja como
 *       "formato desconocido".</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements IStorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final String IMAGE_RESOURCE_TYPE = "image";
    private static final String RAW_RESOURCE_TYPE = "raw";
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String DEFAULT_ROOT_FOLDER = "novaeyetech";
    private static final String DEFAULT_FOLDER = "general";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // Algunos navegadores envian octet-stream para xlsx y docx.
            DEFAULT_MIME_TYPE
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf",
            ".doc", ".docx", ".xls", ".xlsx"
    );

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        // El archivo se valida primero: un tipo no permitido es culpa del cliente y el mensaje debe
        // decir eso, no que al servidor le falten credenciales.
        validate(file);
        requireConfigured();

        String originalName = safeOriginalName(file.getOriginalFilename());
        boolean image = isImage(file.getContentType());
        String resourceType = image ? IMAGE_RESOURCE_TYPE : RAW_RESOURCE_TYPE;
        Path temp = null;

        try {
            temp = Files.createTempFile("novaeyetech_", safeSuffix(originalName));
            Files.write(temp, file.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "folder", resolveFolder(folder),
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );
            if (originalName != null) {
                options.put("filename_override", originalName);
            }

            Map<?, ?> response = cloudinary.uploader().upload(temp.toFile(), options);

            return toStoredFile(response, file, resourceType);
        } catch (IOException ex) {
            log.error("Error subiendo archivo a Cloudinary", ex);
            throw new BusinessRuleException("No se pudo subir el archivo: " + ex.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    @Override
    public byte[] download(String url) {
        // Un adjunto anterior a la migracion guarda una ruta de disco como "/uploads/...". Ese
        // archivo ya no existe, y sin esta comprobacion el cliente HTTP falla con
        // "URI with undefined scheme", que se traduce en un 400 tecnico en vez de un 404 claro.
        if (url == null || url.isBlank() || !url.startsWith("http")) {
            throw new ResourceNotFoundException("Archivo no encontrado en el almacenamiento");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.warn("Cloudinary devolvio {} al descargar {}", response.statusCode(), url);
                throw new ResourceNotFoundException("Archivo no encontrado en el almacenamiento");
            }

            return response.body();
        } catch (URISyntaxException ex) {
            throw new ResourceNotFoundException("La ruta del archivo almacenado no es valida");
        } catch (IOException ex) {
            log.error("Error descargando {} de Cloudinary", url, ex);
            throw new BusinessRuleException("No se pudo descargar el archivo: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException("La descarga del archivo fue interrumpida");
        }
    }

    @Override
    public boolean delete(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        requireConfigured();

        try {
            Map<?, ?> response = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType == null ? IMAGE_RESOURCE_TYPE : resourceType,
                    "invalidate", true));

            return "ok".equals(response.get("result"));
        } catch (IOException ex) {
            // El borrado remoto no debe tumbar el borrado del registro: se reporta y sigue.
            log.warn("No se pudo eliminar {} en Cloudinary: {}", publicId, ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteByUrl(String url) {
        // Un data URL heredado o una ruta local no son recursos del proveedor: no hay nada que borrar.
        if (url == null || !url.startsWith("http") || !url.contains("/upload/")) {
            return false;
        }

        return delete(extractPublicId(url), url.contains("/raw/upload/") ? RAW_RESOURCE_TYPE : IMAGE_RESOURCE_TYPE);
    }

    /**
     * Deriva el public_id de una URL de Cloudinary.
     *
     * <p>De {@code https://res.cloudinary.com/CUENTA/image/upload/v123/novaeyetech/avatars/foo_ab.png}
     * sale {@code novaeyetech/avatars/foo_ab}: se descarta el prefijo hasta {@code /upload/}, la
     * version opcional {@code vNNN/} y la extension.
     */
    private String extractPublicId(String url) {
        int uploadIndex = url.indexOf("/upload/");
        String path = url.substring(uploadIndex + "/upload/".length());

        if (path.startsWith("v") && path.contains("/")) {
            path = path.substring(path.indexOf('/') + 1);
        }

        int dot = path.lastIndexOf('.');

        return dot > 0 ? path.substring(0, dot) : path;
    }

    private StoredFile toStoredFile(Map<?, ?> response, MultipartFile file, String resourceType) {
        Object secureUrl = response.get("secure_url");
        if (secureUrl == null) {
            secureUrl = response.get("url");
        }
        if (secureUrl == null) {
            throw new BusinessRuleException("Cloudinary no devolvio la URL del archivo subido");
        }

        Object publicId = response.get("public_id");
        Object bytes = response.get("bytes");
        long size = bytes instanceof Number number ? number.longValue() : file.getSize();
        String storedName = publicId == null
                ? safeOriginalName(file.getOriginalFilename())
                : lastSegment(publicId.toString());

        return new StoredFile(
                secureUrl.toString(),
                publicId == null ? null : publicId.toString(),
                resourceType,
                storedName,
                size,
                file.getContentType() == null ? DEFAULT_MIME_TYPE : file.getContentType());
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new BusinessRuleException(
                    "El almacenamiento de archivos no esta configurado: faltan CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET.");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Debe adjuntar un archivo");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("El archivo excede 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("Tipo de archivo no permitido: " + contentType);
        }
        // Con octet-stream el navegador no declara el tipo real: se exige extension conocida para no
        // aceptar binarios arbitrarios.
        if (DEFAULT_MIME_TYPE.equalsIgnoreCase(contentType) && !hasAllowedExtension(file.getOriginalFilename())) {
            throw new BusinessRuleException("Extension de archivo no permitida");
        }
    }

    private boolean hasAllowedExtension(String originalName) {
        String name = safeOriginalName(originalName);

        if (name == null || !name.contains(".")) {
            return false;
        }

        return ALLOWED_EXTENSIONS.contains(name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT));
    }

    private boolean isImage(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private String resolveFolder(String folder) {
        String root = properties.rootFolder() == null || properties.rootFolder().isBlank()
                ? DEFAULT_ROOT_FOLDER
                : properties.rootFolder();

        return root + "/" + (folder == null || folder.isBlank() ? DEFAULT_FOLDER : folder);
    }

    /** Nombre original sin ruta y con extension. Cloudinary sanea el resto de caracteres. */
    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return null;
        }

        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();

        return name.isEmpty() ? null : name;
    }

    private String safeSuffix(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return ".tmp";
        }

        String extension = originalName.substring(originalName.lastIndexOf('.'));

        return extension.length() > 10 ? ".tmp" : extension;
    }

    private String lastSegment(String publicId) {
        return publicId.substring(publicId.lastIndexOf('/') + 1);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("No se pudo eliminar el archivo temporal {}", path, ex);
        }
    }
}
