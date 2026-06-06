package com.org73n37.crudapp.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * [DEVELOPER EXPERIENCE (DX) OPTIMIZATION]
 * Reusable Spring Boot Auto-Configuration to bootstrap the Dynamic CRUD engine components.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.org73n37.crudapp.logic",
    "com.org73n37.crudapp.api",
    "com.org73n37.crudapp.infrastructure.security",
    "com.org73n37.crudapp.infrastructure.web"
})
public class CrudEngineAutoConfiguration {
}
