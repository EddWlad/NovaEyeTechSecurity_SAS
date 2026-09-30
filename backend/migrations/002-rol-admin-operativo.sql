-- Rol ADMIN_OPERATIVO: administrador del dia a dia, sin acceso a usuarios, parametros de
-- cotizacion ni auditoria (esos quedan para el super administrador, rol ADMINISTRADOR).
--
-- Solo agrega un valor al tipo enumerado de los roles: no modifica ninguna fila ni cambia el rol
-- de ningun usuario existente. Es idempotente (IF NOT EXISTS, Postgres 12+).
--
-- Debe aplicarse antes de desplegar el backend que conoce el rol; hasta entonces, crear un usuario
-- con ese rol fallaria. Los usuarios actuales no se ven afectados en ningun orden.
--
-- Ejecucion en el servidor:
--   docker exec -i novaeyetech-postgres psql -U postgres -d nexteye_security < 002-rol-admin-operativo.sql

BEGIN;

ALTER TYPE public.users_role_enum ADD VALUE IF NOT EXISTS 'ADMIN_OPERATIVO';

COMMIT;
