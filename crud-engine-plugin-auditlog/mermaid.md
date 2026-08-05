# Audit Log Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the Audit Log plugin (`crud-engine-plugin-auditlog`).

## 1. Class Structure

```mermaid
classDiagram
    class CrudInterceptor~T~ {
        <<interface>>
        +beforeCreate(T) void
        +afterCreate(T) void
        +beforeUpdate(T) void
        +afterUpdate(T) void
        +beforeDelete(Long) void
        +afterDelete(Long) void
    }

    class AuditLoggingInterceptor {
        -log : Logger
        +beforeCreate(BaseEntity) void
        +afterCreate(BaseEntity) void
        +beforeUpdate(BaseEntity) void
        +afterUpdate(BaseEntity) void
        +beforeDelete(Long) void
        +afterDelete(Long) void
    }

    AuditLoggingInterceptor ..|> CrudInterceptor : implements
```

## 2. Dynamic Interception Flow

```mermaid
graph TD
    A[CrudService Operation] --> B{Operation Stage}
    B -- pre-save --> C[Invoke beforeCreate / beforeUpdate]
    C --> D[Log Entity snapshot to Console]
    B -- post-save --> E[Invoke afterCreate / afterUpdate]
    E --> F[Log Saved Entity ID snapshot to Console]
    B -- pre-delete --> G[Invoke beforeDelete]
    G --> H[Log Target ID details to Console]
```
