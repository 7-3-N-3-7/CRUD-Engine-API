# Professional Zero-Code API Platform Documentation

## 📌 Project Overview
This project is an **Enterprise-Grade, Zero-Code API Engine**. It allows for the runtime creation of fully-featured CRUD APIs via a Database Registry. It is designed for multi-tenant, high-scale environments with a focus on security, performance, and traceability.

---

## 🚀 Professional Features (The "Big 6" Fixes)

### ⚡ 1. Ultra-Low Latency Caching
The `SchemaCacheService` uses an in-memory `ConcurrentHashMap` to store API definitions. This eliminates redundant database lookups for metadata on every request, providing sub-millisecond validation and security checks.

### ⛓️ 2. Inter-Entity Relationships
The system now supports **Relationships** between Zero-Code entities.
- By setting a field type to `RELATION` and specifying `relationTo` (e.g., "customers"), the `VirtualCrudService` will automatically validate that the referenced ID exists in the system before saving.

### 👤 3. Record-Level Ownership
Security has evolved from simple roles to **Identity-Based Ownership**.
- Every record in the `virtual_documents` table stores an `owner` (username).
- **Rules**: Only the record creator or a user with `ROLE_ADMIN` can `UPDATE` or `DELETE` a record. Regular users can no longer touch each other's data.

### 📑 4. Full Audit Trail
The `AuditLog` system captures every mutation in the system.
- **Traceability**: Tracks `Action` (CREATE/UPDATE/DELETE), `Resource`, `RecordID`, `User`, `OldData`, and `NewData`.
- **Compliance**: Provides a permanent history of system changes for professional auditing.

### 🔍 5. Persistent MySQL Architecture
The project has moved from volatile H2 to **Persistent MySQL**.
- Includes connection pooling (HikariCP) optimized for high concurrency.
- Schema definitions and data persist across server restarts.

### 📊 6. Intelligent Query Engine
The `GenericSpecification` has been upgraded to support meta-filtering (owner, id) and is structured to facilitate deep JSON querying in production environments.

---

## 🏗️ Technical Architecture

| Component | Responsibility |
| :--- | :--- |
| **Schema Factory** | `/api/meta/schemas` - Manage Entities at runtime. |
| **Zero-Code Proxy** | `/api/{resource}` - Unified entry point for dynamic data. |
| **Virtual Storage** | Single `virtual_documents` table using JSON blobs. |
| **Security Tier** | Deny-by-default RBAC enforced in `crud-engine-core` (`ReactiveJwtFilter`); roles such as ADMIN / USER must be granted explicitly, `ANYONE` opts a resource into public access. |

---

## 🚦 Getting Started

### 1. Start the Database
Ensure your MySQL instance is running (as defined in `docker-compose.yml`):
```bash
docker-compose up -d
```

### 2. Run the Application
```bash
mvn spring-boot:run
```

### 3. API Usage
- **Define a Schema**: `POST /api/meta/schemas` (Admin only).
- **Create Data**: `POST /api/v3/your-path`.
- **Audit Logs**: Inspect the `audit_logs` table to see the system history.

---

## 🔒 Default Credentials
- **Admin**: `admin` / `admin`
- **User**: `user` / `user`
- **Guest**: `guest` / `guest`
