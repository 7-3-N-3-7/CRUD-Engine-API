package com.org73n37.crudapp.logic.spi;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.core.CrudService.Page;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public interface CrudStorageProvider<T extends BaseEntity> {
    List<T> findAll();
    Page<T> findAll(int offset, int limit, Map<String, List<String>> queryParams, String sortParam, Class<?> dtoClass);
    Optional<T> findById(Long id);
    T save(T entity);
    void deleteById(Long id);
    boolean existsById(Long id);
    long count();
}
