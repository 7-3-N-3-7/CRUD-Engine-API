# Weaviate Storage Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the Weaviate storage module (`crud-engine-weaviate`).

## 1. Class Structure

```mermaid
classDiagram
    class WeaviateStorageProvider~T~ {
        -Class~T~ entityClass
        -WeaviateClient client
        -String collectionName
        -JsonMapper objectMapper
        -AtomicLong idSequence
        +save(T) T
        +findById(Long) Optional~T~
        +findAll() List~T~
        +deleteById(Long) void
        -ensureCollectionExists() void
        -initializeSequence() void
        -getUuidFromLong(Long) UUID
    }

    class WeaviateStorageProviderFactory {
        -WeaviateClient client
        +supports(Class) boolean
        +getStorageProvider(Class) CrudStorageProvider
    }

    WeaviateStorageProviderFactory --> WeaviateStorageProvider : creates
```

## 2. Deterministic UUID Generator Flow

```mermaid
graph TD
    start([Start generate UUID]) --> checkNull{Is numeric ID null?}
    checkNull -- Yes --> returnNull[Return null]
    checkNull -- No --> toString[Convert long to String]
    toString --> getBytes[Get UTF-8 bytes]
    getBytes --> nameUUID[UUID.nameUUIDFromBytes]
    nameUUID --> returnUUID([Return deterministic UUID])
```
