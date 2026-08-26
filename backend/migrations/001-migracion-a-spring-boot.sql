-- Migración del esquema creado por TypeORM al que espera el backend Spring Boot.
--
-- Es puramente aditiva: solo agrega columnas nuevas. No renombra, no borra y no altera
-- ningún dato existente. Se puede ejecutar sobre la base en producción sin detener el
-- backend NestJS, que seguirá funcionando porque ignora las columnas nuevas.
--
-- Es idempotente: IF NOT EXISTS permite repetir la ejecución sin efecto.
--
-- Ejecución en el servidor:
--   docker exec -i nexteye-postgres psql -U postgres -d nexteye_security < 001-migracion-a-spring-boot.sql

BEGIN;

-- ---------------------------------------------------------------------------
-- quotation_details.lineNumber
--
-- Orden estable de las líneas dentro de una cotización. TypeORM devolvía las filas
-- en el orden que quisiera el motor; el backend nuevo las ordena explícitamente.
-- Las cotizaciones ya emitidas se numeran por su orden de inserción, que es el
-- único criterio disponible a posteriori.
-- ---------------------------------------------------------------------------
ALTER TABLE public.quotation_details
    ADD COLUMN IF NOT EXISTS "lineNumber" integer;

UPDATE public.quotation_details d
SET "lineNumber" = numerada.orden
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY quotation_id ORDER BY ctid) AS orden
    FROM public.quotation_details
) AS numerada
WHERE d.id = numerada.id
  AND d."lineNumber" IS NULL;

ALTER TABLE public.quotation_details
    ALTER COLUMN "lineNumber" SET NOT NULL;

-- ---------------------------------------------------------------------------
-- attachments.publicId y attachments.resourceType
--
-- Identificador y tipo de recurso en Cloudinary. Quedan nulos en los adjuntos
-- anteriores, que apuntan a rutas de disco (/uploads/...): esos archivos ya no
-- existen y el backend responde 404 al intentar descargarlos.
-- ---------------------------------------------------------------------------
ALTER TABLE public.attachments
    ADD COLUMN IF NOT EXISTS "publicId" character varying(255);

ALTER TABLE public.attachments
    ADD COLUMN IF NOT EXISTS "resourceType" character varying(20);

-- ---------------------------------------------------------------------------
-- attachments.storagePath
--
-- Pasa de guardar una ruta relativa a guardar la URL completa de Cloudinary,
-- que no cabe en 255 caracteres.
-- ---------------------------------------------------------------------------
ALTER TABLE public.attachments
    ALTER COLUMN "storagePath" TYPE character varying(500);

COMMIT;

-- Comprobación posterior:
--   SELECT count(*) FROM quotation_details WHERE "lineNumber" IS NULL;  -- debe dar 0
