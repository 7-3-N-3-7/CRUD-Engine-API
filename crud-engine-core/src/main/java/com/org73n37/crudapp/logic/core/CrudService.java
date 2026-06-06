package com.org73n37.crudapp.logic.core;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import java.util.List;
import java.util.Optional;

public abstract class CrudService<T extends BaseEntity> {

    protected abstract CrudStorageProvider<T> getStorageProvider();

    public List<T> findAll() {
        return getStorageProvider().findAll();
    }

    public Page<T> findAll(int page, int size) {
        return getStorageProvider().findAll(page * size, size, null, null, null);
    }

    public Page<T> findAll(int page, int size, java.util.Map<String, List<String>> queryParams, String sortParam, Class<?> dtoClass) {
        return getStorageProvider().findAll(page * size, size, queryParams, sortParam, dtoClass);
    }

    public Optional<T> findById(Long id) {
        return getStorageProvider().findById(id);
    }

    public T save(T entity) {
        return getStorageProvider().save(entity);
    }

    public void deleteById(Long id) {
        getStorageProvider().deleteById(id);
    }

    public T update(Long id, T entity) {
        if (!getStorageProvider().existsById(id)) {
            throw new com.org73n37.crudapp.api.errors.ResourceNotFoundException("Entity not found with id: " + id);
        }
        entity.setId(id);
        return getStorageProvider().save(entity);
    }

    public static class Page<T> {
        private final List<T> content;
        private final long totalElements;

        public Page(List<T> content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }

        public List<T> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }
}
