# NovaEyeTech Frontend

Portal administrativo de **Nova Eye Technology SAS / Next Eye Security**: catálogo comercial, cotizaciones con vista previa de PDF, mantenimientos con evidencias y bitácora de auditoría.

Consume el backend Spring Boot de [`../novaeyetech-backend`](../novaeyetech-backend).

**Angular 20 (standalone, sin librería de UI) · TypeScript 5.9**

---

## Puesta en marcha

```bash
npm install
```

```bash
npm start
```

La aplicación queda en `http://localhost:4200` y espera el backend en `http://localhost:8080/api`.

El backend permite ese origen por defecto (`CORS_ALLOWED_ORIGINS`), así que no hace falta configurar nada más para desarrollo local.

### Otros comandos

```bash
npm run build
```

```bash
npm test
```

No hay archivos `.spec.ts` en el repositorio y los schematics tienen `skipTests: true`: el comando corre, pero no existe suite. No asumir cobertura previa.

---

## Configuración

No usa `.env`. Los entornos viven en `src/environments/`:

| Archivo | `apiBaseUrl` | Uso |
|---|---|---|
| `environment.ts` | `http://localhost:8080/api` | Desarrollo |
| `environment.prod.ts` | `/api` | Producción, detrás de un reverse proxy |

---

## Arquitectura

### CRUD genérico dirigido por configuración

`core/config/resource-definitions.ts` describe cada recurso (endpoint, roles, campos con su tipo, validación, opciones o lookup remoto). `features/crud/` provee `ResourceListPageComponent`, `ResourceFormPageComponent` y `ResourceDetailPageComponent` para todos ellos.

Para agregar un CRUD simple: definir el recurso ahí, añadir las cuatro rutas (`/x`, `/x/new`, `/x/:id`, `/x/:id/edit`) y la entrada en `core/config/navigation.config.ts`. **No crear componentes por entidad** salvo que la pantalla se salga del patrón.

Pantallas propias, fuera del motor CRUD, solo para: `quotations`, `maintenance`, `quotation-settings`, `audit-logs`, `dashboard`, `profile` y `auth`.

### Capa HTTP

`core/services/api.service.ts` es la única puerta HTTP; los servicios de feature lo envuelven, nunca `HttpClient` directo.

El backend responde de dos formas según la query string, y las dos se usan:

- `ApiService.list()` — sin `?page`, devuelve un array plano. Alimenta selects y lookups.
- `ApiService.listPaginated()` — con `?page`, devuelve un objeto paginado. Alimenta tablas.

Interceptores en `core/interceptors/`: `auth-token` inyecta el JWT, `http-error` centraliza los errores hacia `NotificationService`.

### Rutas y permisos

Todo cuelga de `AppShellComponent` con `authGuard`, y cada ruta lleva `roleGuard` más `data: { roles, resourceKey }`. Solo existen los roles `ADMINISTRADOR` y `TECNICO`.

### Estilos

SCSS global con tokens en `src/styles.scss` (paleta vino `--wine-700/600` más grises, sombras y utilitarias como `.page-title`). Componentes compartidos en `app/shared/components/`: `page-header`, `pagination-controls`, `status-badge`, `empty-state`, `loading-spinner`, `toast-stack`. Reutilizar tokens y componentes antes de crear estilos nuevos.

---

## Archivos e imágenes

Las evidencias y adjuntos se guardan en Cloudinary desde el backend. En las respuestas, `storagePath` es la URL pública del recurso:

- Si es una imagen, se muestra como miniatura directa desde esa URL.
- Si no, se ofrece un enlace para abrirla.
- La descarga sigue pasando por el backend (`GET /api/attachments/{id}/download`) para mantener el endpoint autenticado.

Los registros anteriores a la migración a Cloudinary guardan una ruta de disco que ya no existe; para esos la interfaz muestra "No disponible" y la descarga responde 404.

---

## Origen

Copiado desde `NextEyeSecurity/apps/frontend`, que sigue siendo la referencia histórica con el backend NestJS original. Este repositorio es el que se mantiene de aquí en adelante.
