# Developer Setup & Extension Manual: Dynamic CRUD Engine (v3.0)

Welcome to the **Dynamic CRUD Engine**. This project is a modular, metadata-driven REST engine combining a high-throughput **Spring WebFlux (Netty)** API layer with a pluggable **Service Provider Interface (SPI) Storage Layer** and decoupled security and filtering plugins.

This manual details how to manage the Git submodule workflow, register new resource endpoints for different databases, implement custom logic hooks, and deploy the application.

---

## 1. Git Submodule Workflow & Multi-Module Layout

The project is structured as 1 parent shell repository coordinating 8 independent Git submodules. 

### How to Clone the Project
Because the modules are hosted in separate repositories, you must clone recursively to fetch all files:
```bash
git clone --recursive https://github.com/73N37/Crud_application.git
```
If you have already cloned the repository without the `--recursive` flag, run:
```bash
git submodule update --init --recursive
```

### Developing Inside Submodules
When editing files inside a submodule (such as `crud-engine-core`):
1.  Navigate into the submodule directory: `cd crud-engine-core`
2.  Make your code edits.
3.  Commit and push directly inside the submodule:
    ```bash
    git add .
    git commit -m "feat: updated core capabilities"
    git push origin main
    ```
4.  Navigate back to the parent directory: `cd ..`
5.  Pin the new submodule commit in the parent repository:
    ```bash
    git add crud-engine-core
    git commit -m "chore: pin crud-engine-core to latest commit"
    git push
    ```

---

## 2. Pluggable Storage Layer (SPI)

The engine delegates database operations to a `CrudStorageProvider<T>` resolved at runtime by matching `CrudStorageProviderFactory` implementations. The framework supports three persistence layers out of the box:

### A. SQL/JPA Storage (`crud-engine-jpa`)
*   **Trigger**: Any entity annotated with `jakarta.persistence.Entity` is managed by `JpaStorageProviderFactory` and persists to PostgreSQL using Hibernate.
*   **Multi-Tenancy**: Secured using Row-Level Security (RLS) policies at the PostgreSQL database level.

### B. MongoDB Storage (`crud-engine-mongodb`)
*   **Trigger**: Any entity annotated with `org.springframework.data.mongodb.core.mapping.Document` is managed by `MongoStorageProviderFactory` and persists to MongoDB using `MongoTemplate`.
*   **Tenancy & Filtering**: Filters collections dynamically in memory/queries using tenant claims and regex-based criteria.

### C. In-Memory Storage (`crud-engine-inmemory`)
*   **Trigger**: Used as a fallback if no database annotations are detected, or for unit testing.
*   **Mechanism**: A thread-safe, concurrent hash map segregated by tenant id.

---

## 3. Registering a New Resource Entity

Adding a new resource (e.g. `Device`) is as simple as defining the class, mapping it to a database structure, and creating a DTO record.

### Option A: Creating a JPA Resource
Create the entity in the sample application (`src/main/java/com/org73n37/crudapp/data/Device.java`):
```java
package com.org73n37.crudapp.data;

import com.org73n37.crudapp.api.records.DeviceRecord;
import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
@CrudResource(path = "devices", dto = DeviceRecord.class, roles = {"ADMIN", "USER"})
public class Device extends BaseEntity {
    @Column(nullable = false)
    private String modelName;

    public Device() {}
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
```

### Option B: Creating a MongoDB Resource
Create the document in the sample application:
```java
package com.org73n37.crudapp.data;

import com.org73n37.crudapp.api.records.DeviceRecord;
import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "devices")
@CrudResource(path = "devices", dto = DeviceRecord.class, roles = {"ADMIN", "USER"})
public class Device extends BaseEntity {
    private String modelName;

    public Device() {}
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
```

### Creating the DTO Record
Create the corresponding record mapping (`src/main/java/com/org73n37/crudapp/api/records/DeviceRecord.java`):
```java
package com.org73n37.crudapp.api.records;

import com.org73n37.crudapp.data.Device;
import com.org73n37.crudapp.infrastructure.annotations.EntityMapping;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@EntityMapping(entity = Device.class)
public record DeviceRecord(
    @NotBlank(message = "Model name cannot be blank")
    String modelName,
    Map<String, String> attributes
) {}
```

---

## 4. Custom Hook Interceptors

Interceptors allow you to run business logic around database actions.

### Entity-Specific Interceptor
To run rules for a specific resource type, create a class implementing `CrudInterceptor<T>`:
```java
package com.org73n37.crudapp.domain.device;

import com.org73n37.crudapp.data.Device;
import com.org73n37.crudapp.logic.core.CrudInterceptor;
import org.springframework.stereotype.Component;

@Component
public class DeviceInterceptor implements CrudInterceptor<Device> {
    @Override
    public void beforeCreate(Device entity) {
        entity.setModelName(entity.getModelName().toUpperCase());
    }
}
```

### Global/Assignable Interceptor
To apply an interceptor to **all resources** extending a common parent, register it against the parent class (e.g. `BaseEntity`). `CrudEngine` resolves and merges assignable interceptors automatically:
```java
package com.org73n37.crudapp.logic.auditlog;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.core.CrudInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLoggingInterceptor implements CrudInterceptor<BaseEntity> {
    private static final Logger log = LoggerFactory.getLogger(AuditLoggingInterceptor.class);

    @Override
    public void beforeCreate(BaseEntity entity) {
        log.info("[AUDIT] Creating resource: {} for tenant {}", entity.getClass().getSimpleName(), entity.getTenantId());
    }
}
```

---

## 5. Local Infrastructure Configuration Setup

The application dependencies (PostgreSQL, MongoDB, Keycloak) run inside Docker containers.

### Step 1: Spin up Containers
```bash
docker-compose up -d
```
*   **PostgreSQL:** Runs on port `5433` (DB: `cruddb`, Username: `user`, Password: `password`).
*   **Keycloak:** Runs on port `8081` (Admin Credentials: `admin`/`admin`).

### Step 2: Configure Keycloak Realm
1.  Navigate to `http://localhost:8081/admin` and log in.
2.  Create a realm named `crud-realm`.
3.  Create roles: `ADMIN`, `USER`, `GUEST`.
4.  Create user `test-user`, assign credentials and mapping roles.

---

## 6. Hostinger VPS Deployment

Because the application runs Java 25, Keycloak, and PostgreSQL as persistent daemon processes, **Hostinger Shared Web Hosting plans cannot run this stack**. You must deploy on a **Hostinger VPS Plan** running Ubuntu 22.04 LTS or 24.04 LTS.

### Option A: Native Deployment (systemd)
1.  **Install Java Runtime (JRE 25) on your VPS:**
    ```bash
    wget https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25%2B9/OpenJDK25U-jre_x64_linux_hotspot_25_9.tar.gz
    tar -xvf OpenJDK25U-jre_x64_linux_hotspot_25_9.tar.gz
    sudo mv jdk-25* /usr/lib/jvm/java-25-openjdk
    sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-25-openjdk/bin/java 1
    ```
2.  **Package the application:**
    ```bash
    mvn clean package
    ```
3.  **Upload the Uber-JAR to the VPS:**
    ```bash
    scp crud-app-sample/target/crud-app-sample-0.0.1-SNAPSHOT.jar root@YOUR_VPS_IP:/var/www/crudapp.jar
    ```
4.  **Create a systemd Service:**
    ```bash
    sudo nano /etc/systemd/system/crudapp.service
    ```
    Paste the configuration details:
    ```ini
    [Unit]
    Description=Dynamic CRUD Application
    After=network.target

    [Service]
    User=root
    WorkingDirectory=/var/www
    ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod -Dserver.port=8080 -Dspring.datasource.url=jdbc:postgresql://localhost:5432/cruddb -Dspring.datasource.username=dbuser -Dspring.datasource.password=secure_vps_password -Dkeycloak.jwk-set-uri=http://localhost:8081/realms/crud-realm/protocol/openid-connect/certs -Dkeycloak.issuer=http://localhost:8081/realms/crud-realm /var/www/crudapp.jar
    Restart=always

    [Install]
    WantedBy=multi-user.target
    ```
5.  **Start the Service:**
    ```bash
    sudo systemctl daemon-reload
    sudo systemctl enable crudapp --now
    ```

### Option B: Reverse Proxy Routing (Nginx)
Install Nginx to secure the API behind port `80`/`443` and configure SSL certificates:
```bash
sudo apt install nginx python3-certbot-nginx -y
```
Create a site configuration under `/etc/nginx/sites-available/crudapp`:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
Link the config and restart Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/crudapp /etc/nginx/sites-enabled/
sudo systemctl restart nginx
sudo certbot --nginx -d yourdomain.com
```
