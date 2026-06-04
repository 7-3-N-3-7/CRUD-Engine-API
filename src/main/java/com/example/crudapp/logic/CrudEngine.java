package com.example.crudapp.logic;

import com.example.crudapp.data.core.BaseEntity;
import com.example.crudapp.data.core.CrudRepository;
import com.example.crudapp.infrastructure.annotations.CrudResource;
import com.example.crudapp.infrastructure.mapping.ReflectionCache;
import com.example.crudapp.logic.core.CrudService;
import com.example.crudapp.logic.core.CrudInterceptor;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

@Service
public class CrudEngine {

    private static final Logger log = LoggerFactory.getLogger(CrudEngine.class);
    private final Map<String, ResourceMetadata<?, ?>> resources = new HashMap<>();
    private final Map<Class<?>, CrudInterceptor<?>> interceptors = new HashMap<>();

    @PersistenceContext
    private EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    private ServiceRegistry serviceRegistry;

    @Value("${crud.scan.package:com.example.crudapp.data}")
    private String scanPackage = "com.example.crudapp.data";

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSpringInterceptors(List<CrudInterceptor<?>> springInterceptors) {
        if (springInterceptors != null) {
            for (CrudInterceptor<?> interceptor : springInterceptors) {
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
        interceptors.put(entityClass, interceptor);
    }

    public void discoverAndRegister(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(path);
            while (urls.hasMoreElements()) {
                URL resource = urls.nextElement();
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    File[] files = directory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".class")) {
                                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                                Class<?> clazz = Class.forName(className);
                                if (clazz.isAnnotationPresent(CrudResource.class)) {
                                    registerResource(clazz);
                                }
                            }
                        }
                    }
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

    private <T extends BaseEntity> void doRegister(Class<T> entityClass) {
        CrudResource annotation = entityClass.getAnnotation(CrudResource.class);
        String path = annotation.path();
        log.info("🚀 Registering dynamic resource: [{}] at path [/api/{}]", entityClass.getSimpleName(), path);

        Class<?> dtoClass = annotation.dto();
        
        // Assert bidirectional mapping consistency
        if (!dtoClass.isAnnotationPresent(com.example.crudapp.infrastructure.annotations.EntityMapping.class)) {
            throw new IllegalStateException("DTO Record " + dtoClass.getSimpleName() + " is missing @EntityMapping annotation!");
        }
        Class<?> mappedEntity = dtoClass.getAnnotation(com.example.crudapp.infrastructure.annotations.EntityMapping.class).entity();
        if (!mappedEntity.equals(entityClass)) {
            throw new IllegalStateException("Bidirectional mapping mismatch: DTO Record " + dtoClass.getSimpleName() + " is mapped to " + mappedEntity.getSimpleName() + " but registered on " + entityClass.getSimpleName());
        }

        Class<? extends CrudService> serviceClass = annotation.service();

        CrudRepository<T> repository = new CrudRepository<>(entityClass, entityManager);

        CrudService<T> service;
        try {
            service = (CrudService<T>) serviceClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            service = new CrudService<T>() {
                @Override
                protected CrudRepository<T> getRepository() {
                    return repository;
                }
            };
        }

        // Decouple service resolution via ServiceRegistry
        service = serviceRegistry.getService(entityClass, service);

        CrudInterceptor<T> interceptor = (CrudInterceptor<T>) interceptors.getOrDefault(entityClass, new CrudInterceptor<T>() {});
        List<ResourceMetadata.FieldInfo> fieldMetadata = inspectFields(dtoClass);

        ResourceMetadata<T, ?> metadata = ResourceMetadata.<T, Object>builder()
                .entityClass(entityClass)
                .dtoClass((Class<Object>) dtoClass)
                .basePath(path)
                .version(annotation.version())
                .repository(repository)
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
