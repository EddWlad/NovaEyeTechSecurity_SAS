# NovaEyeTech Frontend

Portal administrativo de **NOVAEYE TECHNOLOGY S.A.S** (antes Next Eye Security): catálogo comercial, cotizaciones con vista previa de PDF, mantenimientos con evidencias y bitácora de auditoría.

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
npm test          # Karma en modo watch, con Chrome
npm run test:ci   # una pasada en Chrome sin ventana (lo que corre el workflow)
```

```bash
npm run lint
```

```bash
npm run check:icons
```

`lint` usa angular-eslint (`eslint.config.js`): reglas recomendadas de TypeScript y Angular más las de accesibilidad en plantillas. `check:icons` verifica que todo icono usado en las plantillas esté en la lista `icon_names` de `src/index.html` (ver "Iconos" abajo). Los tres corren en el workflow de despliegue antes del build: si uno falla, no se despliega.

Las pruebas cubren las piezas compartidas: interceptor `dedupe-get`, utilidades de `core/utils`, `SearchSelectComponent` y la tabla de rutas y roles. Los schematics tienen `skipTests: true`, así que un componente nuevo no trae `.spec.ts`: si tiene lógica que valga la pena fijar, escribirlo a mano.

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

Para agregar un CRUD simple: definir el recurso ahí, añadir **una línea** `...crudRoutes('x', roles)` en `app.routes.ts` (genera `/x`, `/x/new`, `/x/:id` y `/x/:id/edit`) y la entrada en `core/config/navigation.config.ts`. Si el detalle debe mostrar el nombre del registro en las migas de pan, sumar una entrada en `core/config/breadcrumb.config.ts`. **No crear componentes por entidad** salvo que la pantalla se salga del patrón.

Pantallas propias, fuera del motor CRUD, solo para: `quotations`, `maintenance`, `quotation-settings`, `audit-logs`, `dashboard`, `profile` y `auth`.

### Capa HTTP

`core/services/api.service.ts` es la única puerta HTTP; los servicios de feature lo envuelven, nunca `HttpClient` directo.

El backend responde de dos formas según la query string, y las dos se usan:

- `ApiService.list()` — sin `?page`, devuelve un array plano. Alimenta selects y lookups.
- `ApiService.listPaginated()` — con `?page`, devuelve un objeto paginado. Alimenta tablas.

La búsqueda de las tablas la resuelve el servidor sobre todos los registros (`?search=`, y en cotizaciones y mantenimientos también `?status=` y `?type=`), no solo sobre la página visible. Las pantallas lo conectan con `onSearchChange` (`core/utils/search.util.ts`): una petición cuando el usuario deja de escribir, no una por tecla.

Los selects con cientos de opciones (productos y servicios en el detalle de una cotización) usan `SearchSelectComponent` (`shared/components/`): un combobox accesible que filtra por palabras sin distinguir tildes, se usa con `formControlName` y trabaja sobre la lista que ya trae el modo array.

Interceptores en `core/interceptors/`, en este orden: `dedupe-get` unifica en una sola petición los GET idénticos que están en vuelo a la vez (por ejemplo la pantalla y la miga de pan pidiendo el mismo registro; no guarda nada en caché), `auth-token` inyecta el JWT y `http-error` centraliza los errores hacia `NotificationService`.

### Rutas y permisos

Todo cuelga de `AppShellComponent` con `authGuard`, y cada ruta lleva `roleGuard` más `data: { roles, resourceKey }`. Solo existen los roles `ADMINISTRADOR` y `TECNICO`.

Solo el login y el shell se cargan de entrada; el resto de pantallas son **lazy** (`loadComponent`) y el router las precarga en segundo plano (`PreloadAllModules`). Tras un despliegue, los archivos con hash de la versión anterior desaparecen: si un usuario con la app abierta navega a una pantalla aún no descargada, `app.config.ts` recarga la página una vez para tomar la versión nueva.

### Estilos

SCSS global con tokens en `src/styles.scss` (paleta de marca verde `--brand-700/600` (`#374015`), con `--brand-rgb` para los tintes translúcidos, más grises, sombras y utilitarias como `.page-title`). Componentes compartidos en `app/shared/components/`: `page-header`, `pagination-controls`, `status-badge`, `empty-state`, `loading-spinner`, `toast-stack`. Reutilizar tokens y componentes antes de crear estilos nuevos.

**Tablas responsivas.** Una lista con tabla que en pantallas chicas se vuelve una lista de tarjetas usa `<div class="table-wrapper table-stack">` con `data-label` en cada `<td>` y `data-label="Acciones"` en la celda de botones. Toda la lógica vive en `styles.scss` (breakpoint único de 768 px): botones de acción en una sola fila, columna de acciones anclada a la derecha en tablas anchas y diseño de tarjetas en móvil. No repetirla en los componentes.

**Scrollbars.** Finos y discretos en toda la app (`scrollbar-width: thin`). El menú lateral, oscuro, lo oculta en reposo y lo muestra translúcido al pasar el mouse.

**Accesibilidad.** Un botón o enlace que solo tiene un icono lleva `title` y `aria-label`; los iconos decorativos llevan `aria-hidden="true"`.

### Iconos

La fuente Material Symbols se pide con solo los iconos que usa la app (`icon_names` en `src/index.html`): ~6 KB en vez de los 316 KB de la fuente completa. **Al usar un icono nuevo hay que agregarlo a esa lista, en orden alfabético**; si no, se ve como texto. `npm run check:icons` lo detecta (iconos en plantillas, `{{ cond ? 'a' : 'b' }}` y `icon: 'x'` en configuración). Un icono que solo se devuelve desde código (como los del toast) no lo ve el chequeo: agregarlo a mano.

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
