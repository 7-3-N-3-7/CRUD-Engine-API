# Sample App Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the sample application (`crud-app-sample`).

## 1. Class Structure

```mermaid
classDiagram
    class Main {
        +main(String[]) void
    }

    class Product {
        -String name
        -String description
        -Double price
        +getName() String
        +setName(String) void
        +getDescription() String
        +setDescription(String) void
        +getPrice() Double
        +setPrice(Double) void
    }

    class ProductRecord {
        <<record>>
        +String name
        +String description
        +Double price
        +Long parentId
        +Long grandparentId
        +Map~String, String~ attributes
    }

    class ProductInterceptor {
        +beforeCreate(Product) void
        +afterCreate(Product) void
    }

    class DashboardController {
    }

    ProductRecord ..> Product : maps to
    ProductInterceptor ..> Product : intercepts
```

## 2. Product Mutation Lifecycle Interceptor Flow

```mermaid
sequenceDiagram
    autonumber
    participant Controller as UniversalCrudController
    participant Interceptor as ProductInterceptor
    participant Service as CrudService
    
    Controller->>Interceptor: beforeCreate(product)
    Note over Interceptor: Capitalizes name if present
    Interceptor-->>Controller: product (uppercased)
    Controller->>Service: save(product)
    Service-->>Controller: savedProduct
    Controller->>Interceptor: afterCreate(savedProduct)
    Note over Interceptor: Logs success to console
    Interceptor-->>Controller: done
```
