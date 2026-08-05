# Spring Boot Starter Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the Spring Boot auto-configuration starter (`crud-engine-spring-boot-starter`).

## 1. Class Structure

```mermaid
classDiagram
    class CrudEngineAutoConfiguration {
    }
    note for CrudEngineAutoConfiguration "Triggers ComponentScan on:\n- com.org73n37.crudapp.logic\n- com.org73n37.crudapp.api\n- com.org73n37.crudapp.infrastructure.security\n- com.org73n37.crudapp.infrastructure.web"
```

## 2. Bootstrapping Flow

```mermaid
graph TD
    A[Spring Boot starts up] --> B[Finds spring.factories/AutoConfiguration]
    B --> C[Instantiates CrudEngineAutoConfiguration]
    C --> D[Runs ComponentScan on registered packages]
    D --> E[Instantiates Core Engine & WebFlux controllers]
    E --> F[Injects dependency factories SPI]
    F --> G[Context Loaded Successfully]
```
