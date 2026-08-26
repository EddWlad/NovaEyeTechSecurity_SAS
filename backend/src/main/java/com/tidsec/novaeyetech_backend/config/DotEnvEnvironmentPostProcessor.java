package com.tidsec.novaeyetech_backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Carga el archivo {@code .env} del directorio de trabajo antes de que arranque el contexto.
 *
 * <p>Spring no lee {@code .env} por su cuenta: sin esto, arrancar desde el IDE deja
 * {@code ${JWT_SECRET}} sin resolver y la aplicacion falla. Con esto, `.env` (que esta en
 * `.gitignore`) cumple el mismo papel que en el backend NestJS original.
 *
 * <p>La fuente se registra con la <b>menor</b> prioridad: una variable de entorno real, un
 * {@code -D} de la linea de comandos o un valor del run configuration siempre ganan sobre el
 * archivo. Asi el despliegue sigue mandando y `.env` solo cubre el desarrollo local.
 *
 * <p>Se registra en {@code META-INF/spring.factories}, que es el mecanismo que Spring Boot 4 sigue
 * usando para los {@link EnvironmentPostProcessor}: corren antes del contexto, asi que no pueden
 * descubrirse por escaneo de componentes.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final String DEFAULT_FILE = ".env";
    /** Permite apuntar a otro archivo: {@code -Ddotenv.filename=.env.local}. */
    private static final String FILENAME_PROPERTY = "dotenv.filename";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String filename = System.getProperty(FILENAME_PROPERTY, DEFAULT_FILE);
        Path file = Paths.get(filename).toAbsolutePath();

        if (!Files.isRegularFile(file)) {
            return;
        }

        Map<String, Object> values = parse(file);

        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // Un .env ilegible no debe impedir el arranque: el despliegue usa variables de entorno.
            System.err.println("No se pudo leer " + file + ": " + ex.getMessage());
            return values;
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = unquote(line.substring(separator + 1).trim());

            values.put(key, value);
            // Las variables se declaran en mayusculas con guion bajo; el binding relajado de Spring
            // ya traduce JWT_SECRET a app.jwt.secret cuando el placeholder lo referencia por nombre.
        }

        return values;
    }

    private String unquote(String value) {
        boolean quoted = value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")));

        return quoted ? value.substring(1, value.length() - 1) : value;
    }
}
