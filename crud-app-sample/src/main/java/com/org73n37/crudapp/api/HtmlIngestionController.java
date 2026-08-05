package com.org73n37.crudapp.api;

import org.jspace.Space;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/html")
public class HtmlIngestionController {

    private final Space crudSpace;

    public HtmlIngestionController(Space crudSpace) {
        this.crudSpace = crudSpace;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestHtml(
            @RequestParam(value = "slug", required = false) String slug,
            @RequestBody String rawHtml, 
            Principal principal) {
        // Extract userId, fallback to 'anonymous' if security is disabled during dev
        String userId = (principal != null) ? principal.getName() : "anonymous";
        String trackingId = UUID.randomUUID().toString();
        String finalSlug = (slug != null && !slug.isEmpty()) ? slug : trackingId;

        try {
            // Drop tuple: ("PROCESS_HTML", trackingId, userId, slug, rawHtmlPayload)
            crudSpace.put("PROCESS_HTML", trackingId, userId, finalSlug, rawHtml);
            return ResponseEntity.accepted().body("{\"trackingId\": \"" + trackingId + "\", \"slug\": \"" + finalSlug + "\"}");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Failed to enqueue HTML processing task");
        }
    }
}
