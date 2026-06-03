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

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public void getSwaggerUi(Context ctx) {
        ctx.contentType("text/html");
        ctx.result("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Swagger UI - Generic CRUD Engine</title>
                <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
                <style>
                    html { box-sizing: border-box; overflow: -y-scroll; }
                    *, *:before, *:after { box-sizing: inherit; }
                    body { margin:0; background: #fafafa; }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
                <script>
                    window.onload = function() {
                        const ui = SwaggerUIBundle({
                            url: "/api-docs",
                            dom_id: '#swagger-ui',
                            deepLinking: true,
                            presets: [
                                SwaggerUIBundle.presets.apis,
                                SwaggerUIStandalonePreset
                            ],
                            plugins: [
                                SwaggerUIBundle.plugins.DownloadUrl
                            ],
                            layout: "BaseLayout"
                        });
                        window.ui = ui;
                    };
                </script>
            </body>
            </html>
            """);
    }

    @SuppressWarnings("unchecked")
    public void getOpenApiJson(Context ctx) {
        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("openapi", "3.0.1");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "Generic CRUD Engine API");
        info.put("version", "2.0.0");
        info.put("description", "Dynamic metadata-driven secured CRUD endpoints.");
        openapi.put("info", info);

        openapi.put("servers", List.of(Map.of("url", "/")));

        Map<String, Object> paths = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();

        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> securitySchemes = new LinkedHashMap<>();
        securitySchemes.put("BearerAuth", Map.of(
                "type", "http",
                "scheme", "bearer",
                "bearerFormat", "JWT"
        ));
        components.put("securitySchemes", securitySchemes);
        components.put("schemas", schemas);
        openapi.put("components", components);

        openapi.put("security", List.of(Map.of("BearerAuth", List.of())));

        for (Map.Entry<String, ResourceMetadata<?, ?>> entry : crudManager.getResources().entrySet()) {
            String path = entry.getKey();
            ResourceMetadata<?, ?> metadata = entry.getValue();
            String schemaName = metadata.getDtoClass().getSimpleName();

            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");

            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> requiredFields = new ArrayList<>();

            for (ResourceMetadata.FieldInfo field : metadata.getFields()) {
                Map<String, Object> fieldProp = new LinkedHashMap<>();
                String typeName = field.getType().toLowerCase();

                if (typeName.contains("int") || typeName.contains("long")) {
                    fieldProp.put("type", "integer");
                } else if (typeName.contains("double") || typeName.contains("float") || typeName.contains("price") || typeName.contains("number")) {
                    fieldProp.put("type", "number");
                } else if (typeName.contains("boolean")) {
                    fieldProp.put("type", "boolean");
                } else {
                    fieldProp.put("type", "string");
                }

                properties.put(field.getName(), fieldProp);
                if (field.isRequired()) {
                    requiredFields.add(field.getName());
                }
            }

            schema.put("properties", properties);
            if (!requiredFields.isEmpty()) {
                schema.put("required", requiredFields);
            }
            schemas.put(schemaName, schema);

            String resourcePath = "/api/" + path;
            Map<String, Object> pathOperations = new LinkedHashMap<>();

            Map<String, Object> getOp = new LinkedHashMap<>();
            getOp.put("tags", List.of(path));
            getOp.put("summary", "List all " + path);
            getOp.put("parameters", List.of(
                    Map.of("name", "page", "in", "query", "required", false, "schema", Map.of("type", "integer", "default", 0)),
                    Map.of("name", "size", "in", "query", "required", false, "schema", Map.of("type", "integer", "default", 10))
            ));
            getOp.put("responses", Map.of(
                    "200", Map.of(
                            "description", "Successful operation",
                            "content", Map.of("application/json", Map.of("schema", Map.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + schemaName))))
                    )
            ));
            pathOperations.put("get", getOp);

            Map<String, Object> postOp = new LinkedHashMap<>();
            postOp.put("tags", List.of(path));
            postOp.put("summary", "Create new " + path);
            postOp.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName)))
            ));
            postOp.put("responses", Map.of(
                    "201", Map.of(
                            "description", "Created",
                            "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName)))
                    )
            ));
            pathOperations.put("post", postOp);
            paths.put(resourcePath, pathOperations);

            String singleResourcePath = resourcePath + "/{id}";
            Map<String, Object> singlePathOperations = new LinkedHashMap<>();

            List<Map<String, Object>> pathParams = List.of(
                    Map.of("name", "id", "in", "path", "required", true, "schema", Map.of("type", "integer"))
            );

            Map<String, Object> getByIdOp = new LinkedHashMap<>();
            getByIdOp.put("tags", List.of(path));
            getByIdOp.put("summary", "Get " + path + " by ID");
            getByIdOp.put("parameters", pathParams);
            getByIdOp.put("responses", Map.of(
                    "200", Map.of(
                            "description", "Successful operation",
                            "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName)))
                    ),
                    "404", Map.of("description", "Not Found")
            ));
            singlePathOperations.put("get", getByIdOp);

            Map<String, Object> putOp = new LinkedHashMap<>();
            putOp.put("tags", List.of(path));
            putOp.put("summary", "Update " + path + " by ID");
            putOp.put("parameters", pathParams);
            putOp.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName)))
            ));
            putOp.put("responses", Map.of(
                    "200", Map.of(
                            "description", "Successful operation",
                            "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName)))
                    )
            ));
            singlePathOperations.put("put", putOp);

            Map<String, Object> deleteOp = new LinkedHashMap<>();
            deleteOp.put("tags", List.of(path));
            deleteOp.put("summary", "Delete " + path + " by ID");
            deleteOp.put("parameters", pathParams);
            deleteOp.put("responses", Map.of(
                    "204", Map.of("description", "No Content")
            ));
            singlePathOperations.put("delete", deleteOp);

            paths.put(singleResourcePath, singlePathOperations);
        }

        openapi.put("paths", paths);
        ctx.json(openapi);
    }
}
