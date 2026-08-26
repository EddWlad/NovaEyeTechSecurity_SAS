package com.tidsec.novaeyetech_backend.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Estrategia de nombres fisicos del proyecto.
 *
 * <p>La base de datos en produccion la creo el backend NestJS con TypeORM, que nombro las columnas
 * en camelCase y entrecomilladas: {@code "nameOrBusinessName"}, {@code "createdAt"}. Este backend
 * consume esa misma base, con sus datos, asi que se adapta al esquema existente en lugar de exigir
 * un renombrado masivo de columnas sobre datos reales.
 *
 * <p>Dos reglas, y las dos hacen falta:
 *
 * <ul>
 *   <li><b>Nombre literal.</b> Se usa el nombre del atributo tal cual. La estrategia por defecto de
 *       Spring Boot lo convierte a snake_case y buscaria {@code name_or_business_name}, que no
 *       existe en ninguna tabla.</li>
 *   <li><b>Entrecomillado.</b> Postgres pliega a minusculas todo identificador sin comillas, de modo
 *       que {@code createdAt} se buscaria como {@code createdat}. Marcarlo aqui, y no con la
 *       propiedad {@code globally_quoted_identifiers}, es lo que hace que la validacion de esquema
 *       compare el nombre correcto: con esa propiedad la validacion seguia fallando.</li>
 * </ul>
 *
 * <p>El entrecomillado resuelve ademas {@code audit_logs."user"}, que de otro modo chocaria con la
 * palabra reservada de SQL.
 *
 * <p>Los nombres declarados de forma explicita en las entidades ({@code @JoinColumn(name = ...)},
 * las claves foraneas en snake_case) llegan aqui ya resueltos y solo se entrecomillan.
 */
public class CamelCaseNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return quote(name);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return quote(name);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        return quote(name);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return quote(name);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        return quote(name);
    }

    private Identifier quote(Identifier name) {
        return name == null || name.isQuoted() ? name : Identifier.toIdentifier(name.getText(), true);
    }
}
