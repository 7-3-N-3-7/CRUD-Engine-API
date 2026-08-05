package com.org73n37.crudapp.config;

import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JSpaceConfig {
    private static final Logger log = LoggerFactory.getLogger(JSpaceConfig.class);

    @Value("${jspace.port:9002}")
    private int port;

    @Value("${jspace.name:crud-space}")
    private String spaceName;

    @Value("${jspace.mode:embedded}")
    private String mode;

    @Bean
    public Space crudSpace() {
        SequentialSpace space = new SequentialSpace();
        
        if ("embedded".equalsIgnoreCase(mode)) {
            try {
                SpaceRepository repository = new SpaceRepository();
                repository.add(spaceName, space);
                String uri = "tcp://0.0.0.0:" + port + "/?keep";
                repository.addGate(uri);
                log.info("Started embedded jSpace server at {}", uri);
            } catch (Exception e) {
                log.error("Failed to start embedded jSpace server", e);
                throw new RuntimeException("jSpace initialization failed", e);
            }
        }
        
        return space;
    }
}
