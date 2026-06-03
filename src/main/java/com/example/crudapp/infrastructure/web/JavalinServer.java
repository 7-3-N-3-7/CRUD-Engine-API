package com.example.crudapp.infrastructure.web;

import com.example.crudapp.api.JavalinUniversalController;
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

@Component
public class JavalinServer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(JavalinServer.class);

    private final DynamicCrudManager crudManager;
    private final PlatformTransactionManager transactionManager;
    private final JwtInterceptor jwtInterceptor;
    
    @Value("${server.port:8080}")
    private int port;

    private Javalin app;

    public JavalinServer(DynamicCrudManager crudManager, PlatformTransactionManager transactionManager, JwtInterceptor jwtInterceptor) {
        this.crudManager = crudManager;
        this.transactionManager = transactionManager;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Javalin server on port {}...", port);
        
        JavalinUniversalController controller = new JavalinUniversalController(crudManager, transactionManager);

        app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                // Public routes
                io.javalin.apibuilder.ApiBuilder.get("/api/v2/metadata", controller::getMetadata);
                
                // Secure routes
                io.javalin.apibuilder.ApiBuilder.before("/api/v2/{resource}*", jwtInterceptor);
                
                io.javalin.apibuilder.ApiBuilder.get("/api/v2/{resource}", controller::getAll);
                io.javalin.apibuilder.ApiBuilder.post("/api/v2/{resource}", controller::create);
                io.javalin.apibuilder.ApiBuilder.get("/api/v2/{resource}/{id}", controller::getById);
                io.javalin.apibuilder.ApiBuilder.put("/api/v2/{resource}/{id}", controller::update);
                io.javalin.apibuilder.ApiBuilder.delete("/api/v2/{resource}/{id}", controller::delete);
            });
        }).start(port);

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
