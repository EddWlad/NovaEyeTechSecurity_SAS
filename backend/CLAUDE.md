# CLAUDE.md

Guía para trabajar en este repositorio. El código, los mensajes de error, los DTOs y la documentación están en **español**: mantener ese idioma en strings de usuario, excepciones y comentarios nuevos.

---

## Qué es

Backend de **Nova Eye Technology SAS / Next Eye Security**: catálogo comercial, motor de cotizaciones con PDF, mantenimientos con evidencias y bitácora de auditoría.

Es la migración del backend NestJS que vivía en `../NextEyeSecurity/apps/backend`. El contrato HTTP se conservó para que el frontend Angular de ese repo siga funcionando sin cambios: mismo prefijo `/api`, mismas rutas, `PATCH` para actualizaciones, `access_token` en el login y paginación de doble modo.

Stack: **Java 21 + Spring Boot 4.1 + Spring Security + JPA/Hibernate + PostgreSQL + Maven**.

> Spring Boot 4.1 usa **Jackson 3**: los imports son `tools.jackson.*`, no `com.fasterxml.jackson.databind.*`. Las anotaciones (`@JsonFormat`, `@JsonProperty`) sí siguen en `com.fasterxml.jackson.annotation`.

---

## Comandos

```bash
# Configuración local (una vez). .env está en .gitignore
cp .env.example .env

# Base de datos (Postgres 16 en el puerto 5435)
docker compose up -d

# Esquema: no lo genera Hibernate, viene de migrations/ (ver migrations/README.md)
docker exec -i novaeyetech-postgres psql -U postgres -c "CREATE DATABASE novaeyetech;"
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/000-esquema-base.sql
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/001-migracion-a-spring-boot.sql

# Compilar
./mvnw -DskipTests compile

# Ejecutar
./mvnw spring-boot:run

# Empaquetar
./mvnw -DskipTests package

# Tests
./mvnw test
```

`DotEnvEnvironmentPostProcessor` carga `.env` antes de que arranque el contexto, así que la aplicación funciona igual desde el IDE que desde Maven, sin run configuration especial. La fuente se registra con la **menor** prioridad: una variable de entorno real o un `-D` siempre ganan sobre el archivo.

Se registra en `META-INF/spring.factories` bajo la clave `org.springframework.boot.EnvironmentPostProcessor`. Ojo: en Spring Boot 4 la interfaz vive en `org.springframework.boot`, no en `org.springframework.boot.env` (esa está deprecada), y usar la clave equivocada hace que el post-processor **no se ejecute y no avise**.

La API queda en `http://localhost:8080/api`.

### Variables de entorno

| Variable | Por defecto | Notas |
|---|---|---|
| `JWT_SECRET` | — | **Obligatoria**, mínimo 32 caracteres. Sin ella la aplicación no arranca. |
| `JWT_EXPIRES_IN` | `8h` | Formato `Duration` de Spring. |
| `PORT` | `8080` | El frontend Angular apunta a este puerto en `environment.ts`. |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` / `DB_USERNAME` / `DB_PASSWORD` | `127.0.0.1` / `5435` / `novaeyetech` / `postgres` / `postgres` | |
| `DB_SYNC` | `validate` | El esquema viene de `migrations/`, no lo genera Hibernate. **Nunca `update`.** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Lista separada por comas. |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | — | Sin ellas la app arranca, pero toda subida responde 400. |
| `CLOUDINARY_ROOT_FOLDER` | `novaeyetech` | Carpeta raíz en Cloudinary. |
| `SEED_ENABLED` | `false` | Solo desarrollo. |

---

## Estructura

Arquitectura por capas, siguiendo la convención del backend modelo `mediapp-backend`:

```
com.tidsec.novaeyetech_backend
├── config/      SecurityConfig, MapperConfig, propiedades tipadas, DataSeeder
├── controller/  Un controlador por recurso. Solo orquesta: valida, delega, mapea a DTO
├── dto/         Request y respuesta. `common/` paginación y auditoría, `validation/` grupos
├── exception/   Excepciones de dominio y el @RestControllerAdvice
├── model/       Entidades JPA. `enums/` los enumerados del dominio
├── repo/        IGenericRepo + un repositorio por entidad
├── security/    JWT, filtro de autenticación, principal, respuestas 401/403
├── service/     Interfaces (ICRUD + una por dominio)
├── service/impl/ Implementaciones (CRUDImpl + una por dominio) y QuotationCalculator
└── util/        Paginación, dinero, mapeo, generación de PDF
```

### Abstracciones genéricas

- `Identifiable<ID>`: toda entidad expone `getId()`. Es el bound genérico de los repositorios y deja leer el identificador sin reflexión ni casts.
- `IGenericRepo<T extends Identifiable<ID>, ID>`: base de todos los repositorios.
- `ICRUD<T, ID>` / `CRUDImpl<T, ID>`: **solo lectura y borrado** (`findAll`, `findAll(Pageable)`, `findById`, `delete`). El alta y la actualización no están aquí: cada dominio las declara con su propio request DTO porque necesita resolver relaciones, normalizar campos y registrar auditoría.
- `DtoMapper`: fachada sobre ModelMapper. `map`, `mapList` y `patch` (vuelca sobre una instancia existente ignorando nulos).
- `ListingResponder`: resuelve la paginación de doble modo en un solo lugar.

**Antes de crear una abstracción nueva, revisar si alguna de estas cubre el caso.**

---

## Reglas que no se cambian sin pedido explícito

### Motor de cotizaciones (`QuotationCalculator`, `QuotationServiceImpl`)

1. **Orden de cálculo: primero IVA, después ganancia.**
   `unitPriceFinal = baseCost * (1 + iva/100) * (1 + margen/100)`
2. **Snapshot histórico.** Cada `QuotationDetail` congela descripción, precio base, % IVA, % margen, precio unitario y totales. Una cotización guardada **nunca** se recalcula aunque cambien los parámetros del sistema.
3. Los porcentajes válidos salen de `quotation_settings` (fila única). El margen `0` es válido a propósito (instalación a precio cerrado): **no reintroducir validaciones de "margen > 0"**.
4. El descuento se valida contra el total bruto y se resta al final. El `subtotal` almacenado es la suma de bases **sin** IVA.
5. Numeración `COT-<año>-<secuencia de 6 dígitos>`, derivada de `count()`.
6. Solo se edita una cotización en estado `BORRADOR`.
7. Alcance por rol: un `TECNICO` solo ve y edita sus propias cotizaciones. Un id ajeno responde **404, no 403**, para no revelar que el recurso existe. Lo mismo aplica a mantenimientos con su técnico asignado.

### Paginación de doble modo

`PaginationQuery.isPaginated()` es verdadero **solo si llega `?page`**.

- Sin `page`: array plano.
- Con `page`: `PageResponse<T>` (`page`, `limit`, `total`, `totalPages`, `hasNext`, `hasPrevious`, `items`).

El frontend depende de las dos formas: `ApiService.list()` alimenta selects y lookups, `ApiService.listPaginated()` alimenta tablas. Romper el modo array rompe los combos de los formularios.

`limit` se acota a 1..100. El default es 10, salvo auditoría que usa 20.

### Dinero

Todos los importes son `BigDecimal` con escala 2 y redondeo `HALF_UP`, normalizados por `MoneyUtils`. En los DTOs se serializan **como String** (`@JsonFormat(shape = STRING)`) para conservar el formato que el frontend ya recibía de las columnas `numeric`.

### Auditoría

No hay interceptor. Cada servicio llama explícitamente a `auditLogService.register(AuditEntry.builder()...)`. **Una operación mutante nueva debe registrar su log** o se pierde la trazabilidad.

### Esquema de base de datos

**Hibernate no genera el esquema.** La base existe desde el backend NestJS, con datos reales en producción, y este backend se adapta a ella:

- Columnas en **camelCase** (`"nameOrBusinessName"`, `"createdAt"`), no snake_case. Lo resuelve `CamelCaseNamingStrategy`, que además marca cada identificador como entrecomillado: sin eso Postgres pliega a minúsculas y busca `createdat`. La propiedad `globally_quoted_identifiers` **no sirve** aquí — la validación de esquema seguía fallando con ella.
- Los enums son **tipos nativos de Postgres** (`users_role_enum`). Cada campo enum lleva `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` y `columnDefinition` con el nombre del tipo. Sin eso el insert falla: `column "role" is of type users_role_enum but expression is of type character varying`.
- `DB_SYNC` es `validate`, también en local. Un campo nuevo exige su script en `migrations/`. Ver [`migrations/README.md`](migrations/README.md).

La base local se monta desde `migrations/` para que replique producción. No hay dos formas de esquema conviviendo.

### Archivos

Todo lo que sube un usuario (evidencias, adjuntos, foto de perfil) va a **Cloudinary** vía `IStorageService`. El servidor no escribe en disco: la aplicación es apta para un despliegue con sistema de archivos efímero o con varias instancias.

Un servicio nuevo que necesite guardar archivos inyecta `IStorageService` y no toca `java.nio.file`. Detalles del proveedor y sus trampas en [`.agents/features/file-storage-cloudinary.md`](.agents/features/file-storage-cloudinary.md).

---

## Seguridad

- Stateless: sin sesión, JWT en `Authorization: Bearer`.
- Regla base `anyRequest().authenticated()` **más** `@PreAuthorize` en cada handler. Es una diferencia deliberada con el backend NestJS, donde el `RolesGuard` dejaba pasar cualquier handler sin `@Roles`: allí olvidar el decorador abría el endpoint, aquí no.
- Único endpoint público: `POST /api/auth/login`.
- Solo dos roles: `ADMINISTRADOR` y `TECNICO`.
- El filtro JWT revalida el usuario contra la base en cada petición: un usuario desactivado pierde acceso aunque su token siga vigente.
- `JwtAuthenticationFilter` **no es un bean**: lo instancia `SecurityConfig`. Declararlo como bean haría que Boot lo registrara también como filtro del contenedor y se ejecutaría dos veces.
- `UserDTO` no declara `password`: el hash no puede salir por un descuido de saneado.
- Nunca hardcodear secretos.

---

## Convenciones

- Lombok e inyección por constructor (`@RequiredArgsConstructor`).
- `ResponseEntity` en los controladores; los controladores no contienen lógica de negocio.
- Validación Jakarta con grupos: `@Validated(OnCreate.class)` en POST, `@Valid` en PATCH. Un mismo request DTO sirve para crear y para actualizar parcialmente.
- ModelMapper en modo **STRICT** y con **skip nulls**: un origen nulo no pisa el destino, que es la semántica de PATCH.
- Un campo no declarado en el DTO responde **400** (`fail-on-unknown-properties`), equivalente a `forbidNonWhitelisted` de NestJS.
- La entidad de servicios se llama `ServiceItem` para no colisionar con `org.springframework.stereotype.Service`.

---

## Catálogo de agentes

Los agentes, workflows y features viven en `.agents/`:

**Subagentes** (`.agents/subagents/`)

| Agente | Cuándo |
|---|---|
| `spring-vertical-builder` | Crear o extender una vertical CRUD |
| `spring-api-reviewer` | Revisar cambios del backend |
| `quotation-rules-guardian` | Cualquier cambio que toque cotizaciones, precios, dinero o el PDF |
| `spring-test-data-generator` | Generar payloads de datos de prueba |

**Workflows** (`.agents/workflows/`)

| Workflow | Cuándo |
|---|---|
| `new-catalog-vertical` | Recurso CRUD nuevo |
| `review-backend-change` | Auditoría o revisión de diff |
| `verify-frontend-contract` | Antes de dar por buena cualquier alteración de una respuesta |
| `generate-test-data` | Poblar la base vía API |

**Features** (`.agents/features/`) — `security-jwt`, `pagination-dual-mode`, `quotation-engine`, `file-storage-cloudinary`, `legacy-schema`, `cors`. Leer la nota correspondiente antes de tocar esa área.

**Skills** (`.agents/skills/`) — `java-code-review`, que invocan el subagente `spring-api-reviewer` y el workflow de revisión.

---

## Reglas generales

- Nunca hardcodear secretos.
- No modificar infraestructura compartida sin pedido explícito.
- Seguir las convenciones existentes antes de introducir otras nuevas.
