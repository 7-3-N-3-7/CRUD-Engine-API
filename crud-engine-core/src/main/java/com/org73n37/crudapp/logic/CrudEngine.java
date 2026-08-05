package com.org73n37.crudapp.logic;

import com.org73n37.crudapp.data.core.BaseEntity;
import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import com.org73n37.crudapp.infrastructure.mapping.ReflectionCache;
import com.org73n37.crudapp.logic.core.CrudService;
import com.org73n37.crudapp.logic.core.CrudInterceptor;
import com.org73n37.crudapp.logic.core.CompositeCrudInterceptor;
import com.org73n37.crudapp.logic.spi.CrudStorageProvider;
import com.org73n37.crudapp.logic.spi.CrudStorageProviderFactory;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class CrudEngine {

    private static final Logger log = LoggerFactory.getLogger(CrudEngine.class);
    private final Map<String, ResourceMetadata<?, ?>> resources = new HashMap<>();
    private final Map<Class<?>, List<CrudInterceptor<?>>> interceptorsMap = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    private ServiceRegistry serviceRegistry;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<CrudStorageProviderFactory> storageFactories = new ArrayList<>();

    @Value("${crud.scan.package:com.org73n37.crudapp.data}")
    private String scanPackage = "com.org73n37.crudapp.data";

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSpringInterceptors(List<CrudInterceptor<?>> springInterceptors) {
        if (springInterceptors != null) {
            List<CrudInterceptor<?>> sorted = new ArrayList<>(springInterceptors);
            org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(sorted);
            for (CrudInterceptor<?> interceptor : sorted) {
                Class<?>[] types = org.springframework.core.GenericTypeResolver.resolveTypeArguments(
                        interceptor.getClass(),
                        CrudInterceptor.class
                );
                if (types != null && types.length > 0) {
                    Class<?> entityClass = types[0];
                    registerInterceptor(entityClass, interceptor);
                    log.info("Registered interceptor {} for entity {}", interceptor.getClass().getSimpleName(), entityClass.getSimpleName());
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        discoverAndRegister(scanPackage);
    }

    public void registerInterceptor(Class<?> entityClass, CrudInterceptor<?> interceptor) {
        interceptorsMap.computeIfAbsent(entityClass, k -> new ArrayList<>()).add(interceptor);
    }

    public void discoverAndRegister(String packageName) {
        try {
            org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider scanner =
                    new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(CrudResource.class));

            for (org.springframework.beans.factory.config.BeanDefinition bd : scanner.findCandidateComponents(packageName)) {
                String className = bd.getBeanClassName();
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(CrudResource.class)) {
                    registerResource(clazz);
                }
            }
        } catch (Exception e) {
            log.error("Failed to discover resources in package: " + packageName, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void registerResource(Class<?> entityClazz) {
        if (!BaseEntity.class.isAssignableFrom(entityClazz)) return;
        Class<? extends BaseEntity> entityClass = (Class<? extends BaseEntity>) entityClazz;
        doRegister(entityClass);
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity> void doRegister(Class<T> entityClass) {
        CrudResource annotation = entityClass.getAnnotation(CrudResource.class);
        String path = annotation.path();
        log.info("🚀 Registering dynamic resource: [{}] at path [/api/{}]", entityClass.getSimpleName(), path);

        Class<?> dtoClass = annotation.dto();
        
        // Assert bidirectional mapping consistency
        if (!dtoClass.isAnnotationPresent(com.org73n37.crudapp.infrastructure.annotations.EntityMapping.class)) {
            throw new IllegalStateException("DTO Record " + dtoClass.getSimpleName() + " is missing @EntityMapping annotation!");
        }
        Class<?> mappedEntity = dtoClass.getAnnotation(com.org73n37.crudapp.infrastructure.annotations.EntityMapping.class).entity();
        if (!mappedEntity.equals(entityClass)) {
            throw new IllegalStateException("Bidirectional mapping mismatch: DTO Record " + dtoClass.getSimpleName() + " is mapped to " + mappedEntity.getSimpleName() + " but registered on " + entityClass.getSimpleName());
        }

        Class<? extends CrudService> serviceClass = annotation.service();

        // Pluggable Storage Provider resolution via SPI
        CrudStorageProvider<T> storageProvider = null;
        for (CrudStorageProviderFactory factory : storageFactories) {
            if (factory.supports(entityClass)) {
                storageProvider = factory.getStorageProvider(entityClass);
                break;
            }
        }
        if (storageProvider == null) {
            throw new IllegalStateException("No CrudStorageProviderFactory supports entity: " + entityClass.getName());
        }

        final CrudStorageProvider<T> finalStorage = storageProvider;
        CrudService<T> service;
        try {
            service = (CrudService<T>) serviceClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            service = new CrudService<T>() {
                @Override
                protected CrudStorageProvider<T> getStorageProvider() {
                    return finalStorage;
                }
            };
        }

        // Decouple service resolution via ServiceRegistry
        service = serviceRegistry.getService(entityClass, service);

        // Wrap interceptors in a Composite Chain (including any assignable generic parent interceptors like BaseEntity)
        List<CrudInterceptor<T>> list = new ArrayList<>();
        for (Map.Entry<Class<?>, List<CrudInterceptor<?>>> entry : interceptorsMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(entityClass)) {
                list.addAll((List<CrudInterceptor<T>>) (List<?>) entry.getValue());
            }
        }
        CrudInterceptor<T> interceptor = new CompositeCrudInterceptor<>(list);
        
        List<ResourceMetadata.FieldInfo> fieldMetadata = inspectFields(dtoClass);

        ResourceMetadata<T, ?> metadata = ResourceMetadata.<T, Object>builder()
                .entityClass(entityClass)
                .dtoClass((Class<Object>) dtoClass)
                .basePath(path)
                .version(annotation.version())
                .repository(storageProvider)
                .service(service)
                .interceptor(interceptor)
                .fields(fieldMetadata)
                .build();

        resources.put(path, metadata);
    }

    private List<ResourceMetadata.FieldInfo> inspectFields(Class<?> clazz) {
        List<ResourceMetadata.FieldInfo> infos = new ArrayList<>();
        for (Field field : ReflectionCache.getDeclaredFields(clazz)) {
            Map<String, Object> constraints = new HashMap<>();
            boolean required = field.isAnnotationPresent(NotNull.class) || field.isAnnotationPresent(NotBlank.class);
            
            Size size = ReflectionCache.getAnnotation(field, Size.class);
            if (size != null) {
                constraints.put("min", size.min());
                constraints.put("max", size.max());
            }
            Positive positive = ReflectionCache.getAnnotation(field, Positive.class);
            if (positive != null) {
                constraints.put("positive", true);
            }

            infos.add(new ResourceMetadata.FieldInfo(
                    field.getName(),
                    field.getType().getSimpleName(),
                    required,
                    constraints
            ));
        }
        return infos;
    }

    public Map<String, ResourceMetadata<?, ?>> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    public ResourceMetadata<?, ?> getMetadata(String path) {
        return resources.get(path);
    }
}
