package com.org73n37.crudapp.logic.core;

import com.org73n37.crudapp.data.core.BaseEntity;
import java.util.List;

public class CompositeCrudInterceptor<T extends BaseEntity> implements CrudInterceptor<T> {
    private final List<CrudInterceptor<T>> interceptors;

    public CompositeCrudInterceptor(List<CrudInterceptor<T>> interceptors) {
        this.interceptors = interceptors != null ? interceptors : List.of();
    }

    @Override
    public void beforeCreate(T entity) {
        interceptors.forEach(i -> i.beforeCreate(entity));
    }

    @Override
    public void afterCreate(T entity) {
        interceptors.forEach(i -> i.afterCreate(entity));
    }

    @Override
    public void beforeUpdate(T entity) {
        interceptors.forEach(i -> i.beforeUpdate(entity));
    }

    @Override
    public void afterUpdate(T entity) {
        interceptors.forEach(i -> i.afterUpdate(entity));
    }

    @Override
    public void beforeDelete(Long id) {
        interceptors.forEach(i -> i.beforeDelete(id));
    }

    @Override
    public void afterDelete(Long id) {
        interceptors.forEach(i -> i.afterDelete(id));
    }
}
