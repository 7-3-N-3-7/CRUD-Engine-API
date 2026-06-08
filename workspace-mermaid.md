# Workspace Architecture (Mermaid Diagrams)

This document contains top-level visual diagrams showing how all sub-modules interplay in the Zero-Code CRUD Engine platform.

## 1. Submodule Dependency Graph

```mermaid
graph TD
    %% Submodules
    starter[crud-engine-spring-boot-starter]
    core[crud-engine-core]
    webflux[crud-engine-webflux]
    inmemory[crud-engine-inmemory]
    jpa[crud-engine-jpa]
    weaviate[crud-engine-weaviate]
    keycloak[crud-engine-security-keycloak]
    ratelimit[crud-engine-plugin-ratelimiter]
    auditlog[crud-engine-plugin-auditlog]
    sample[crud-app-sample]

    %% Relationships
    starter --> core
    starter --> webflux
    
    webflux --> core
    inmemory --> core
    jpa --> core
    weaviate --> core
    keycloak --> core
    ratelimit --> core
    auditlog --> core
    
    sample --> starter
    sample --> jpa
    sample --> keycloak
    sample --> ratelimit
    sample --> auditlog
```

## 2. Core Request Flow (Sequence View)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Router as WebFlux Router / Filter Chain
    participant Keycloak as Keycloak JWT Filter
    participant RateLimiter as Rate Limiter Filter
    participant Controller as UniversalCrudController
    participant Engine as CrudEngine
    participant Interceptor as CompositeCrudInterceptor (Audit, Custom)
    participant Service as CrudService
    participant Storage as CrudStorageProvider (JPA/InMemory/Weaviate)
    
    Client->>Router: HTTP Request (e.g. POST /api/products)
    Router->>Keycloak: Extract & Validate JWT
    alt Auth Failed
        Keycloak-->>Client: 401 Unauthorized
    end
    Keycloak->>RateLimiter: Check Rate Limits
    alt Limit Exceeded
        RateLimiter-->>Client: 429 Too Many Requests
    end
    RateLimiter->>Controller: Route to handler
    Controller->>Engine: Look up Metadata & Service
    Engine-->>Controller: Return ResourceMetadata
    Controller->>Interceptor: preCreate(entity)
    Interceptor->>Service: create(entity)
    Service->>Storage: save(entity)
    Storage-->>Service: Saved Entity
    Service-->>Interceptor: Return Entity
    Interceptor->>Interceptor: postCreate(entity) (Audit logs change)
    Interceptor-->>Controller: Return Final DTO
    Controller-->>Client: HTTP 201 Created
```

## 3. Class Hierarchy and SPI Architecture

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +String id
        +String tenantId
        +Instant createdAt
        +Instant updatedAt
    }

    class CrudStorageProvider {
        <<interface>>
        +save(T) T
        +findById(String) Optional
        +findAll() List
        +deleteById(String) void
    }

    class CrudStorageProviderFactory {
        <<interface>>
        +supports(Class) boolean
        +getStorageProvider(Class) CrudStorageProvider
    }

    class CrudService {
        <<abstract>>
        #getStorageProvider() CrudStorageProvider
        +create(T) T
        +update(T) T
        +delete(String) void
    }

    class CrudInterceptor {
        <<interface>>
        +preCreate(T) void
        +postCreate(T) void
        +preUpdate(T) void
        +postUpdate(T) void
        +preDelete(String) void
        +postDelete(String) void
    }

    class CrudEngine {
        -Map~String, ResourceMetadata~ resources
        +registerResource(Class) void
        +getMetadata(String) ResourceMetadata
    }

    CrudService --> CrudStorageProvider : delegating
    CrudStorageProviderFactory --> CrudStorageProvider : builds
    CrudEngine --> CrudStorageProviderFactory : discovers via SPI
    CrudEngine --> CrudService : registers
    CrudEngine --> CrudInterceptor : wraps
```
