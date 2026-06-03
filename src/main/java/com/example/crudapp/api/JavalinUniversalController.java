package com.example.crudapp.api;

import com.example.crudapp.data.core.BaseEntity;
import com.example.crudapp.logic.DynamicCrudManager;
import com.example.crudapp.logic.ResourceMetadata;
import com.example.crudapp.logic.core.BaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JavalinUniversalController {
    private static final Logger log = LoggerFactory.getLogger(JavalinUniversalController.class);
    private final DynamicCrudManager crudManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    public JavalinUniversalController(DynamicCrudManager crudManager, PlatformTransactionManager transactionManager) {
        this.crudManager = crudManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public void getMetadata(Context ctx) {
        log.debug("🔍 Fetching global metadata");
        Map<String, List<ResourceMetadata.FieldInfo>> metadata = crudManager.getResources().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getFields()
                ));
        ctx.json(metadata);
    }

    public void getAll(Context ctx) {
        String resource = ctx.pathParam("resource");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
        int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(10);

        ResourceMetadata metadata = getMetadataOrThrow(resource);
        
        BaseService.Page<? extends BaseEntity> entityPage = transactionTemplate.execute(status -> 
            metadata.getService().findAll(page, size)
        );

        ctx.header("X-Total-Count", String.valueOf(entityPage.getTotalElements()));
        ctx.json(entityPage.getContent());
    }

    @SuppressWarnings("unchecked")
    public void getById(Context ctx) {
        String resource = ctx.pathParam("resource");
        Long id = Long.parseLong(ctx.pathParam("id"));

        ResourceMetadata metadata = getMetadataOrThrow(resource);
        
        Optional<BaseEntity> result = (Optional<BaseEntity>) transactionTemplate.execute(status -> 
            metadata.getService().findById(id)
        );
        
        result.ifPresentOrElse(ctx::json, () -> ctx.status(404));
    }

    public void create(Context ctx) {
        String resource = ctx.pathParam("resource");
        ResourceMetadata metadata = getMetadataOrThrow(resource);

        Object dto = ctx.bodyAsClass(metadata.getDtoClass());
        validate(dto);

        BaseEntity entity = (BaseEntity) objectMapper.convertValue(dto, metadata.getEntityClass());
        
        BaseEntity saved = transactionTemplate.execute(status -> {
            metadata.getInterceptor().beforeCreate(entity);
            BaseEntity res = (BaseEntity) metadata.getService().save(entity);
            metadata.getInterceptor().afterCreate(res);
            return res;
        });

        ctx.status(201).json(saved);
    }

    public void update(Context ctx) {
        String resource = ctx.pathParam("resource");
        Long id = Long.parseLong(ctx.pathParam("id"));
        ResourceMetadata metadata = getMetadataOrThrow(resource);

        Object dto = ctx.bodyAsClass(metadata.getDtoClass());
        validate(dto);

        BaseEntity entity = (BaseEntity) objectMapper.convertValue(dto, metadata.getEntityClass());
        
        BaseEntity updated = transactionTemplate.execute(status -> {
            metadata.getInterceptor().beforeUpdate(entity);
            BaseEntity res = (BaseEntity) metadata.getService().update(id, entity);
            metadata.getInterceptor().afterUpdate(res);
            return res;
        });

        ctx.json(updated);
    }

    public void delete(Context ctx) {
        String resource = ctx.pathParam("resource");
        Long id = Long.parseLong(ctx.pathParam("id"));
        ResourceMetadata metadata = getMetadataOrThrow(resource);

        transactionTemplate.executeWithoutResult(status -> {
            metadata.getInterceptor().beforeDelete(id);
            metadata.getService().deleteById(id);
            metadata.getInterceptor().afterDelete(id);
        });

        ctx.status(204);
    }

    private ResourceMetadata getMetadataOrThrow(String resource) {
        ResourceMetadata metadata = crudManager.getMetadata(resource);
        if (metadata == null) {
            throw new RuntimeException("Resource not found: " + resource);
        }
        return metadata;
    }

    private void validate(Object dto) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new RuntimeException("Validation failed: " + violations);
        }
    }
}
