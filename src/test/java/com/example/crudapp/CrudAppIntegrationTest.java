package com.example.crudapp;

import com.example.crudapp.infrastructure.web.JavalinServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "server.port=0")
public class CrudAppIntegrationTest {

    private static final KeyPair keyPair;
    private static final String publicKeyPem;

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
        registry.add("keycloak.test.public-key", () -> publicKeyPem);
        registry.add("keycloak.jwk-set-uri", () -> "http://localhost:8081/realms/crud-realm/protocol/openid-connect/certs");
    }

    @Autowired
    private JavalinServer javalinServer;

    private String generateToken(String username, List<String> roles) {
        Map<String, Object> realmAccess = Map.of("roles", roles);
        return io.jsonwebtoken.Jwts.builder()
                .header()
                    .keyId("test-key-id")
                    .and()
                .subject(username)
                .claim("preferred_username", username)
                .claim("realm_access", realmAccess)
                .signWith(keyPair.getPrivate(), io.jsonwebtoken.Jwts.SIG.RS256)
                .compact();
    }

    @Test
    public void testMetadataEndpoint() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/metadata"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("products"));
    }

    @Test
    public void testProductsEndpointAuthorized() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        
        String testToken = generateToken("admin-user", List.of("ADMIN"));

        // 1. Get products list (should return 200 OK)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/products"))
                .header("Authorization", "Bearer " + testToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        // 2. Post a new product (should return 201 Created)
        String productJson = "{\"name\":\"Sample Product\",\"description\":\"A sample description\",\"price\":99.99}";
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/products"))
                .header("Authorization", "Bearer " + testToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(productJson))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());
        assertTrue(postResponse.body().contains("SAMPLE PRODUCT")); // ProductInterceptor makes name uppercase!
    }

    @Test
    public void testProductsEndpointForbidden() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        
        // Generate token with unprivileged role
        String guestToken = generateToken("guest-user", List.of("GUEST"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/products"))
                .header("Authorization", "Bearer " + guestToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("Insufficient privileges"));
    }

    @Test
    public void testProductsEndpointUnauthorized() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Call without Authorization Header
        HttpRequest requestNoAuth = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/products"))
                .GET()
                .build();

        HttpResponse<String> responseNoAuth = client.send(requestNoAuth, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, responseNoAuth.statusCode());

        // 2. Call with invalid token signature (using HMAC instead of RSA, or incorrect key)
        String badToken = io.jsonwebtoken.Jwts.builder()
                .subject("bad-user")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("a-different-secret-key-that-is-not-valid-hmac-key".getBytes()))
                .compact();

        HttpRequest requestBadAuth = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v2/products"))
                .header("Authorization", "Bearer " + badToken)
                .GET()
                .build();

        HttpResponse<String> responseBadAuth = client.send(requestBadAuth, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, responseBadAuth.statusCode());
    }
}
