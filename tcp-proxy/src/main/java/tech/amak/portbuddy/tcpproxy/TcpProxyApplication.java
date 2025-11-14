package tech.amak.portbuddy.tcpproxy;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class TcpProxyApplication {

    static void main(String[] args) {
        SpringApplication.run(TcpProxyApplication.class, args);
    }

    @Bean
    CommandLineRunner onStart() {
        return args -> {
            log.info("TCP Proxy service started (placeholder)");
        };
    }
}
