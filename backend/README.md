# NovaEyeTech Backend

Backend de **NOVAEYE TECHNOLOGY S.A.S** (antes Next Eye Security): catálogo comercial, motor de cotizaciones con generación de PDF, mantenimientos con evidencias y bitácora de auditoría.

Migración a Spring Boot del backend NestJS que vivía en `../NextEyeSecurity/apps/backend`. El contrato HTTP se conservó intacto, de modo que el frontend Angular de ese repositorio funciona contra este backend sin cambios.

**Java 21 · Spring Boot 4.1 · Spring Security · JPA/Hibernate · PostgreSQL 16 · Maven**

---

## Puesta en marcha

**1.** Configuración local. `.env` está en `.gitignore`, así que el secreto nunca se versiona:

```bash
cp .env.example .env
```

Editar `.env` y poner un `JWT_SECRET` propio de al menos 32 caracteres (`openssl rand -base64 48`). Para arrancar con datos de demostración, poner además `SEED_ENABLED=true`.

**2.** Base de datos:

```bash
docker compose up -d
```

**3.** Aplicación:

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`. Con la siembra activa, el usuario es `admin1@nexteye.com` / `Admin123*`.

El archivo `.env` lo carga `DotEnvEnvironmentPostProcessor` al arrancar, así que **funciona igual desde el IDE**, sin run configuration especial. Una variable de entorno real siempre gana sobre el archivo, de modo que el despliegue sigue mandando.

`JWT_SECRET` es obligatoria y no tiene valor por defecto: si falta, la aplicación no arranca en lugar de firmar tokens con una clave conocida. El resto de variables está documentado en `.env.example`.

### Otros comandos

```bash
./mvnw -DskipTests compile
```

```bash
./mvnw test
```

```bash
./mvnw -DskipTests package
```

El test de contexto completo necesita la base levantada y solo corre con `INTEGRATION_TESTS=true`.

---

## Endpoints

Todos cuelgan de `/api`. Salvo el login, todos exigen `Authorization: Bearer <token>`.

| Método | Ruta | Roles |
|---|---|---|
| `POST` | `/auth/login` | público |
| `GET` | `/` | público (health) |
| `GET/POST/PATCH/DELETE` | `/users`, `/users/{id}` | ADMINISTRADOR |
| `GET` | `/users/me` | ambos |
| `PATCH` | `/users/me/profile` | ambos |
| `GET/POST/PATCH/DELETE` | `/clients` | ambos |
| `GET` | `/suppliers`, `/products`, `/services`, `/product-categories`, `/service-categories` | ambos |
| `POST/PATCH/DELETE` | `/suppliers`, `/products`, `/services`, `/product-categories`, `/service-categories` | ADMINISTRADOR |
| `GET/POST/PATCH` | `/quotations` | ambos (el TÉCNICO solo las suyas) |
| `PATCH` | `/quotations/{id}/status` | ambos |
| `GET` | `/quotations/{id}/pdf` | ambos |
| `GET` | `/quotation-settings` | ambos |
| `PATCH` | `/quotation-settings` | ADMINISTRADOR |
| `GET/POST/PATCH/DELETE` | `/maintenance` | ambos (el TÉCNICO solo los suyos) |
| `POST` | `/maintenance-comments` | ambos |
| `GET` | `/maintenance-comments/maintenance/{id}` | ambos |
| `POST` | `/users/me/avatar` (multipart, campo `file`) | ambos |
| `POST` | `/attachments/upload/maintenance/{id}` (multipart, campo `file`) | ambos |
| `GET` | `/attachments/{id}/download`, `/attachments/{origen}/{id}` | ambos |
| `DELETE` | `/attachments/{id}` | ambos |
| `GET` | `/audit-logs` | ADMINISTRADOR |

### Paginación de doble modo

Sin `?page` los listados devuelven un **array plano**; con `?page` devuelven un objeto paginado:

```json
{ "page": 1, "limit": 10, "total": 42, "totalPages": 5, "hasNext": true, "hasPrevious": false, "items": [] }
```

`limit` se acota a 1..100.

### Errores

```json
{ "timestamp": "...", "statusCode": 400, "error": "Bad Request", "message": "...", "path": "/api/clients", "errors": ["campo: motivo"] }
```

Un campo no declarado en el DTO responde 400.

---

## Reglas de negocio

**Cotizaciones**

- Orden de cálculo fijo: primero IVA, después ganancia. `unitPriceFinal = baseCost * (1 + iva/100) * (1 + margen/100)`.
- Cada línea congela su descripción, precio base, porcentajes y totales. Una cotización guardada nunca se recalcula.
- Los porcentajes válidos salen de `quotation_settings`. El margen `0` es válido a propósito.
- El descuento se valida contra el total bruto; el `subtotal` almacenado es la suma de bases sin IVA.
- Numeración `COT-<año>-<6 dígitos>`. Solo se edita en estado `BORRADOR`.

**Alcance por rol**

Solo existen `ADMINISTRADOR` y `TECNICO`. Un técnico solo ve y edita sus propias cotizaciones y los mantenimientos donde figura como técnico asignado; un id ajeno responde 404, no 403.

---

## Archivos e imágenes

Evidencias, adjuntos y foto de perfil se guardan en **Cloudinary**. El servidor no escribe en disco, así que la aplicación funciona en un despliegue con sistema de archivos efímero o con varias instancias.

- Máximo **10 MB** por archivo.
- Tipos aceptados: JPEG, PNG, WebP, GIF, PDF, Word y Excel.
- La descarga pasa por el backend (`GET /api/attachments/{id}/download`) para mantener el endpoint autenticado. El DTO expone además `storagePath` con la URL, útil para mostrar una imagen directo en un `img`.
- Sin credenciales configuradas la aplicación arranca igual, pero toda subida responde 400 con un mensaje explícito.

Configuración en `.env`:

```
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_ROOT_FOLDER=novaeyetech
```

Detalles del proveedor en [`.agents/features/file-storage-cloudinary.md`](.agents/features/file-storage-cloudinary.md).

---

## Estructura

```
com.tidsec.novaeyetech_backend
├── config/       seguridad, mapeo, propiedades tipadas, siembra de datos
├── controller/   un controlador por recurso
├── dto/          request y respuesta, grupos de validación
├── exception/    excepciones de dominio y manejador global
├── model/        entidades JPA y enumerados
├── repo/         repositorios
├── security/     JWT y contexto de seguridad
├── service/      interfaces
├── service/impl/ implementaciones y motor de cálculo
└── util/         paginación, dinero, mapeo, PDF
```

Las convenciones de trabajo y las reglas que no se cambian sin pedido explícito están en [CLAUDE.md](CLAUDE.md). Los agentes y workflows del repositorio viven en `.agents/`.
