package com.tidsec.novaeyetech_backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraccion del almacenamiento de archivos e imagenes.
 *
 * <p>La implementacion actual es Cloudinary. Al ser interfaz, cambiar de proveedor (S3, Azure Blob)
 * no obliga a tocar la capa de negocio: `AttachmentServiceImpl` y `UserServiceImpl` solo conocen
 * este contrato.
 */
public interface IStorageService {

    /**
     * Sube un archivo y devuelve sus datos de almacenamiento.
     *
     * @param file   archivo recibido por multipart
     * @param folder carpeta logica dentro del almacenamiento (por ejemplo "maintenance", "avatars")
     */
    StoredFile upload(MultipartFile file, String folder);

    /**
     * Descarga el contenido de un recurso ya almacenado.
     *
     * <p>El backend hace de intermediario en vez de redirigir al cliente: mantiene el endpoint de
     * descarga autenticado y conserva el contrato que ya consume el frontend.
     */
    byte[] download(String url);

    /** Elimina un recurso. Devuelve false si el proveedor no lo encontro o no pudo borrarlo. */
    boolean delete(String publicId, String resourceType);

    /**
     * Elimina un recurso a partir de su URL, deduciendo identificador y tipo.
     *
     * <p>Para los casos donde la entidad solo guarda la URL, como el avatar del usuario. Ignora
     * cualquier valor que no sea un recurso propio: un data URL heredado, una ruta local o una URL de
     * otra cuenta o carpeta del proveedor.
     */
    boolean deleteByUrl(String url);

    /**
     * Datos que devuelve el proveedor tras una subida.
     *
     * @param url          URL segura y publica del recurso
     * @param publicId     identificador dentro del proveedor, necesario para poder borrarlo
     * @param resourceType "image" para imagenes, "raw" para documentos
     * @param storedName   nombre final del recurso en el proveedor
     * @param size         tamano en bytes
     * @param mimeType     tipo de contenido detectado
     */
    record StoredFile(
            String url,
            String publicId,
            String resourceType,
            String storedName,
            long size,
            String mimeType
    ) {
    }
}
