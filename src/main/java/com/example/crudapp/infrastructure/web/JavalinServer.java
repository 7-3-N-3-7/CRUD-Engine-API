package com.example.crudapp.infrastructure.web;

import com.example.crudapp.api.JavalinUniversalController;
import com.example.crudapp.api.HealthController;
import com.example.crudapp.api.errors.ErrorResponse;
import com.example.crudapp.api.errors.ResourceNotFoundException;
import com.example.crudapp.api.errors.ValidationException;
import com.example.crudapp.infrastructure.security.JwtInterceptor;
import com.example.crudapp.logic.DynamicCrudManager;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.time.Instant;

import tools.jackson.databind.ObjectMapper;

@Component
public class JavalinServer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(JavalinServer.class);

    private final DynamicCrudManager crudManager;
    private final PlatformTransactionManager transactionManager;
    private final JwtInterceptor jwtInterceptor;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    
    @Value("${server.port:8080}")
    private int port;

    private Javalin app;

    public JavalinServer(DynamicCrudManager crudManager, 
                         PlatformTransactionManager transactionManager, 
                         JwtInterceptor jwtInterceptor,
                         DataSource dataSource,
                         ObjectMapper objectMapper) {
        this.crudManager = crudManager;
        this.transactionManager = transactionManager;
        this.jwtInterceptor = jwtInterceptor;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Javalin server on port {}...", port);
        
        JavalinUniversalController controller = new JavalinUniversalController(crudManager, transactionManager);
        HealthController healthController = new HealthController(dataSource);

        app = Javalin.create(config -> {
            config.jsonMapper(new Jackson3Mapper(objectMapper));
            config.jetty.modifyServer(server -> {
                server.setStopTimeout(15000); // 15 seconds grace period for active requests
            });

            // Exception handlers
            config.routes.exception(ResourceNotFoundException.class, (e, ctx) -> {
                String reqId = ctx.attribute(RequestTracingFilter.MDC_KEY);
                ctx.status(404).json(new ErrorResponse(
                    Instant.now(), 404, "Not Found", e.getMessage(), ctx.path(), reqId, null
                ));
            });

            config.routes.exception(ValidationException.class, (e, ctx) -> {
                String reqId = ctx.attribute(RequestTracingFilter.MDC_KEY);
                ctx.status(400).json(new ErrorResponse(
                    Instant.now(), 400, "Bad Request", "Validation failed", ctx.path(), reqId, e.getErrors()
                ));
            });

            config.routes.exception(org.springframework.orm.ObjectOptimisticLockingFailureException.class, (e, ctx) -> {
                String reqId = ctx.attribute(RequestTracingFilter.MDC_KEY);
                ctx.status(409).json(new ErrorResponse(
                    Instant.now(), 409, "Conflict", "Concurrency conflict detected: the record has been modified by another transaction", ctx.path(), reqId, null
                ));
            });

            config.routes.exception(SecurityException.class, (e, ctx) -> {
                String reqId = ctx.attribute(RequestTracingFilter.MDC_KEY);
                ctx.status(403).json(new ErrorResponse(
                    Instant.now(), 403, "Forbidden", e.getMessage(), ctx.path(), reqId, null
                ));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                log.error("Unhandled internal server error", e);
                String reqId = ctx.attribute(RequestTracingFilter.MDC_KEY);
                ctx.status(500).json(new ErrorResponse(
                    Instant.now(), 500, "Internal Server Error", "An unexpected error occurred", ctx.path(), reqId, null
                ));
            });

            config.routes.apiBuilder(() -> {
                // Request tracing context initialization (MDC + Headers)
                io.javalin.apibuilder.ApiBuilder.before("*", new RequestTracingFilter());

                // Public endpoints
                io.javalin.apibuilder.ApiBuilder.get("/api/metadata", controller::getMetadata);
                io.javalin.apibuilder.ApiBuilder.get("/swagger-ui", controller::getSwaggerUi);
                io.javalin.apibuilder.ApiBuilder.get("/api-docs", controller::getOpenApiJson);
                
                // Health Check Probes
                io.javalin.apibuilder.ApiBuilder.get("/health/liveness", healthController::liveness);
                io.javalin.apibuilder.ApiBuilder.get("/health/readiness", healthController::readiness);
                
                // Secure routes interceptor
                io.javalin.apibuilder.ApiBuilder.before("/api/{resource}*", jwtInterceptor);
                
                // Secure endpoints
                io.javalin.apibuilder.ApiBuilder.get("/api/{resource}", controller::getAll);
                io.javalin.apibuilder.ApiBuilder.post("/api/{resource}", controller::create);
                io.javalin.apibuilder.ApiBuilder.get("/api/{resource}/{id}", controller::getById);
                io.javalin.apibuilder.ApiBuilder.put("/api/{resource}/{id}", controller::update);
                io.javalin.apibuilder.ApiBuilder.delete("/api/{resource}/{id}", controller::delete);

                // Global post-request cleanup of thread-local resources and MDC
                io.javalin.apibuilder.ApiBuilder.after("*", ctx -> {
                    com.example.crudapp.infrastructure.security.TenantContext.clear();
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    RequestTracingFilter.clear();
                });
            });
        });

        app.start(port);
        log.info("🚀 Javalin server started successfully on port {}", port);
    }

    public int getPort() {
        return app != null ? app.port() : port;
    }

    @PreDestroy
    public void stop() {
        if (app != null) {
            log.info("Stopping Javalin server...");
            app.stop();
        }
    }
}


