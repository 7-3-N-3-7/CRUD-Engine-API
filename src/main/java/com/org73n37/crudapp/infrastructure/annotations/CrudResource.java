package com.org73n37.crudapp.infrastructure.annotations;

import com.org73n37.crudapp.logic.core.CrudService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an entity as a CRUD resource.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrudResource {
    String path();
    Class<?> dto();
    
    /**
     * 🏗️ ARCHITECTURE OPTIMIZATION: Custom Service
     * Allows developers to provide a specific Service implementation.
     */
    Class<? extends CrudService> service() default CrudService.class;

    /**
     * 🔒 SECURITY OPTIMIZATION: Role-Based Access Control
     * List of roles required to access this resource.
     */
    String[] roles() default {"ANYONE", "USER", "ADMIN"};

    /**
     * 🚀 ARCHITECTURE OPTIMIZATION: DTO Versioning
     * Version identifier for routing and schema segregation.
     */
    String version() default "v1";
}
