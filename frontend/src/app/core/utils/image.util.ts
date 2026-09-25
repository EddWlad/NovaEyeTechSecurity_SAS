/**
 * Tamaño máximo de un archivo elegido por el usuario (imágenes y adjuntos). Coincide con el límite
 * del backend (`spring.servlet.multipart.max-file-size`) y con el de Cloudinary en el plan gratuito.
 */
export const MAX_UPLOAD_MB = 10;
export const MAX_UPLOAD_BYTES = MAX_UPLOAD_MB * 1024 * 1024;

/** Mensaje para un archivo que supera {@link MAX_UPLOAD_MB}. */
export const tooLargeMessage = (fileName?: string): string =>
  fileName
    ? `"${fileName}" supera ${MAX_UPLOAD_MB} MB.`
    : `El archivo no debe superar ${MAX_UPLOAD_MB} MB.`;

const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];

/**
 * Motivo por el que una imagen elegida no sirve, o `null` si es válida. Las pantallas la reducen en
 * el navegador antes de subirla, así que el servidor recibe mucho menos que esto.
 */
export const imageFileError = (file: File): string | null => {
  if (!IMAGE_TYPES.includes(file.type)) {
    return 'Formato inválido. Usa PNG, JPG o WEBP.';
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return tooLargeMessage();
  }
  return null;
};

/**
 * URL apta para el `src` de una imagen, o `null` si no hay imagen.
 *
 * Acepta URLs https del almacenamiento, data URLs de las imágenes heredadas y direcciones que
 * empiezan por `www.`.
 */
export const toImageSrc = (raw: unknown): string | null => {
  if (typeof raw !== 'string') {
    return null;
  }

  const url = raw.trim();
  if (!url) {
    return null;
  }

  return url.startsWith('www.') ? `https://${url}` : url;
};
