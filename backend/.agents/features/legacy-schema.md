# Esquema heredado de TypeORM

## Resumen

La base de datos **no la genera Hibernate**. Existe desde el backend NestJS, corre en producción con datos reales (682 productos, 62 cotizaciones, 1172 registros de auditoría) y este backend se adapta a ella.

| Pieza | Rol |
|---|---|
| `config/CamelCaseNamingStrategy.java` | Traduce nombres de atributo a los identificadores reales |
| `migrations/000-esquema-base.sql` | Volcado del esquema de producción |
| `migrations/001-migracion-a-spring-boot.sql` | Lo que este backend necesitaba y no existía |

## Las tres diferencias, y cómo se resolvieron

**1. Columnas en camelCase.** TypeORM creó `"nameOrBusinessName"`, `"createdAt"`. La estrategia por defecto de Spring Boot busca `name_or_business_name` y no encuentra nada.

`CamelCaseNamingStrategy` usa el nombre del atributo tal cual **y lo marca como entrecomillado**. Las dos cosas hacen falta: Postgres pliega a minúsculas todo identificador sin comillas, así que `createdAt` se buscaría como `createdat`.

> La propiedad `hibernate.globally_quoted_identifiers` **no resuelve esto**. Con ella activa la validación de esquema seguía reportando `missing column [createdAt] in table [attachments]` aunque la columna existiera. Marcar el identificador en la estrategia sí funciona.

**2. Enums nativos de Postgres.** `users_role_enum`, `quotations_status_enum`, `quotation_details_itemtype_enum`, `maintenance_type_enum`, `maintenance_status_enum`.

Con solo `@Enumerated(EnumType.STRING)` el insert falla:

```
ERROR: column "role" is of type users_role_enum but expression is of type character varying
```

Cada campo enum lleva:

```java
@Enumerated(EnumType.STRING)
@JdbcTypeCode(SqlTypes.NAMED_ENUM)
@Column(nullable = false, columnDefinition = "users_role_enum")
private Role role;
```

Un enum nuevo necesita su tipo creado por migración y su `columnDefinition`.

**3. Columnas que no existían.** `quotation_details."lineNumber"`, `attachments."publicId"` y `attachments."resourceType"` las introdujo este backend. Las añade `001-migracion-a-spring-boot.sql`, que también amplía `storagePath` a 500 caracteres para las URL de Cloudinary.

La migración es **aditiva e idempotente**: el backend NestJS sigue funcionando con ella aplicada, porque ignora las columnas que no conoce. Eso permite aplicarla sin ventana de mantenimiento.

## `audit_logs."user"`

La columna se llama `user`, palabra reservada en SQL. Funciona porque la estrategia entrecomilla todos los identificadores, igual que hacía TypeORM. No hace falta renombrarla.

## Reglas al tocar el modelo

- **`DB_SYNC` es `validate`, también en local.** Con `update` Hibernate crearía columnas snake_case duplicadas junto a las camelCase y partiría los datos en dos.
- Un campo nuevo en una entidad **exige su script de migración**. Sin él la aplicación no arranca, que es el comportamiento deseado: avisa en el momento y no en producción.
- La base local se monta desde `migrations/`. No hay dos formas de esquema conviviendo: se prueba contra lo mismo que se despliega.
- Al probar, cuidado con `.env`: define `DB_DATABASE` y gana sobre el valor por defecto de cualquier perfil. Un rato de diagnóstico se fue en validar contra la base equivocada.
