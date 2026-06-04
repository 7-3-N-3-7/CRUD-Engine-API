package com.example.crudapp.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * [DEVELOPER EXPERIENCE (DX) OPTIMIZATION]
 * Reusable Spring Boot Auto-Configuration to bootstrap the Dynamic CRUD engine components.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.example.crudapp.logic",
    "com.example.crudapp.api",
    "com.example.crudapp.infrastructure.security",
    "com.example.crudapp.infrastructure.web"
})
public class CrudEngineAutoConfiguration {
}
