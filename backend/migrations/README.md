# Migraciones

El esquema de la base **no lo genera Hibernate**: existe desde el backend NestJS anterior y tiene datos reales en producción. Aquí viven los scripts que lo llevan de un estado al siguiente.

| Script | Qué hace |
|---|---|
| `000-esquema-base.sql` | Volcado del esquema en producción, tal como lo dejó TypeORM. Punto de partida para montar una base local idéntica. No se ejecuta en producción, que ya lo tiene aplicado. |
| `001-migracion-a-spring-boot.sql` | Añade lo que el backend Spring Boot necesita y no existía. Aditivo e idempotente. |
| `002-rol-admin-operativo.sql` | Agrega el rol `ADMIN_OPERATIVO` (administrador sin usuarios, parámetros ni auditoría). No toca filas. |

## Montar la base local

```bash
docker compose up -d
docker exec -i novaeyetech-postgres psql -U postgres -c "CREATE DATABASE novaeyetech;"
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/000-esquema-base.sql
docker exec -i novaeyetech-postgres psql -U postgres -d novaeyetech < migrations/001-migracion-a-spring-boot.sql
```

Con `SEED_ENABLED=true` el arranque siembra datos de demostración sobre ese esquema.

## Por qué la base local replica producción

Durante la migración hubo dos formas de esquema conviviendo: Hibernate generaba `snake_case` en local mientras producción tenía `camelCase`. Eso significa probar contra algo que no es lo que se despliega, y garantiza que un fallo aparezca solo en producción.

Ahora hay una sola forma. `CamelCaseNamingStrategy` adapta Hibernate al esquema real, y la base local se monta desde estos scripts.

## Añadir una migración

1. Numerar en orden: `002-...`, `003-...`.
2. Que sea **aditiva**. Sobre una base con datos reales no se renombra ni se borra sin un plan aparte.
3. Que sea **idempotente**: `IF NOT EXISTS`, `IF EXISTS`. Volver a ejecutarla no debe hacer daño.
4. Envolver en `BEGIN; ... COMMIT;`.
5. Probar primero contra una copia local antes de tocar producción.

## Ejecutar en producción

```bash
docker exec -i nexteye-postgres psql -U postgres -d nexteye_security < 00X-....sql
```

Antes, respaldo:

```bash
docker exec nexteye-postgres pg_dump -U postgres -d nexteye_security > respaldo-$(date +%F).sql
```

En producción `DB_SYNC` debe ser `validate`. Nunca `update`: Hibernate crearía columnas `snake_case` duplicadas junto a las `camelCase` existentes y partiría los datos en dos.
