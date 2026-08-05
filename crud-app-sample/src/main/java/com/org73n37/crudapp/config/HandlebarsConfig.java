package com.org73n37.crudapp.config;

import com.github.jknack.handlebars.Handlebars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlebarsConfig {

    @Bean
    public Handlebars handlebars() {
        return new Handlebars();
    }
}
