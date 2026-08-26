# NovaEyeTech Security

Sistema administrativo de **Nova Eye Technology SAS / Next Eye Security**: catálogo comercial, motor de cotizaciones con generación de PDF, mantenimientos con evidencias y bitácora de auditoría.

```
NovaEyeTechSecurity_SAS/
├── backend/     API REST — Java 21 · Spring Boot 4.1 · PostgreSQL
└── frontend/    Portal administrativo — Angular 20
```

Cada carpeta tiene su propio README con el detalle de instalación, arquitectura y convenciones.

---

## Arranque rápido

**1. Base de datos y backend**

```bash
cd backend
cp .env.example .env
```

Editar `.env`: poner un `JWT_SECRET` propio de al menos 32 caracteres y, si vas a subir archivos, las credenciales de Cloudinary. Después levanta la base y aplica el esquema (ver `backend/migrations/README.md`):

```bash
docker compose up -d
docker exec -i novaeyetech-postgres psql -U postgres -c "CREATE DATABASE novaeyetech;"
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/000-esquema-base.sql
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/001-migracion-a-spring-boot.sql
./mvnw spring-boot:run
```

API en `http://localhost:8080/api`.

El esquema no lo genera Hibernate: viene de `backend/migrations/` y replica el de producción, que creó el backend NestJS anterior. Un campo nuevo en una entidad exige su script de migración.

**2. Frontend**

```bash
cd frontend
npm install
npm start
```

Portal en `http://localhost:4200`. Espera el backend en `http://localhost:8080/api`, origen que el backend ya permite por defecto.

Para arrancar con datos de demostración, poner `SEED_ENABLED=true` en `backend/.env`. El usuario sembrado es `admin1@nexteye.com` / `Admin123*`.

---

## Módulos

| Módulo | Qué hace |
|---|---|
| Clientes y proveedores | Datos comerciales y de contacto |
| Catálogo | Productos y servicios con sus categorías |
| Cotizaciones | Cálculo con IVA y margen, numeración propia, PDF imprimible |
| Mantenimientos | Preventivos y correctivos con comentarios técnicos y evidencias |
| Usuarios | Dos roles: `ADMINISTRADOR` y `TECNICO` |
| Auditoría | Bitácora de toda operación que modifica datos |

---

## Reglas de negocio que no se cambian sin pedido explícito

**Cotizaciones**

- Orden de cálculo fijo: primero IVA, después ganancia.
  `unitPriceFinal = baseCost * (1 + iva/100) * (1 + margen/100)`
- Cada línea congela descripción, precio base, porcentajes y totales. Una cotización guardada nunca se recalcula, aunque cambien los parámetros del sistema.
- El margen `0` es válido a propósito: cubre servicios con precio ya cerrado, como la instalación.
- Numeración `COT-<año>-<6 dígitos>`. Solo se edita en estado `BORRADOR`.

**Alcance por rol**

Un técnico solo ve y edita sus propias cotizaciones y los mantenimientos donde figura como técnico asignado. Un identificador ajeno responde 404, no 403, para no revelar que el recurso existe.

El detalle completo está en [`backend/CLAUDE.md`](backend/CLAUDE.md).

---

## Seguridad

- API stateless con JWT en la cabecera `Authorization`.
- Regla base: toda petición exige autenticación, y además cada endpoint declara los roles que admite.
- Único endpoint público: `POST /api/auth/login`.
- **Ningún secreto se versiona.** `JWT_SECRET` y las credenciales de Cloudinary viven en `backend/.env`, que está ignorado por git. Las plantillas con valores de ejemplo son los `.env.example`.

---

## Historia

Migración del sistema anterior, cuyo backend estaba en NestJS. El contrato HTTP se conservó intacto para que el frontend siguiera funcionando sin cambios durante la transición: mismo prefijo `/api`, mismas rutas, `PATCH` para actualizaciones y paginación de doble modo.
