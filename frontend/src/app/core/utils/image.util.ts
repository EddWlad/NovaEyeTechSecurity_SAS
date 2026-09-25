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
