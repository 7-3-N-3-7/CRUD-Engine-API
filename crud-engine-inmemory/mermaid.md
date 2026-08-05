# In-Memory Storage Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the in-memory storage module (`crud-engine-inmemory`).

## 1. Class Structure

```mermaid
classDiagram
    class CrudStorageProvider~T~ {
        <<interface>>
        +save(T) T
        +findById(Long) Optional~T~
        +findAll() List~T~
        +deleteById(Long) void
    }

    class InMemoryStorageProvider~T~ {
        -Map~String, Map~Long, T~~ db
        -AtomicLong idSequence
        -getActiveTenantId() String
        -getTenantStorage() Map~Long, T~
        +save(T) T
        +findById(Long) Optional~T~
        +findAll() List~T~
        +deleteById(Long) void
    }

    class InMemoryStorageProviderFactory {
        -Map~Class, CrudStorageProvider~ providers
        +supports(Class) boolean
        +getStorageProvider(Class) CrudStorageProvider
    }

    InMemoryStorageProvider ..|> CrudStorageProvider : implements
    InMemoryStorageProviderFactory ..|> CrudStorageProviderFactory : implements
```

## 2. In-Memory Tenant Lookup Sequence

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Provider as InMemoryStorageProvider
    participant TenantContext
    participant Map as DB Map
    
    Client->>Provider: findById(10)
    Provider->>TenantContext: getTenantId()
    TenantContext-->>Provider: "tenant_abc"
    Provider->>Map: get("tenant_abc")
    alt Tenant Map exists
        Map-->>Provider: tenantMap
    else Tenant Map absent
        Provider->>Map: computeIfAbsent("tenant_abc")
        Map-->>Provider: empty tenantMap
    end
    Provider->>Provider: tenantMap.get(10)
    Provider-->>Client: Optional~T~
```
