# Testing Strategy & Reference Documentation

Welcome to the **Dynamic CRUD Engine** testing suite. This project uses a multi-layered testing strategy combining isolated Unit-tests, module-level Integration-tests, and preventative Regression-tests to verify the correctness, performance, and security of the entire codebase.

---

## 🏗️ 1. Test Architecture & Classification

The test suite is structured into three distinct tiers:

### ⚡ Tier 1: Unit Tests
*   **Purpose**: Verify the smallest testable parts of the application (e.g. helper utilities, reflection caches, context propagation) in complete isolation.
*   **Execution Speed**: Sub-millisecond. No Spring ApplicationContext is booted, and no database or network calls are made.
*   **Key Examples**:
    *   [`ReflectionCacheTest.java`](file:///c:/Users/vase_/Desktop/Crud_application/crud-engine-core/src/test/java/com/org73n37/crudapp/infrastructure/mapping/ReflectionCacheTest.java): Verifies the runtime Reflection cache mechanism, caching behavior of constructors, fields, and custom annotations.
    *   [`TenantContextTest.java`](file:///c:/Users/vase_/Desktop/Crud_application/crud-engine-core/src/test/java/com/org73n37/crudapp/infrastructure/security/TenantContextTest.java): Verifies that tenant propagation is thread-isolated via a `ThreadLocal` structure and that contexts are cleared securely.

### 🔗 Tier 2: Integration Tests
*   **Purpose**: Verify that multiple classes, plugins, and physical data layers function correctly when combined.
*   **Execution Speed**: Medium to Slow. May boot a partial or full Spring WebFlux Context and connect to active database/storage layers.
*   **Key Examples**:
    *   [`InMemoryStorageProviderTest.java`](file:///c:/Users/vase_/Desktop/Crud_application/crud-engine-inmemory/src/test/java/com/org73n37/crudapp/data/inmemory/InMemoryStorageProviderTest.java): Tests the pluggable storage provider logic, specifically validating record creation, search, deletion, and robust multi-tenant data segregation.
    *   [`CrudAppIntegrationTest.java`](file:///c:/Users/vase_/Desktop/Crud_application/crud-app-sample/src/test/java/com/org73n37/crudapp/CrudAppIntegrationTest.java): A full end-to-end integration test suite of the reactive WebFlux layer. It boots Spring Boot on a random port and exercises:
        *   Security authorization bypass for development keys.
        *   Strict payload validation (unknown fields restriction) and unified Exception mapping.
        *   Dynamic sorting and query-filtering against a real database.
        *   Input XSS sanitization.
        *   Sliding Token Bucket rate limiting.

### 🛡️ Tier 3: Regression Tests
*   **Purpose**: Guard against the re-introduction of past bugs and protect crucial security invariants.
*   **Key Examples**:
    *   [`CrudResourceRbacTest.java`](file:///c:/Users/vase_/Desktop/Crud_application/crud-engine-core/src/test/java/com/org73n37/crudapp/infrastructure/annotations/CrudResourceRbacTest.java): Enforces the "Deny-by-Default" security constraint. It guarantees that any new resource declared with `@CrudResource` exposes *no* roles if the developer forgets to configure them, preventing accidental public exposure of data.

---

## 🚦 2. Running the Tests

### Prerequisites
Before running tests that require a database connection (like the full sample application integration tests), ensure the local container infrastructure is active.

```bash
# Spin up PostgreSQL mapped to port 5433
docker-compose up -d postgres
```

### Running the Full Test Suite
To execute all tests across all modules under the parent project:
```bash
mvn clean test
```

### Running Module-Specific Tests
If you are working inside a specific submodule, you can run only its tests:

```bash
# Run Core engine unit/regression tests
mvn test -pl crud-engine-core

# Run In-Memory storage integration tests
mvn test -pl crud-engine-inmemory

# Run Sample application full integration tests
mvn test -pl crud-app-sample
```

---

## 🧪 3. Best Practices for Adding Tests

1.  **Deny-by-Default Assertion**: Any test asserting access control should verify that guest/unauthenticated traffic is explicitly rejected with `401 Unauthorized` or `403 Forbidden` before testing successful authorization cases.
2.  **ThreadLocal Cleanup**: Always clear context states in a `@AfterEach` or `finally` block to prevent thread pollution in shared web server threads (such as `TenantContext.clear()`).
3.  **Database Seeding**: Clean up databases or use distinct transaction boundaries/tenant scopes for separate test methods to ensure tests are deterministic.
