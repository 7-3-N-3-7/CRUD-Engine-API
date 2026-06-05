package com.example.crudapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    @LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.crudapp.infrastructure.web.ReactiveRateLimiterFilter rateLimiterFilter;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        if (rateLimiterFilter != null) {
            rateLimiterFilter.reset();
        }
    }

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

    private String generateTokenWithTenant(String username, List<String> roles, String tenant) {
        Map<String, Object> realmAccess = Map.of("roles", roles);
        return io.jsonwebtoken.Jwts.builder()
                .header()
                    .keyId("test-key-id")
                    .and()
                .subject(username)
                .claim("preferred_username", username)
                .claim("tenant", tenant)
                .claim("realm_access", realmAccess)
                .signWith(keyPair.getPrivate(), io.jsonwebtoken.Jwts.SIG.RS256)
                .compact();
    }

    @Test
    public void testRequestTracingCorrelationId() throws Exception {
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
        HttpClient client = HttpClient.newHttpClient();

        // 1. Create a token for tenant 'tenant-a'
        String tokenA = generateTokenWithTenant("user-a", List.of("ADMIN"), "tenant-a");

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
        String tokenB = generateTokenWithTenant("user-b", List.of("ADMIN"), "tenant-b");

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

    @Test
    public void testDynamicFilteringAndSorting() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String adminToken = generateToken("admin-user", List.of("ADMIN"));

        // 1. Post two products with different prices
        String p1 = "{\"name\":\"Alpha Laptop\",\"description\":\"High performance laptop\",\"price\":1200.00}";
        String p2 = "{\"name\":\"Beta Phone\",\"description\":\"Smart mobile phone\",\"price\":800.00}";

        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(p1))
                .build(), HttpResponse.BodyHandlers.ofString());

        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(p2))
                .build(), HttpResponse.BodyHandlers.ofString());

        // 2. Get with sorting by price descending
        HttpRequest sortedGet = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products?sort=price,desc&size=100"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> sortedResponse = client.send(sortedGet, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, sortedResponse.statusCode());
        String sortedBody = sortedResponse.body();
        // Alpha Laptop (1200) should appear before Beta Phone (800) in descending sort
        int idxLaptop = sortedBody.indexOf("ALPHA LAPTOP");
        int idxPhone = sortedBody.indexOf("BETA PHONE");
        assertTrue(idxLaptop != -1);
        assertTrue(idxPhone != -1);
        assertTrue(idxLaptop < idxPhone);

        // 3. Get with filtering (price > 1000)
        HttpRequest filteredGet = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products?price_gt=1000&size=100"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> filteredResponse = client.send(filteredGet, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, filteredResponse.statusCode());
        String filteredBody = filteredResponse.body();
        assertTrue(filteredBody.contains("ALPHA LAPTOP"));
        assertTrue(!filteredBody.contains("BETA PHONE"));
    }

    @Test
    public void testRateLimiterSecurityEvent() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String testToken = generateToken("admin-user", List.of("ADMIN"));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + testToken)
                .GET()
                .build();

        // Perform 50 quick requests (should succeed or return 403/401/200, but rate limiter shouldn't block yet)
        // Then perform 1 more which should be blocked with 429.
        int rateLimitCapacity = 50;
        boolean rateLimited = false;
        
        for (int i = 0; i < rateLimitCapacity + 5; i++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                rateLimited = true;
                assertTrue(response.body().contains("API rate limit exceeded"));
                break;
            }
        }
        assertTrue(rateLimited, "Request should be rate limited with HTTP 429 after exceeding limit");
    }

    @Test
    public void testCorsOriginWhitelisting() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // 1. Whitelisted Origin
        HttpRequest requestWhitelisted = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metadata"))
                .header("Origin", "http://localhost:5173")
                .GET()
                .build();
        HttpResponse<String> responseWhitelisted = client.send(requestWhitelisted, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseWhitelisted.statusCode());
        assertEquals("http://localhost:5173", responseWhitelisted.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

        // 2. Non-whitelisted Origin (should return 403 Forbidden since the origin is not allowed)
        HttpRequest requestNonWhitelisted = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/metadata"))
                .header("Origin", "http://malicious.com")
                .GET()
                .build();
        HttpResponse<String> responseNonWhitelisted = client.send(requestNonWhitelisted, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, responseNonWhitelisted.statusCode());
    }

    @Test
    public void testInputXssSanitization() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String testToken = generateToken("admin-user", List.of("ADMIN"));

        // Post a product with XSS payload in description
        String xssProductJson = "{" +
                "\"name\":\"Clean Laptop\"," +
                "\"description\":\"<script>alert('xss')</script>Description clean of script tags\"," +
                "\"price\":1500.00" +
                "}";

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + testToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(xssProductJson))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());
        String body = postResponse.body();
        // The script tag should be stripped out
        assertTrue(!body.contains("<script>"));
        assertTrue(body.contains("Description clean of script tags"));
    }

    @Test
    public void testStrictPayloadRestrictions() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String testToken = generateToken("admin-user", List.of("ADMIN"));

        // Post a product with unexpected/unwhitelisted fields
        String badProductJson = "{" +
                "\"name\":\"Dirty Laptop\"," +
                "\"description\":\"Unexpected field check\"," +
                "\"price\":1500.00," +
                "\"hackerField\":\"unwhitelisted\"" +
                "}";

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("Authorization", "Bearer " + testToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(badProductJson))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        // Since we enabled fail-on-unknown-properties globally/locally, it should return 400 Bad Request
        assertEquals(400, postResponse.statusCode());
    }
}
