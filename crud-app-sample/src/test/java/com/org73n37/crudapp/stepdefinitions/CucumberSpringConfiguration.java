package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.autoconfigure.exclude=com.org73n37.crudapp.data.weaviate.config.WeaviateAutoConfiguration"
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    public static final KeyPair keyPair;
    public static final String publicKeyPem;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
            byte[] pubBytes = keyPair.getPublic().getEncoded();
            publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getEncoder().encodeToString(pubBytes) +
                    "\n-----END PUBLIC KEY-----";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.mode", () -> "DEVELOPMENT");
        registry.add("keycloak.test.public-key", () -> publicKeyPem);
        registry.add("keycloak.jwk-set-uri", () -> "http://localhost:8081/realms/crud-realm/protocol/openid-connect/certs");
    }

    @LocalServerPort
    private int port;

    public int getPort() {
        return port;
    }
}
