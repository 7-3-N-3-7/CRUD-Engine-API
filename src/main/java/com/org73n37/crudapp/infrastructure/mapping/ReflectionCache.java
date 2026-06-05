package com.org73n37.crudapp.infrastructure.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [PERFORMANCE OPTIMIZATION]
 * Thread-safe cache for reflection metadata (fields, constructors, annotations)
 * to avoid high-volume JVM lookup overhead.
 */
public final class ReflectionCache {

    private static final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
    private static final Map<Field, Annotation[]> fieldAnnotationCache = new ConcurrentHashMap<>();

    private ReflectionCache() {}

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, Class::getDeclaredFields);
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getNoArgsConstructor(Class<T> clazz) {
        return (Constructor<T>) constructorCache.computeIfAbsent(clazz, cl -> {
            try {
                Constructor<?> ctor = cl.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor;
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("No-args constructor missing for " + cl.getName(), e);
            }
        });
    }

    public static Annotation[] getAnnotations(Field field) {
        return fieldAnnotationCache.computeIfAbsent(field, Field::getAnnotations);
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Field field, Class<A> annotationClass) {
        for (Annotation ann : getAnnotations(field)) {
            if (ann.annotationType().equals(annotationClass)) {
                return (A) ann;
            }
        }
        return null;
    }
}
