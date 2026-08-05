# Core Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the core module (`crud-engine-core`).

## 1. Class Structure

```mermaid
classDiagram
    class CrudEngine {
        -Map~String, ResourceMetadata~ resources
        +init() void
        +registerResource(Class) void
        +getMetadata(String) ResourceMetadata
    }

    class ResourceMetadata {
        +Class entityClass
        +Class dtoClass
        +String basePath
        +int version
        +CrudStorageProvider repository
        +CrudService service
        +CrudInterceptor interceptor
    }

    class ServiceRegistry {
        -Map~Class, CrudService~ registry
        +getService(Class, CrudService) CrudService
    }

    CrudEngine --> ResourceMetadata : manages
    CrudEngine --> ServiceRegistry : uses
```

## 2. Dynamic Initialization Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Idle : App Starts
    Idle --> Scanning : @PostConstruct init()
    Scanning --> Validation : Candidate Class Path Scanning
    Validation --> ResolveStorage : Validate @EntityMapping Bidirectional Consistency
    ResolveStorage --> ResolveService : Query SPI factories for match
    ResolveService --> WrapInterceptors : Query ServiceRegistry
    WrapInterceptors --> Registered : Wrap matched interceptors in Composite Chain
    Registered --> Idle : Registered in resources map
```
