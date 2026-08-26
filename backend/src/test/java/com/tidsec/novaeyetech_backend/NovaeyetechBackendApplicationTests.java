package com.tidsec.novaeyetech_backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que todo el contexto de Spring levante: beans, seguridad y mapeo JPA.
 *
 * <p>Necesita PostgreSQL en marcha ({@code docker compose up -d}), asi que solo corre cuando la
 * variable {@code INTEGRATION_TESTS} esta presente. De lo contrario {@code ./mvnw test} fallaria en
 * cualquier maquina sin la base levantada.
 */
@SpringBootTest(properties = "app.jwt.secret=test-secret-solo-para-pruebas-32-chars")
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class NovaeyetechBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
