package com.example.DrogueriaLosTresHermanos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.datos=target/test-datos/contexto-${random.uuid}.properties")
class DrogueriaLosTresHermanosApplicationTests {

	@Test
	void contextLoads() {
	}

}
