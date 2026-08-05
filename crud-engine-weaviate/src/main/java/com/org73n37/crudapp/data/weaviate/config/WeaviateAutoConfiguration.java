package com.org73n37.crudapp.data.weaviate.config;

import io.weaviate.client6.v1.api.WeaviateClient;
import io.weaviate.client6.v1.api.Authentication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(WeaviateClient.class)
@EnableConfigurationProperties(WeaviateProperties.class)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "crud.engine.weaviate", name = "enabled", havingValue = "true", matchIfMissing = false
)
public class WeaviateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WeaviateClient weaviateClient(WeaviateProperties properties) {
        String host = properties.getHost();
        String hostName = host;
        int port = 8080;
        if (host.contains(":")) {
            String[] parts = host.split(":");
            hostName = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }
        final String finalHost = hostName;
        final int finalPort = port;
        final int finalGrpcPort = properties.getGrpcPort();

        return WeaviateClient.connectToCustom(conn -> {
            conn.scheme(properties.getScheme())
                .httpHost(finalHost)
                .httpPort(finalPort)
                .grpcHost(finalHost)
                .grpcPort(finalGrpcPort);
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                conn.authentication(Authentication.apiKey(properties.getApiKey()));
            }
            return conn;
        });
    }
}
