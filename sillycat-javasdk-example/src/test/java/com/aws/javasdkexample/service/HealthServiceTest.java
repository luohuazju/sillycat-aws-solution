package com.aws.javasdkexample.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@SpringBootTest
@ActiveProfiles("dev")
public class HealthServiceTest {

	@Autowired
	HealthService healthService;

	@Test
	public void springBean() {
		Assert.notNull(healthService, "Spring Bean should be loadded!");
	}

}
