# Frontend UX and Architecture Diagrams

This document outlines the planned architecture and interaction flows for the React TypeScript frontend.

## 1. System Architecture (Component Diagram)

This diagram illustrates the high-level architecture, showing how the frontend interacts with various backend submodules and external services.

```mermaid
graph TD
    subgraph Frontend["React TypeScript Frontend"]
        App["App Container"]
        AuthCtx["Auth Context (Keycloak)"]
        APITester["API Tester UI"]
        IconGallery["Icon Gallery (MinIO)"]
    end

    subgraph Backend["Backend API Gateway / Submodules"]
        TranslationsAPI["/api/translations (Public)"]
        ProductsAPI["/api/products (Secure)"]
    end

    subgraph Infrastructure["Infrastructure & Data Services"]
        Keycloak["Keycloak (Identity Provider)"]
        MongoDB["MongoDB (Translations Data)"]
        PostgreSQL["PostgreSQL / JPA (Products Data)"]
        MinIO["MinIO (Icon Storage)"]
    end

    App --> AuthCtx
    App --> APITester
    App --> IconGallery

    AuthCtx -->|Authenticate| Keycloak
    
    APITester -->|GET public data| TranslationsAPI
    APITester -->|GET secure data (w/ Token)| ProductsAPI
    
    IconGallery -->|Fetch static icons| MinIO
    
    TranslationsAPI --> MongoDB
    ProductsAPI --> PostgreSQL
```

## 2. API Tester Sequence Diagram

This sequence diagram details the flow of user interactions within the API tester, specifically handling public and secure API calls, along with authentication.

```mermaid
sequenceDiagram
    actor User
    participant UI as API Tester UI
    participant Keycloak as Keycloak Auth
    participant PublicAPI as Translations API (MongoDB)
    participant SecureAPI as Products API (JPA)

    User->>UI: Request Public Translations
    UI->>PublicAPI: GET /api/translations
    PublicAPI-->>UI: 200 OK (Translations Data)
    UI-->>User: Display Translations

    User->>UI: Request Secure Products
    alt Not Authenticated
        UI->>SecureAPI: GET /api/products (No Token)
        SecureAPI-->>UI: 401 Unauthorized
        UI-->>User: Prompt Login
        User->>UI: Click Login
        UI->>Keycloak: Redirect to Login
        Keycloak-->>UI: Return JWT Token
    end
    
    UI->>SecureAPI: GET /api/products (Bearer Token)
    SecureAPI-->>UI: 200 OK (Products Data)
    UI-->>User: Display Products
```

## 3. Class/Component Structure (Frontend)

```mermaid
classDiagram
    class App {
        +render()
    }
    class AuthProvider {
        +login()
        +logout()
        +getToken()
    }
    class APITesterView {
        -fetchTranslations()
        -fetchProducts()
    }
    class IconGalleryView {
        -fetchIconsFromMinIO()
    }
    class ApiClient {
        +get(url, config)
        +post(url, data, config)
    }

    App *-- AuthProvider
    App *-- APITesterView
    App *-- IconGalleryView
    APITesterView --> ApiClient : uses
    IconGalleryView --> ApiClient : uses
```

## 4. Deployment Architecture

```mermaid
graph LR
    UserNode((User Browser))
    
    subgraph WebServer ["Web Server (e.g., Nginx)"]
        ReactApp["React SPA (Static Files)"]
    end
    
    subgraph BackendServices ["Backend Docker Network"]
        Gateway["API Gateway / Backend App"]
        KeycloakServer["Keycloak"]
        MinIOServer["MinIO Server"]
    end
    
    UserNode -->|HTTPS| ReactApp
    ReactApp -->|REST API| Gateway
    ReactApp -->|Auth Flow| KeycloakServer
    ReactApp -->|Fetch Icons| MinIOServer
```
