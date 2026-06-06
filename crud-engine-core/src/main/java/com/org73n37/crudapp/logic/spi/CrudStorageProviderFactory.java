package com.org73n37.crudapp.logic.spi;

import com.org73n37.crudapp.data.core.BaseEntity;

public interface CrudStorageProviderFactory {
    boolean supports(Class<? extends BaseEntity> entityClass);
    <T extends BaseEntity> CrudStorageProvider<T> getStorageProvider(Class<T> entityClass);
}
