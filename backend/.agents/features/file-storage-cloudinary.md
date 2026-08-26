# Almacenamiento de archivos (Cloudinary)

## Resumen

Las evidencias de mantenimiento, los adjuntos y la foto de perfil se guardan en **Cloudinary**. El servidor no escribe nada en disco: en la base solo queda el metadato y la URL.

| Archivo | Rol |
|---|---|
| `service/IStorageService.java` | Contrato del almacenamiento |
| `service/impl/CloudinaryStorageServiceImpl.java` | Implementación: subida, descarga, borrado, validación |
| `config/CloudinaryConfig.java` | Bean del cliente |
| `config/CloudinaryProperties.java` | Credenciales (`app.cloudinary.*`) |

## Por qué una interfaz

`AttachmentServiceImpl` y `UserServiceImpl` solo conocen `IStorageService`. Cambiar de proveedor (S3, Azure Blob) no obliga a tocar la capa de negocio.

## Decisiones

**El disco desapareció.** El backend NestJS escribía en `uploads/<origen>/`. Eso ata la aplicación a un sistema de archivos persistente y rompe cualquier despliegue con contenedor efímero o más de una instancia. Con Cloudinary el estado vive fuera del proceso.

**`image` frente a `raw`.** Las imágenes se suben como `resource_type = image`; todo lo demás como `raw`. Cloudinary responde 401 al entregar un PDF subido como imagen; como `raw` se descarga sin restricción.

**`filename_override`.** En `raw` Cloudinary no agrega la extensión por su cuenta, así que se envía el nombre original con extensión. Sin ella el archivo baja como "formato desconocido". En imágenes la extensión se ignora porque usa el `format`.

**La descarga pasa por el backend.** `GET /api/attachments/{id}/download` descarga de Cloudinary y devuelve los bytes en vez de redirigir. Mantiene el endpoint autenticado y conserva el contrato que ya consume el frontend, que espera un blob. El DTO expone además `storagePath` con la URL, para que una imagen pueda mostrarse directo en un `img` sin pasar por el backend.

**El archivo se valida antes que la configuración.** Un tipo no permitido es culpa del cliente y el mensaje debe decir eso, no que al servidor le falten credenciales.

**El borrado remoto no bloquea el local.** Al eliminar un adjunto se borra primero en Cloudinary y después en la base. Si el remoto falla, el registro se borra igual: un archivo huérfano en el proveedor es menos dañino que una fila que apunta a algo que el usuario cree eliminado.

**Sin credenciales la aplicación arranca.** El bean se crea siempre; quien valida es el servicio, y cualquier subida responde 400 con un mensaje explícito. Así un entorno a medio configurar no impide levantar el resto de la API.

## Límites

- Tamaño máximo: **10 MB** por archivo (`MAX_BYTES`), alineado con `spring.servlet.multipart.max-file-size`.
- Tipos permitidos: JPEG, PNG, WebP, GIF, PDF, Word y Excel.
- `application/octet-stream` se acepta solo si la extensión está en la lista blanca: algunos navegadores lo envían para `.xlsx` y `.docx`, pero sin esa comprobación se aceptaría cualquier binario.

## Estructura en Cloudinary

```
<CLOUDINARY_ROOT_FOLDER>/       (por defecto "novaeyetech")
├── maintenance/                evidencias de mantenimiento
├── avatars/                    fotos de perfil
└── general/                    subidas sin carpeta indicada
```

## Configuración

```
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_ROOT_FOLDER=novaeyetech
```

Van en `.env` (ignorado por git) o como variables de entorno del despliegue. Nunca en `.env.example` ni en el código.

## Foto de perfil

`POST /api/users/me/avatar` (multipart, campo `file`) sube la imagen y guarda su URL. El campo de la entidad sigue llamándose `avatarDataUrl` por compatibilidad: el frontend lo pone directo en el `src` de una imagen, y ahí un data URL heredado y una URL https funcionan igual. Al reemplazar la foto se borra la anterior, pero solo **después** de guardar la nueva: si la subida falla, el usuario conserva la que tenía.
