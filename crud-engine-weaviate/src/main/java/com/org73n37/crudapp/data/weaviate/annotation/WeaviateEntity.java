package com.org73n37.crudapp.data.weaviate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark entities that should be persisted in Weaviate.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WeaviateEntity {
    /**
     * The collection (class) name in Weaviate. If empty, the simple class name is used.
     */
    String value() default "";
}
