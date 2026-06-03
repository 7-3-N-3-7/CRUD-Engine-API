package com.example.crudapp.logic;

import com.example.crudapp.data.core.BaseEntity;
import com.example.crudapp.infrastructure.annotations.CrudResource;
import com.example.crudapp.infrastructure.annotations.Parent;
import com.example.crudapp.infrastructure.mapping.MappingCache;
import tools.jackson.databind.ObjectMapper;
import com.example.crudapp.infrastructure.mapping.MappingCache.RecordMapping;


import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * [LOGIC LAYER]
 * Decoupled mapping service responsible for Entity <-> Record conversions.
 * Optimized with MethodHandles for high performance.
 */
public class MappingService {

    private final ObjectMapper objectMapper;

    public MappingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public <R extends Record> R toRecord(BaseEntity entity) {
        Class<?> entityClass = entity.getClass();
        if (!entityClass.isAnnotationPresent(CrudResource.class)) {
            throw new RuntimeException("No @CrudResource annotation present on " + entityClass.getName());
        }

        Class<R> recordClass = (Class<R>) entityClass.getAnnotation(CrudResource.class).dto();
        RecordMapping<R> mapping = MappingCache.get(recordClass);
        
        String[] names = mapping.getParameterNames();
        Object[] values = new Object[names.length];

        for (int i = 0; i < names.length; i++) {
            values[i] = resolveValue(entity, names[i]);
        }

        try {
            // ⚡ Optimized invocation using MethodHandle
            return (R) mapping.getConstructorHandle().invokeWithArguments(values);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to instantiate record via MethodHandle: " + recordClass.getName(), e);
        }
    }

    public <R extends Record> R mapToRecord(Map<String, Object> data, Class<R> recordClass) {
        return objectMapper.convertValue(data, recordClass);
    }

    private Object resolveValue(BaseEntity entity, String componentName) {
        if ("parentId".equals(componentName)) {
            return findAnnotatedField(entity.getClass(), Parent.class)
                    .map(field -> {
                        try {
                            field.setAccessible(true);
                            BaseEntity p = (BaseEntity) field.get(entity);
                            return p != null ? p.getId() : null;
                        } catch (IllegalAccessException e) {
                            return null;
                        }
                    }).orElse(null);
        }

        if ("grandparentId".equals(componentName)) {
            return entity.getGrandparent().map(BaseEntity::getId).orElse(null);
        }

        try {
            Field field = findField(entity.getClass(), componentName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<Field> findAnnotatedField(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotation) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    return Optional.of(field);
                }
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }
}
