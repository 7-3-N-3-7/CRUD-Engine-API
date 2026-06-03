# Developer Setup & Extension Manual: Dynamic CRUD Engine

Welcome to the **Dynamic CRUD Engine**. This project is a metadata-driven, enterprise-grade REST engine combining the data safety and persistence power of **Spring Boot (Spring Data JPA)** with the lightweight, performant web server capabilities of **Javalin**. 

This manual details how to set up the infrastructure, register new resource endpoints, extend core behaviors, and implement custom logic hooks.

---

## 1. Local Infrastructure Configuration Setup

The application depends on **PostgreSQL** (for persistence) and **Keycloak** (for JWT authentication and Role-Based Access Control).

### Step 1: Spin up Containers
Run the following command at the project root to spin up the required PostgreSQL and Keycloak instances:
```bash
docker-compose up -d
```
*   **PostgreSQL:** Runs on port `5433` (DB: `cruddb`, Username: `user`, Password: `password`).
*   **Keycloak:** Runs on port `8081` (Admin Credentials: `admin`/`admin`).

### Step 2: Configure Keycloak Admin Console
1.  Navigate to `http://localhost:8081/admin` and log in.
2.  Create a realm named `crud-realm`.
3.  Create roles: `ADMIN`, `USER`, `GUEST`.
4.  Create client `crud-client` or configure users and roles.
5.  Generate a JWT containing the user identity, roles list under `realm_access.roles`, and optionally a custom `tenant` claim (e.g. `tenant-a`).

### Step 3: Application Configuration properties
Review `src/main/resources/application.properties` to align settings:
```properties
server.port=8080

# Database Connectivity (linked to Docker PostgreSQL instance)
spring.datasource.url=jdbc:postgresql://localhost:5433/cruddb
spring.datasource.username=user
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA validation and formatting
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Keycloak Token Signature verification certs
keycloak.jwk-set-uri=http://localhost:8081/realms/crud-realm/protocol/openid-connect/certs
keycloak.issuer=http://localhost:8081/realms/crud-realm
```

---

## 2. Registering a New Resource Entity

The CRUD engine registers resource endpoints dynamically at startup by scanning JPA entities. Follow this guide to add a new resource endpoint (e.g., `Device`).

### Step 1: Create the JPA Entity
Your entity class must extend `BaseEntity` and be annotated with `@Entity` and `@CrudResource`.

Create `src/main/java/com/example/crudapp/data/Device.java`:
```java
package com.example.crudapp.data;

import com.example.crudapp.api.records.DeviceRecord;
import com.example.crudapp.data.core.BaseEntity;
import com.example.crudapp.infrastructure.annotations.CrudResource;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
@CrudResource(
    path = "devices", 
    dto = DeviceRecord.class, 
    roles = {"ADMIN", "USER"} // RBAC Roles allowed to query/write to this resource
)
public class Device extends BaseEntity {

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String serialNumber;

    // Default constructor is required by Hibernate
    public Device() {}

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
}
```

#### Why extending `BaseEntity` matters:
*   **JPA Auditing:** Automatically manages database audit logging fields (`created_by`, `created_date`, `last_modified_by`, `last_modified_date`).
*   **Optimistic Version Locking:** Maps a `@Version` field to prevent simultaneous transactional overwrites (prevents dirty writes).
*   **Multi-Tenancy Segregation:** Implements a logical `tenantId` field to separate and restrict read/write boundaries automatically.
*   **Dynamic Attributes:** Inherits an `@ElementCollection` map to let users post custom runtime fields not defined in the schema.

---

## 3. Creating and Mapping the DTO Record

To protect the internal database layout, client endpoints accept and return DTO records rather than raw database entities.

### Step 1: Create the Java Record
Create `src/main/java/com/example/crudapp/api/records/DeviceRecord.java`:
```java
package com.example.crudapp.api.records;

import com.example.crudapp.data.Device;
import com.example.crudapp.infrastructure.annotations.EntityMapping;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@EntityMapping(entity = Device.class) // Maps this DTO Record back to the JPA Entity
public record DeviceRecord(
    @NotBlank(message = "Model name cannot be blank")
    String modelName,

    @NotBlank(message = "Serial number cannot be blank")
    String serialNumber,

    Map<String, String> attributes // Capture dynamic custom attributes
) {}
```

#### Why `@EntityMapping` matters:
*   **Bidirectional Mapping Validation:** During application startup, `DynamicCrudManager` asserts that the entity's `@CrudResource(dto = ...)` matches the DTO's `@EntityMapping(entity = ...)` annotation. If they do not match, the application immediately throws a validation exception and halts execution. This prevents developers from deploying mismatching or unmapped configurations to production.
*   **Input Validation:** Enforces JSR-380 input validations (e.g. `@NotBlank`, `@Min`, etc.). Violations are caught and mapped directly to unified JSON error payloads (status `400`).

---

## 4. Custom Hook Interceptors

Sometimes you need to validate business logic, format fields, or log specific audits before an entity is written to or deleted from the database. This is achieved by creating an interceptor.

### Step 1: Create the Interceptor
Create a class implementing `CrudInterceptor<T>`.

Create `src/main/java/com/example/crudapp/domain/device/DeviceInterceptor.java`:
```java
package com.example.crudapp.domain.device;

import com.example.crudapp.data.Device;
import com.example.crudapp.logic.core.CrudInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceInterceptor implements CrudInterceptor<Device> {
    private static final Logger log = LoggerFactory.getLogger(DeviceInterceptor.class);

    @Override
    public void beforeCreate(Device entity) {
        log.info("Processing device creation hook for Model: {}", entity.getModelName());
        // Capitalize the serial number format
        if (entity.getSerialNumber() != null) {
            entity.setSerialNumber(entity.getSerialNumber().toUpperCase());
        }
    }

    @Override
    public void afterCreate(Device entity) {
        log.info("Device registered with ID: {}", entity.getId());
    }

    @Override
    public void beforeUpdate(Device entity) {
        log.info("Pre-update hook executed for device ID: {}", entity.getId());
    }

    @Override
    public void afterUpdate(Device entity) {
        log.info("Device updated: {}", entity.getId());
    }

    @Override
    public void beforeDelete(Long id) {
        log.info("Pre-delete check executing for Device: {}", id);
    }

    @Override
    public void afterDelete(Long id) {
        log.info("Device removed: {}", id);
    }
}
```

#### Why interceptors matter:
*   Instead of writing custom controllers and routing tables for every single resource, you write simple interceptors. The core engine detects these interceptors automatically and registers them to run around JDBC persist operations, keeping the CRUD engine extremely modular.

---

## 5. Liquibase Database Migrations

Every schema adjustment (creating tables, altering columns) must be configured in Liquibase changesets to enable automated deployment pipelines.

Add a changeset to `src/main/resources/db/changelog/db.changelog-master.xml`:
```xml
<changeSet id="5" author="developer">
    <!-- Device Entity Table -->
    <createTable tableName="device">
        <column name="id" type="bigint">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="model_name" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
        <column name="serial_number" type="varchar(255)">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <!-- Map device ID reference back to baseentity primary key -->
    <addForeignKeyConstraint baseColumnNames="id"
                             baseTableName="device"
                             constraintName="fk_device_baseentity"
                             referencedColumnNames="id"
                             referencedTableName="baseentity"
                             onDelete="CASCADE"/>
</changeSet>
```

---

## 6. Core Enterprise Mechanisms

Understanding how the background engine handles requests is essential for modifying or debugging.

### Request Flow Diagram
```
Client -> HTTP Headers -> TracingFilter (Generates Request-ID)
   -> JwtInterceptor (Validates Token, populates TenantContext + Spring Security Context)
   -> CRUD Resource mapping checks Role privileges (RBAC)
   -> DTO Mapper maps payload to Entity
   -> Business Interceptors execute Hooks (beforeCreate / beforeUpdate)
   -> BaseEntity Persist (Populates Auditing, TenantID, Revision Version)
   -> Database Write
   -> Business Interceptors execute Hooks (afterCreate / afterUpdate)
   -> Response (JSON payload + X-Request-ID Header)
   -> AFTER Filter (Flushes MDC, ThreadLocal TenantContext, SecurityContext to prevent leaks)
```

### Key Components

*   **Multi-Tenancy Segregation:** `GenericRepository` dynamically injects `WHERE tenantId = :tenantId` filter criteria on queries. The active tenant is extracted from the client's validated JWT token during the `before` filter.
*   **Request Diagnostics:** If a bug occurs, search the logs using the correlation token (`X-Request-ID`). This token is automatically propagated inside SLF4J MDC logs for simple trace aggregation.
*   **Diagnostics endpoints:**
    *   `/api-docs` returns dynamic, metadata-generated OpenAPI 3.0 specs.
    *   `/swagger-ui` renders a graphical web console mapped to client-defined routes.
    *   `/health/readiness` and `/health/liveness` provide diagnostics verification (useful for Kubernetes cluster deployment checks).
