package com.example.crudapp.api;

import io.javalin.http.Context;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

public class HealthController {
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void liveness(Context ctx) {
        ctx.status(200).result("UP");
    }

    public void readiness(Context ctx) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            ctx.status(200).json(Map.of("status", "UP", "database", "UP"));
        } catch (Exception e) {
            ctx.status(503).json(Map.of("status", "DOWN", "database", "DOWN", "error", e.getMessage()));
        }
    }
}
