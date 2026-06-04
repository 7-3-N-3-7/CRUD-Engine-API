package com.example.crudapp.logic.core;

import com.example.crudapp.data.core.BaseEntity;
import com.example.crudapp.data.core.CrudRepository;
import java.util.List;
import java.util.Optional;

public abstract class CrudService<T extends BaseEntity> {

    protected abstract CrudRepository<T> getRepository();

    public List<T> findAll() {
        return getRepository().findAll();
    }

    public Page<T> findAll(int page, int size) {
        List<T> content = getRepository().findAll(page * size, size);
        long total = getRepository().count();
        return new Page<>(content, total);
    }

    public Page<T> findAll(int page, int size, java.util.Map<String, List<String>> queryParams, String sortParam, Class<?> dtoClass) {
        List<T> content = getRepository().findAll(page * size, size, queryParams, sortParam, dtoClass);
        long total = getRepository().count();
        return new Page<>(content, total);
    }

    public Optional<T> findById(Long id) {
        return getRepository().findById(id);
    }

    public T save(T entity) {
        return getRepository().save(entity);
    }

    public void deleteById(Long id) {
        getRepository().deleteById(id);
    }

    public T update(Long id, T entity) {
        if (!getRepository().existsById(id)) {
            throw new com.example.crudapp.api.errors.ResourceNotFoundException("Entity not found with id: " + id);
        }
        entity.setId(id);
        return getRepository().save(entity);
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
