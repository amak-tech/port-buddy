/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.netproxy;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class NetProxyApplication {

    static void main(final String[] args) {
        SpringApplication.run(NetProxyApplication.class, args);
    }

    @Bean
    CommandLineRunner onStart() {
        return args -> {
            log.info("Net Proxy service started");
        };
    }
}
