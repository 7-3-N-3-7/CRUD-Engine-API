package com.org73n37.crudapp.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bean holding and exposing the application environment mode.
 */
@Configuration
public class AppModeConfig {

    @Value("${app.mode:PRODUCTION}")
    private AppMode mode;

    public AppMode getMode() {
        return mode;
    }

    public void setMode(AppMode mode) {
        this.mode = mode;
    }

    public boolean isProduction() {
        return mode == AppMode.PRODUCTION;
    }

    public boolean isDevelopment() {
        return mode == AppMode.DEVELOPMENT;
    }
}
