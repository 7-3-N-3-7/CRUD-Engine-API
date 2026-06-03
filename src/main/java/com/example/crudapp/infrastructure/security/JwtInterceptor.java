package com.example.crudapp.infrastructure.security;

import com.example.crudapp.infrastructure.annotations.CrudResource;
import com.example.crudapp.logic.DynamicCrudManager;
import com.example.crudapp.logic.ResourceMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtInterceptor implements Handler {
    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Value("${keycloak.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.test.public-key:}")
    private String testPublicKeyPem;

    @Autowired
    private DynamicCrudManager crudManager;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, PublicKey> jwkCache = new ConcurrentHashMap<>();
    private PublicKey parsedTestPublicKey = null;
    private final Object testKeyLock = new Object();

    @Override
    public void handle(Context ctx) throws Exception {
        // Skip metadata public endpoint
        if (ctx.path().equals("/api/v2/metadata")) {
            return;
        }

        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(401).result("Missing or invalid token");
            ctx.skipRemainingHandlers();
            return;
        }

        String token = authHeader.substring(7);
        try {
            PublicKey verificationKey = getVerificationKey(token);
            if (verificationKey == null) {
                ctx.status(401).result("Signing key not found for token verification");
                ctx.skipRemainingHandlers();
                return;
            }

            Claims claims = Jwts.parser()
                    .verifyWith(verificationKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.get("preferred_username", String.class);
            if (username == null) {
                username = claims.getSubject();
            }
            ctx.attribute("user", username);

            // Extract roles and perform RBAC
            List<String> userRoles = new ArrayList<>();
            Map<String, Object> realmAccess = claims.get("realm_access", Map.class);
            if (realmAccess != null) {
                List<?> rolesList = (List<?>) realmAccess.get("roles");
                if (rolesList != null) {
                    for (Object r : rolesList) {
                        userRoles.add(r.toString().toUpperCase());
                    }
                }
            }

            // Identify resource for route
            String resource = getResourceName(ctx);
            if (resource != null) {
                ResourceMetadata<?, ?> metadata = crudManager.getMetadata(resource);
                if (metadata != null) {
                    Class<?> entityClass = metadata.getEntityClass();
                    CrudResource annotation = entityClass.getAnnotation(CrudResource.class);
                    if (annotation != null) {
                        String[] allowedRoles = annotation.roles();
                        boolean authorized = false;
                        for (String allowedRole : allowedRoles) {
                            if ("ANYONE".equalsIgnoreCase(allowedRole)) {
                                authorized = true;
                                break;
                            }
                            if (userRoles.contains(allowedRole.toUpperCase())) {
                                authorized = true;
                                break;
                            }
                        }

                        if (!authorized) {
                            ctx.status(403).result("Forbidden: Insufficient privileges");
                            ctx.skipRemainingHandlers();
                            return;
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Token verification failed", e);
            ctx.status(401).result("Invalid token: " + e.getMessage());
            ctx.skipRemainingHandlers();
        }
    }

    private String getResourceName(Context ctx) {
        String resource = null;
        try {
            resource = ctx.pathParam("resource");
        } catch (Exception e) {
            // ignore
        }
        if (resource == null || resource.isEmpty()) {
            String path = ctx.path();
            if (path.startsWith("/api/v2/")) {
                String remaining = path.substring(8);
                int slashIdx = remaining.indexOf('/');
                if (slashIdx != -1) {
                    resource = remaining.substring(0, slashIdx);
                } else {
                    resource = remaining;
                }
            }
        }
        return resource;
    }

    private PublicKey getVerificationKey(String token) throws Exception {
        if (testPublicKeyPem != null && !testPublicKeyPem.trim().isEmpty()) {
            synchronized (testKeyLock) {
                if (parsedTestPublicKey == null) {
                    parsedTestPublicKey = parsePemPublicKey(testPublicKeyPem);
                }
            }
            return parsedTestPublicKey;
        }

        String kid = extractKid(token);
        if (kid == null) {
            return null;
        }

        if (jwkCache.containsKey(kid)) {
            return jwkCache.get(kid);
        }

        fetchJwks();
        return jwkCache.get(kid);
    }

    private String extractKid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 0) {
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
                return (String) header.get("kid");
            }
        } catch (Exception e) {
            log.warn("Failed to extract kid from token header", e);
        }
        return null;
    }

    private PublicKey parsePemPublicKey(String pem) throws Exception {
        String cleaned = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private synchronized void fetchJwks() {
        try {
            log.info("Fetching Keycloak JWKS from: {}", jwkSetUri);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwkSetUri))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch JWKS: status code {}", response.statusCode());
                return;
            }

            Map<String, Object> jwks = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            if (keys != null) {
                for (Map<String, Object> key : keys) {
                    String keyId = (String) key.get("kid");
                    String kty = (String) key.get("kty");
                    if ("RSA".equals(kty)) {
                        String n = (String) key.get("n");
                        String e = (String) key.get("e");

                        byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
                        byte[] exponentBytes = Base64.getUrlDecoder().decode(e);
                        BigInteger modulus = new BigInteger(1, modulusBytes);
                        BigInteger exponent = new BigInteger(1, exponentBytes);
                        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                        KeyFactory factory = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = factory.generatePublic(spec);

                        jwkCache.put(keyId, publicKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching or parsing JWKS from URL " + jwkSetUri, e);
        }
    }
}
