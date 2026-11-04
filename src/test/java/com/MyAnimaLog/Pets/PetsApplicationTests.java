package com.MyAnimaLog.Pets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"app.security.internal-api-key=clave-interna-de-prueba",
		"app.security.jwks-uri=http://localhost:8080/.well-known/jwks.json"
})
class PetsApplicationTests {

	@Test
	void contextLoads() {
	}

}
