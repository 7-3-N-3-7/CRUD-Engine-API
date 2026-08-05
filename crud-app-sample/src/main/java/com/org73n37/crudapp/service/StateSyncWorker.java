package com.org73n37.crudapp.service;

import com.org73n37.crudapp.api.StateSyncWebSocketHandler;
import org.jspace.FormalField;
import org.jspace.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class StateSyncWorker {

    private static final Logger log = LoggerFactory.getLogger(StateSyncWorker.class);

    @Autowired
    @Qualifier("crudSpace")
    private Space crudSpace;

    @Autowired
    private StateSyncWebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        executorService.submit(() -> {
            log.info("StateSyncWorker started listening for BROADCAST tuples on crud-space");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for a broadcast tuple
                    Object[] tuple = crudSpace.get(
                            new FormalField(String.class), // Action: "BROADCAST"
                            new FormalField(String.class), // trackingId
                            new FormalField(String.class), // userId
                            new FormalField(String.class)  // status
                    );

                    String action = (String) tuple[0];
                    if (!"BROADCAST".equals(action)) {
                        continue;
                    }

                    String trackingId = (String) tuple[1];
                    String userId = (String) tuple[2];
                    String status = (String) tuple[3];

                    log.info("Received BROADCAST for user {} - trackingId: {} status: {}", userId, trackingId, status);

                    // Push to the user's websocket
                    Map<String, String> message = new HashMap<>();
                    message.put("type", "UPDATE");
                    message.put("trackingId", trackingId);
                    message.put("status", status);

                    String jsonMessage = objectMapper.writeValueAsString(message);
                    webSocketHandler.pushToUser(userId, jsonMessage);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("StateSyncWorker interrupted", e);
                } catch (Exception e) {
                    log.error("Error processing broadcast tuple", e);
                }
            }
        });
    }
}
