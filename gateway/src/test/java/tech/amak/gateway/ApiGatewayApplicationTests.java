/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;

import tech.amak.portbuddy.gateway.ApiGatewayApplication;

@SpringBootTest(
    classes = ApiGatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySources(
    {@TestPropertySource(properties = {"app.ssl.enabled=false"})}
)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
