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
    public void testRequestTracingCorrelationId() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // Send request with custom correlation ID
        String customId = "my-custom-correlation-12345";
        HttpRequest requestWithId = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metadata"))
                .header("X-Request-ID", customId)
                .GET()
                .build();
        HttpResponse<String> responseWithId = client.send(requestWithId, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseWithId.statusCode());
        assertEquals(customId, responseWithId.headers().firstValue("X-Request-ID").orElse(""));

        // Send request without correlation ID (should generate one)
        HttpRequest requestNoId = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metadata"))
                .GET()
                .build();
        HttpResponse<String> responseNoId = client.send(requestNoId, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseNoId.statusCode());
        String generatedId = responseNoId.headers().firstValue("X-Request-ID").orElse("");
        assertTrue(generatedId != null && !generatedId.isBlank());
    }

    @Test
    public void testUnifiedExceptionMapping() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        String testToken = generateToken("admin-user", List.of("ADMIN"));

        // 1. Test validation error (empty product payload)
        String invalidProductJson = "{\"name\":\"\",\"description\":\"\",\"price\":-10.00}";
        HttpRequest invalidPost = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + testToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidProductJson))
                .build();
        HttpResponse<String> invalidResponse = client.send(invalidPost, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, invalidResponse.statusCode());
        String body = invalidResponse.body();
        assertTrue(body.contains("\"status\":400"));
        assertTrue(body.contains("\"error\":\"Bad Request\""));
        assertTrue(body.contains("\"message\":\"Validation failed\""));
        assertTrue(body.contains("\"requestId\":"));
        assertTrue(body.contains("\"details\":{"));

        // 2. Test resource missing (access non-existent product)
        HttpRequest getNonExistent = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products/999999"))
                .header("Authorization", "Bearer " + testToken)
                .GET()
                .build();
        HttpResponse<String> missingResponse = client.send(getNonExistent, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, missingResponse.statusCode());
        String missingBody = missingResponse.body();
        assertTrue(missingBody.contains("\"status\":404"));
        assertTrue(missingBody.contains("\"error\":\"Not Found\""));
        assertTrue(missingBody.contains("\"message\":\"Resource 'products' with ID 999999 not found\""));
        assertTrue(missingBody.contains("\"requestId\":"));
    }

    @Test
    public void testHealthCheckProbes() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Get liveness probe
        HttpRequest requestLiveness = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/health/liveness"))
                .GET()
                .build();
        HttpResponse<String> responseLiveness = client.send(requestLiveness, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseLiveness.statusCode());
        assertEquals("UP", responseLiveness.body());

        // 2. Get readiness probe
        HttpRequest requestReadiness = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/health/readiness"))
                .GET()
                .build();
        HttpResponse<String> responseReadiness = client.send(requestReadiness, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseReadiness.statusCode());
        assertTrue(responseReadiness.body().contains("\"status\":\"UP\""));
        assertTrue(responseReadiness.body().contains("\"database\":\"UP\""));
    }

    @Test
    public void testMetadataEndpoint() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metadata"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("products"));
    }

    @Test
    public void testSwaggerAndApiDocsEndpoints() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Get Swagger-UI html
        HttpRequest requestSwagger = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/swagger-ui"))
                .GET()
                .build();
        HttpResponse<String> responseSwagger = client.send(requestSwagger, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseSwagger.statusCode());
        assertTrue(responseSwagger.body().contains("Swagger UI - Generic CRUD Engine"));

        // 2. Get OpenAPI JSON
        HttpRequest requestDocs = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api-docs"))
                .GET()
                .build();
        HttpResponse<String> responseDocs = client.send(requestDocs, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseDocs.statusCode());
        assertTrue(responseDocs.body().contains("Generic CRUD Engine API"));
        assertTrue(responseDocs.body().contains("/api/products"));
    }

    @Test
    public void testAuditingMultiTenancyAndDynamicAttributes() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Create a token for tenant 'tenant-a'
        Map<String, Object> realmAccess = Map.of("roles", List.of("ADMIN"));
        String tokenA = io.jsonwebtoken.Jwts.builder()
                .subject("user-a")
                .claim("preferred_username", "user-a")
                .claim("tenant", "tenant-a")
                .claim("realm_access", realmAccess)
                .signWith(keyPair.getPrivate(), io.jsonwebtoken.Jwts.SIG.RS256)
                .compact();

        // 2. Post product with dynamic attributes under tenant-a
        String productJson = "{" +
                "\"name\":\"Tenant A Product\"," +
                "\"description\":\"Dynamic attributes test\"," +
                "\"price\":50.00," +
                "\"attributes\":{\"color\":\"blue\",\"size\":\"large\"}" +
                "}";

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + tokenA)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(productJson))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());
        String body = postResponse.body();
        
        // Assert auditing, version, tenant_id, and attributes are present in response JSON
        assertTrue(body.contains("\"createdBy\":\"user-a\""));
        assertTrue(body.contains("\"tenantId\":\"tenant-a\""));
        assertTrue(body.contains("\"version\":0"));
        assertTrue(body.contains("\"attributes\":{"));
        assertTrue(body.contains("\"color\":\"blue\""));
        assertTrue(body.contains("\"size\":\"large\""));

        // Extract ID of created product to verify isolation
        int idIdx = body.indexOf("\"id\":");
        int endIdx = body.indexOf(",", idIdx);
        if (endIdx == -1) endIdx = body.indexOf("}", idIdx);
        String idStr = body.substring(idIdx + 5, endIdx).trim();
        Long productId = Long.parseLong(idStr);

        // 3. Create a token for tenant 'tenant-b'
        String tokenB = io.jsonwebtoken.Jwts.builder()
                .subject("user-b")
                .claim("preferred_username", "user-b")
                .claim("tenant", "tenant-b")
                .claim("realm_access", realmAccess)
                .signWith(keyPair.getPrivate(), io.jsonwebtoken.Jwts.SIG.RS256)
                .compact();

        // 4. Fetch products list with tenant-b token -> should NOT return tenant-a product
        HttpRequest getRequestB = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + tokenB)
                .GET()
                .build();
        HttpResponse<String> getResponseB = client.send(getRequestB, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponseB.statusCode());
        assertTrue(!getResponseB.body().contains("Tenant A Product"));

        // 5. Fetch the product directly by ID with tenant-b token -> should return 404 Not Found (filtered by tenant)
        HttpRequest getSingleRequestB = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products/" + productId))
                .header("Authorization", "Bearer " + tokenB)
                .GET()
                .build();
        HttpResponse<String> getSingleResponseB = client.send(getSingleRequestB, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getSingleResponseB.statusCode());
    }

    @Test
    public void testProductsEndpointAuthorized() throws Exception {
        int port = javalinServer.getPort();
        HttpClient client = HttpClient.newHttpClient();
        
        String testToken = generateToken("admin-user", List.of("ADMIN"));

        // 1. Get products list (should return 200 OK)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + testToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        // 2. Post a new product (should return 201 Created)
        String productJson = "{\"name\":\"Sample Product\",\"description\":\"A sample description\",\"price\":99.99}";
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
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
                .uri(URI.create("http://localhost:" + port + "/api/products"))
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
                .uri(URI.create("http://localhost:" + port + "/api/products"))
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
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + badToken)
                .GET()
                .build();

        HttpResponse<String> responseBadAuth = client.send(requestBadAuth, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, responseBadAuth.statusCode());
    }
}
