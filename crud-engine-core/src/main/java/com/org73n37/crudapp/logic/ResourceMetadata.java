package com.org73n37.crudapp.logic;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.core.CrudService;
import com.org73n37.crudapp.logic.core.CrudInterceptor;

import java.util.List;
import java.util.Map;

public class ResourceMetadata<T extends BaseEntity, D> {
    private final Class<T> entityClass;
    private final Class<D> dtoClass;
    private final String basePath;
    private final String version;
    private final CrudStorageProvider<T> repository;
    private final CrudService<T> service;
    private final CrudInterceptor<T> interceptor;
    private final List<FieldInfo> fields;

    private ResourceMetadata(Builder<T, D> builder) {
        this.entityClass = builder.entityClass;
        this.dtoClass = builder.dtoClass;
        this.basePath = builder.basePath;
        this.version = builder.version;
        this.repository = builder.repository;
        this.service = builder.service;
        this.interceptor = builder.interceptor;
        this.fields = builder.fields;
    }

    public static <T extends BaseEntity, D> Builder<T, D> builder() {
        return new Builder<>();
    }

    public Class<T> getEntityClass() { return entityClass; }
    public Class<D> getDtoClass() { return dtoClass; }
    public String getBasePath() { return basePath; }
    public String getVersion() { return version; }
    public CrudStorageProvider<T> getRepository() { return repository; }
    public CrudService<T> getService() { return service; }
    public CrudInterceptor<T> getInterceptor() { return interceptor; }
    public List<FieldInfo> getFields() { return fields; }

    public static class FieldInfo {
        private final String name;
        private final String type;
        private final boolean required;
        private final Map<String, Object> constraints;

        public FieldInfo(String name, String type, boolean required, Map<String, Object> constraints) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.constraints = constraints;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        public Map<String, Object> getConstraints() { return constraints; }
    }

    public static class Builder<T extends BaseEntity, D> {
        private Class<T> entityClass;
        private Class<D> dtoClass;
        private String basePath;
        private String version;
        private CrudStorageProvider<T> repository;
        private CrudService<T> service;
        private CrudInterceptor<T> interceptor;
        private List<FieldInfo> fields;

        public Builder<T, D> entityClass(Class<T> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        public Builder<T, D> dtoClass(Class<D> dtoClass) {
            this.dtoClass = dtoClass;
            return this;
        }

        public Builder<T, D> basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder<T, D> version(String version) {
            this.version = version;
            return this;
        }

        public Builder<T, D> repository(CrudStorageProvider<T> repository) {
            this.repository = repository;
            return this;
        }

        public Builder<T, D> service(CrudService<T> service) {
            this.service = service;
            return this;
        }

        public Builder<T, D> interceptor(CrudInterceptor<T> interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public Builder<T, D> fields(List<FieldInfo> fields) {
            this.fields = fields;
            return this;
        }

        public ResourceMetadata<T, D> build() {
            return new ResourceMetadata<>(this);
        }
    }
}
