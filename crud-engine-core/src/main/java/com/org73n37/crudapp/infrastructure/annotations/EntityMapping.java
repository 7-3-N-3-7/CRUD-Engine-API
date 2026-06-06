package com.org73n37.crudapp.infrastructure.annotations;

import com.org73n37.crudapp.data.core.BaseEntity;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to map a DTO (Record) back to its JPA Database Entity class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityMapping {
    Class<? extends BaseEntity> entity();
}
