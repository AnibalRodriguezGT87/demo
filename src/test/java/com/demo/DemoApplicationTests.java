package com.demo;

import org.cccc_online.commons.crypto.PGPProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.batch.job.name=jobMaking")
class DemoApplicationTests {

	@Autowired
	private PGPProperties pgpProperties;

	@Test
	void contextLoads() {
		assertNotNull(pgpProperties);
	}

}
