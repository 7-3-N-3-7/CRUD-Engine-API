package com.org73n37.crudapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Generic CRUD Application (Spring Boot)...");

        // 🔧 Generate a test token for development
        String testSecret = "your-very-secure-and-long-secret-key-for-jwt-validation";
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        String testToken = io.jsonwebtoken.Jwts.builder()
                .subject("test-user")
                .signWith(key)
                .compact();
        log.info("--------------------------------------------------");
        log.info("🔑 TEST JWT TOKEN (for Postman/Browser extension):");
        log.info("Bearer " + testToken);
        log.info("--------------------------------------------------");

        SpringApplication app = new SpringApplication(Main.class);
        app.setWebApplicationType(WebApplicationType.REACTIVE);
        app.run(args);
    }
}
