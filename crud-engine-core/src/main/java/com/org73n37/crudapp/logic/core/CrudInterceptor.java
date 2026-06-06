package com.org73n37.crudapp.logic.core;

import com.org73n37.crudapp.data.core.BaseEntity;

/**
 * [LOGIC LAYER]
 * Generic interceptor for custom business logic hooks.
 * @param <T> The entity type this interceptor handles.
 */
public interface CrudInterceptor<T extends BaseEntity> {
    default void beforeCreate(T entity) {}
    default void afterCreate(T entity) {}
    default void beforeUpdate(T entity) {}
    default void afterUpdate(T entity) {}
    default void beforeDelete(Long id) {}
    default void afterDelete(Long id) {}
}
