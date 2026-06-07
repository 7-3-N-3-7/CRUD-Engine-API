package com.org73n37.crudapp.web;

import com.org73n37.crudapp.logic.CrudEngine;
import com.org73n37.crudapp.logic.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Duration;
import java.util.*;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private CrudEngine crudEngine;

    @org.springframework.beans.factory.annotation.Value("${server.port:8080}")
    private String serverPort;

    private final HttpClient httpClient;

    public DashboardController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static class ArchNode {
        private final String id;
        private final String title;
        private final String subtitle;
        private final String desc;

        public ArchNode(String id, String title, String subtitle, String desc) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.desc = desc;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getDesc() { return desc; }
    }

    private static final List<ArchNode> ARCH_NODES = List.of(
            new ArchNode("client", "Client Webapp", "Thymeleaf & Java", 
                    "Communicates with REST endpoints by signing OAuth2-compliant JWT tokens on the server using private key spec, supporting correlation request tracing (X-Request-ID) and multi-tenancy."),
            new ArchNode("rate_limiter", "Rate Limiter", "Token Bucket Filter", 
                    "Intercepts incoming WebFlux requests, enforcing a sliding Token Bucket limit of 50 requests/min per IP to block denial-of-service attempts. Returns standard RFC 7807 (HTTP 429) details when exceeded."),
            new ArchNode("gateway", "API Controllers", "Byte Buddy Beans", 
                    "At startup, a BeanFactoryPostProcessor scans annotations and uses Byte Buddy to dynamically compile and register separate WebFlux RestController classes for each entity at runtime."),
            new ArchNode("exceptions", "RFC 7807 Handler", "Unified ControllerAdvice", 
                    "A unified handler intercepts database integrity errors, validation violations, concurrency conflicts, and security/access exceptions, mapping them to standard RFC 7807 Problem Details."),
            new ArchNode("security", "Security Filters", "Reactive WebFilter", 
                    "A ReactiveJwtFilter validates the cryptographic token signature, extracts roles and tenant variables, validates authorization access rules, and propagates values into the Reactor context."),
            new ArchNode("service", "Decoupled Core", "Service Registry", 
                    "Delegates business logic. Decouples controllers from entities via a ServiceRegistry lookup that returns custom service overrides or defaults to a dynamic reactive CrudService."),
            new ArchNode("data", "Dynamic Data Access", "JPA Graphs & Specs", 
                    "Executes database queries on boundedElastic scheduler threads. Creates dynamic JPA specifications from query filters and injects Entity Graphs to solve the N+1 select problem."),
            new ArchNode("database", "RLS Database", "PostgreSQL RLS", 
                    "Enforces Row-Level Security (RLS) policies based on transaction-level context setting via SET LOCAL app.current_tenant, isolating records dynamically per tenant.")
    );

    @GetMapping
    public Mono<String> showDashboard(
            Model model,
            WebSession session,
            @RequestParam(name = "resource", required = false) String resource,
            @RequestParam(name = "op", required = false) String op,
            @RequestParam(name = "activeNode", required = false) String activeNode) {

        // Establish Defaults in WebSession if not already set
        if (session.getAttribute("tenantId") == null) session.getAttributes().put("tenantId", "tenant-a");
        if (session.getAttribute("username") == null) session.getAttributes().put("username", "admin-user");
        if (session.getAttribute("selectedRole") == null) session.getAttributes().put("selectedRole", "ADMIN");

        String selectedResource = (resource != null) ? resource : "products";
        String selectedOp = (op != null) ? op : "GET_ALL";
        String selectedNode = (activeNode != null) ? activeNode : "client";

        // Find active ArchNode
        ArchNode activeArchNode = ARCH_NODES.stream()
                .filter(n -> n.getId().equals(selectedNode))
                .findFirst()
                .orElse(ARCH_NODES.get(0));

        // Get Metadata and Active Resource Metadata
        Map<String, ResourceMetadata<?, ?>> resources = crudEngine.getResources();
        ResourceMetadata<?, ?> currentResource = resources.get(selectedResource);

        // Generate JWT Token based on current session config
        String jwtToken = generateToken(
                (String) session.getAttribute("username"),
                (String) session.getAttribute("tenantId"),
                (String) session.getAttribute("selectedRole")
        );

        // Populate Model
        model.addAttribute("archNodes", ARCH_NODES);
        model.addAttribute("activeArchNode", activeArchNode);
        model.addAttribute("resources", resources);
        model.addAttribute("selectedResource", selectedResource);
        model.addAttribute("selectedOp", selectedOp);
        model.addAttribute("currentResource", currentResource);
        model.addAttribute("jwtToken", jwtToken);

        // Bind Session Credentials to model for inputs
        model.addAttribute("tenantId", session.getAttribute("tenantId"));
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("selectedRole", session.getAttribute("selectedRole"));

        // Bind Console outputs
        model.addAttribute("consoleReq", session.getAttribute("consoleReq"));
        model.addAttribute("consoleRes", session.getAttribute("consoleRes"));

        return Mono.just("dashboard");
    }

    @PostMapping("/security")
    public Mono<String> updateSecurity(
            WebSession session,
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "selectedRole") String selectedRole,
            @RequestParam(name = "selectedResource") String selectedResource,
            @RequestParam(name = "selectedOp") String selectedOp) {

        session.getAttributes().put("tenantId", tenantId);
        session.getAttributes().put("username", username);
        session.getAttributes().put("selectedRole", selectedRole);

        return Mono.just("redirect:/dashboard?resource=" + selectedResource + "&op=" + selectedOp);
    }

    @PostMapping("/execute")
    public Mono<String> executeQuery(
            WebSession session,
            @RequestParam(name = "selectedResource") String selectedResource,
            @RequestParam(name = "selectedOp") String selectedOp,
            @RequestParam(name = "singleId", required = false) String singleId,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sortOrder,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam Map<String, String> allParams) {

        return Mono.defer(() -> {
            String username = session.getAttribute("username");
            String tenantId = session.getAttribute("tenantId");
            String role = session.getAttribute("selectedRole");
            String jwtToken = generateToken(username, tenantId, role);

            // Construct HTTP path
            String baseUrl = "http://localhost:" + serverPort + "/api/" + selectedResource;
            String method = "GET";
            String requestBody = "";

            if (selectedOp.equals("GET_ALL")) {
                StringBuilder queryParams = new StringBuilder();
                queryParams.append("?page=").append(page != null ? page : "0");
                queryParams.append("&size=").append(size != null ? size : "10");
                if (sortField != null && !sortField.isEmpty()) {
                    queryParams.append("&sort=").append(sortField).append(",").append(sortOrder != null ? sortOrder : "asc");
                }
                
                // Add query filter builder params (field=value and field_operator=value)
                allParams.forEach((k, v) -> {
                    if (k.startsWith("filter_field_") && v != null && !v.trim().isEmpty()) {
                        String idx = k.substring(13);
                        String val = allParams.get("filter_value_" + idx);
                        String opStr = allParams.get("filter_op_" + idx);
                        if (val != null && !val.trim().isEmpty()) {
                            String key = opStr.equals("=") ? v : v + opStr;
                            queryParams.append("&").append(key).append("=").append(val);
                        }
                    }
                });

                baseUrl += queryParams.toString();
            } else if (selectedOp.equals("GET_BY_ID") || selectedOp.equals("DELETE")) {
                baseUrl += "/" + (singleId != null ? singleId.trim() : "");
                if (selectedOp.equals("DELETE")) {
                    method = "DELETE";
                }
            } else if (selectedOp.equals("POST") || selectedOp.equals("PUT")) {
                method = selectedOp;
                if (selectedOp.equals("PUT")) {
                    baseUrl += "/" + (singleId != null ? singleId.trim() : "");
                }

                // Construct JSON request body from dynamic fields
                Map<String, Object> bodyMap = new LinkedHashMap<>();
                ResourceMetadata<?, ?> metadata = crudEngine.getResources().get(selectedResource);
                if (metadata != null) {
                    for (ResourceMetadata.FieldInfo f : metadata.getFields()) {
                        String val = allParams.get("field_" + f.getName());
                        if (val != null && !val.trim().isEmpty()) {
                            if (f.getType().equals("Double") || f.getType().equals("Float") || f.getType().equals("Price")) {
                                bodyMap.put(f.getName(), Double.parseDouble(val));
                            } else if (f.getType().equals("Long") || f.getType().equals("Integer")) {
                                bodyMap.put(f.getName(), Long.parseLong(val));
                            } else if (f.getType().equals("Boolean")) {
                                bodyMap.put(f.getName(), Boolean.parseBoolean(val));
                            } else {
                                bodyMap.put(f.getName(), val);
                            }
                        }
                    }
                }
                try {
                    requestBody = new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(bodyMap);
                } catch (Exception e) {
                    log.error("Failed to build JSON body", e);
                }
            }

            // Record request logs
            Map<String, String> reqHeaders = new LinkedHashMap<>();
            reqHeaders.put("Accept", "application/json");
            reqHeaders.put("Authorization", "Bearer " + jwtToken.substring(0, 15) + "...");
            if (method.equals("POST") || method.equals("PUT")) {
                reqHeaders.put("Content-Type", "application/json");
            }

            Map<String, String> consoleReq = new LinkedHashMap<>();
            consoleReq.put("url", baseUrl);
            consoleReq.put("method", method);
            consoleReq.put("headers", reqHeaders.toString());
            consoleReq.put("body", requestBody);
            session.getAttributes().put("consoleReq", consoleReq);

            // Execute HTTP Request
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + jwtToken);

                if (method.equals("GET")) {
                    reqBuilder.GET();
                } else if (method.equals("DELETE")) {
                    reqBuilder.DELETE();
                } else {
                    reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(requestBody));
                }

                return Mono.fromCompletionStage(httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofString()))
                        .map(res -> {
                            Map<String, Object> consoleRes = new LinkedHashMap<>();
                            consoleRes.put("status", res.statusCode());
                            consoleRes.put("statusText", getStatusText(res.statusCode()));
                            
                            Map<String, String> resHeaders = new LinkedHashMap<>();
                            res.headers().map().forEach((k, v) -> resHeaders.put(k, String.join(", ", v)));
                            consoleRes.put("headers", resHeaders);

                            // Format JSON response
                            String body = res.body();
                            try {
                                tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                                Object json = mapper.readValue(body, Object.class);
                                body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            } catch (Exception e) {
                                // Not JSON, keep raw
                            }
                            consoleRes.put("body", body);
                            consoleRes.put("isError", res.statusCode() >= 400);

                            session.getAttributes().put("consoleRes", consoleRes);
                            return "redirect:/dashboard?resource=" + selectedResource + "&op=" + selectedOp;
                        });
            } catch (Exception e) {
                log.error("Execution failed", e);
                Map<String, Object> consoleRes = new LinkedHashMap<>();
                consoleRes.put("status", 500);
                consoleRes.put("statusText", "Server Connectivity Error");
                consoleRes.put("headers", Collections.emptyMap());
                consoleRes.put("body", "Failed to connect to the backend server: " + e.getMessage());
                consoleRes.put("isError", true);
                session.getAttributes().put("consoleRes", consoleRes);
                return Mono.just("redirect:/dashboard?resource=" + selectedResource + "&op=" + selectedOp);
            }
        });
    }

    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 429 -> "Too Many Requests";
            default -> "Internal Error";
        };
    }

    private String generateToken(String username, String tenantId, String role) {
        try {
            PrivateKey key = getPrivateVerificationKey();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", "test-key-id");

            List<String> roles = role.equals("NONE") ? Collections.emptyList() : List.of(role);
            Map<String, Object> realmAccess = new LinkedHashMap<>();
            realmAccess.put("roles", roles);

            return io.jsonwebtoken.Jwts.builder()
                    .header().add(header).and()
                    .subject(username)
                    .claim("preferred_username", username)
                    .claim("tenant", tenantId)
                    .claim("realm_access", realmAccess)
                    .issuer("http://localhost:8081/realms/crud-realm")
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(key, io.jsonwebtoken.Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            log.error("Token generation failed", e);
            return "";
        }
    }

    private static PrivateKey getPrivateVerificationKey() throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode("tzBhHDmFE0O1nv4B2ZWnVmuvswS9Vrzz2swgQ49smMgTdCi-q_8Ka-bH99AiQab78dM1vk0Q8ot2_Ha-mgBeG2G0CTFDMLySjVxGiBcFI-Z-4t5_nPlFVTsjz5Sjbt2piFCEZqZzEEybb6nbLCXdzokCAvURPBwCAwSr1iLzFTxiHphPUYvG4mdQEJgvJSVbECP9YvXLJwHzvARsh3hWVE2LxTsa2xIZa2cukqi05S0G4oQxbFJHlDwoh5FyzHctFnArzVDESHaEeSOeI_oUNLyZHwQxxD1SRuZ1XsYr-hziEciv4Le5aOWs7At4bHh6vz25HX__fpDMUkB8c7Xcdw");
        byte[] eBytes = Base64.getUrlDecoder().decode("AQAB");
        byte[] dBytes = Base64.getUrlDecoder().decode("FbtTieROTn7AfmARGsg6Kx0TTDV1ELknD1Rv72kwWjTsueGrh6lLNIm9kenLETwctLojAgm6ckuWjf9i6noTWpmN9hk1_gN2OfbcbICZR4JXQyq0u4vcFxmw9zXhkuFmSe7jW29w7wS829OsAIzCx3b8GhsLNF-jjXVxvTGK4istXnjPq7yoUeEAnsrlYUzuQDInOMmbDhW3DNcrK_RltaTgA8Ga3dQo0ev94V93bnqv7oTegHLcwI_5pdPPt-PR_0CGmsK8e9Ew6CVg23W4i6D8IJNrLP90PugORKjP5NNuRojSe8K5uCOkxClq4z16UHzP9StWazH0RSYBxhv9IQ");
        byte[] pBytes = Base64.getUrlDecoder().decode("7qL1rioac5lsKwuo4DBCAzp8AG7TkMqGz4H8jDmE5B125PGmiluqYpnni12MzS0uPT6w8O3BlLydSp1YdvlLWzq8hgz7_7UnojjPK_if9zFN3RXclInSD_Eu2sUuXbAFkDG6gjdp3ZaqeLgqXhbkXPSg-VI724j0-j02SGepieU");
        byte[] qBytes = Base64.getUrlDecoder().decode("xISbiNBIq0WDVoyGJ4ZlLguk2KL5I8Lhyz0H63DeRhyswdmhKodtd09BNq1yizHB0H43toHE52nWo5iO8Ha53133rikKSfURyl4nGtFw5iI_BQoXd0g0sLe-SE07SIzNapJrki4_UmW-y86cP_tNKVE0YF7vo-XhB6E8YFd1tys");
        byte[] dpBytes = Base64.getUrlDecoder().decode("SjdILRhPDbCjYWfI56Bah2KC-id9iMRT1OlaP8oLuF4pgd5dqx4DCZNP3ZoEljL89HMw2F05HSbjzDbPMoEpnH_R7ebP4KDYaK0-UTCLn3cn_iA0b8XFHMwnhEZauyxpLoUouiK9u_qFnfG4y3ZXI0m5XpDiqM4ZUlIDNdV3drk");
        byte[] dqBytes = Base64.getUrlDecoder().decode("eKFB7CCWivPXpDgMXZTE5Rfmr8iSkF4fRieHhgG5n2YYscHKiZWqH1O6HzsnFcSMSVRBFLnhyX-RbsjF7VujyzYeRH0SwMU7j3JuJKst10ZsUsaYEvNyzItttWobGvS7X1DT0V6sJgMotGh2R1wWSGd9dC6ygXQpxwo1SppFOxM");
        byte[] qiBytes = Base64.getUrlDecoder().decode("x0h5WWp8TgBW1jY142PnpNQvquLhiznwx73kjtzfA3VqTg3e6bXlPuVInm2AGMvvhqRp7vab7rrq3ixWUWTEvpq4py0WFElLwVtmIrC9UpX0J8Q2YyvhJuZH1TZVWJYSDz3UTQRSKZxOaXENAD0qYSIL1AFraKYqRJi81vI8TUc");

        java.math.BigInteger n = new java.math.BigInteger(1, nBytes);
        java.math.BigInteger e = new java.math.BigInteger(1, eBytes);
        java.math.BigInteger d = new java.math.BigInteger(1, dBytes);
        java.math.BigInteger p = new java.math.BigInteger(1, pBytes);
        java.math.BigInteger q = new java.math.BigInteger(1, qBytes);
        java.math.BigInteger dp = new java.math.BigInteger(1, dpBytes);
        java.math.BigInteger qd = new java.math.BigInteger(1, dqBytes);
        java.math.BigInteger qi = new java.math.BigInteger(1, qiBytes);

        RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, qd, qi);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}
