package com.org73n37.crudapp.api;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateSyncWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StateSyncWebSocketHandler.class);

    private final Map<String, Sinks.Many<String>> userSinks = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    @Autowired
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        
        // Setup output sink for this session to push messages down to the client
        Sinks.Many<String> sessionSink = Sinks.many().unicast().onBackpressureBuffer();
        
        // Stream to send back to client
        Flux<WebSocketMessage> outFlux = sessionSink.asFlux()
                .map(session::textMessage);

        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        JsonNode json = objectMapper.readTree(payload);
                        String type = json.path("type").asText();
                        
                        if ("AUTH".equals(type)) {
                            String token = json.path("token").asText();
                            
                            return jwtDecoder.decode(token)
                                    .flatMap(jwt -> {
                                        String userId = jwt.getClaimAsString("preferred_username");
                                        if (userId == null) {
                                            userId = jwt.getSubject();
                                        }
                                        
                                        log.info("WebSocket authenticated for user: {}", userId);
                                        sessionToUser.put(sessionId, userId);
                                        userSinks.put(userId, sessionSink);
                                        sessionSink.tryEmitNext("{\"type\":\"AUTH_SUCCESS\"}");
                                        return Mono.empty();
                                    })
                                    .onErrorResume(e -> {
                                        log.warn("WebSocket Auth failed: {}", e.getMessage());
                                        sessionSink.tryEmitNext("{\"type\":\"AUTH_FAILED\"}");
                                        return session.close();
                                    })
                                    .then();
                        }
                    } catch (Exception e) {
                        log.error("Error processing websocket message", e);
                    }
                    return Mono.empty();
                })
                .doFinally(sig -> {
                    String userId = sessionToUser.remove(sessionId);
                    if (userId != null) {
                        userSinks.remove(userId);
                        log.info("WebSocket disconnected for user: {}", userId);
                    }
                })
                .then();

        Mono<Void> output = session.send(outFlux);
        
        return Mono.zip(input, output).then();
    }
    
    public void pushToUser(String userId, String message) {
        Sinks.Many<String> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(message);
        }
    }
}
