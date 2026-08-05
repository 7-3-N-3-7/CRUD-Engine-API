package com.org73n37.crudapp.service;

import com.org73n37.crudapp.data.IngestedHtmlRepository;
import com.org73n37.crudapp.domain.IngestedHtml;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class HtmlIngestionWorker {
    private static final Logger log = LoggerFactory.getLogger(HtmlIngestionWorker.class);

    private final Space crudSpace;
    private final MinioClient minioClient;
    private final IngestedHtmlRepository repository;

    @Value("${minio.bucket:frontend-assets}")
    private String bucketName;
    
    @Value("${minio.url:http://localhost:9000}")
    private String minioUrl;

    public HtmlIngestionWorker(Space crudSpace, MinioClient minioClient, IngestedHtmlRepository repository) {
        this.crudSpace = crudSpace;
        this.minioClient = minioClient;
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this::processLoop);
        log.info("HtmlIngestionWorker started, waiting for PROCESS_HTML tuples.");
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // ("PROCESS_HTML", trackingId, userId, rawHtmlPayload)
                Object[] tuple = crudSpace.get(
                        new ActualField("PROCESS_HTML"),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(String.class)
                );

                String trackingId = (String) tuple[1];
                String userId = (String) tuple[2];
                String rawHtml = (String) tuple[3];

                log.info("Worker picked up task trackingId={}", trackingId);
                
                try {
                    String processedHtml = processHtml(rawHtml);
                    repository.save(new IngestedHtml(trackingId, userId, processedHtml, "SUCCESS"));
                    crudSpace.put("HTML_PROCESSED", trackingId, "SUCCESS");
                    log.info("Successfully processed task trackingId={}", trackingId);
                } catch (Exception e) {
                    log.error("Failed to process HTML for trackingId={}", trackingId, e);
                    repository.save(new IngestedHtml(trackingId, userId, "", "FAILED: " + e.getMessage()));
                    crudSpace.put("HTML_PROCESSED", trackingId, "FAILED");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("HtmlIngestionWorker interrupted", e);
            }
        }
    }

    private String processHtml(String rawHtml) throws Exception {
        Document doc = Jsoup.parse(rawHtml);
        Elements images = doc.select("img[src^=data:image/]");

        for (Element img : images) {
            String src = img.attr("src");
            String[] parts = src.split(",");
            if (parts.length == 2) {
                String meta = parts[0];
                String base64Data = parts[1];
                
                String extension = "png"; // default
                if (meta.contains("image/jpeg")) extension = "jpg";
                else if (meta.contains("image/gif")) extension = "gif";

                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                String objectName = "extracted/" + UUID.randomUUID().toString() + "." + extension;

                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .stream(bais, imageBytes.length, -1)
                                    .contentType(meta.replace("data:", "").replace(";base64", ""))
                                    .build()
                    );
                }
                
                String newUrl = minioUrl + "/" + bucketName + "/" + objectName;
                img.attr("src", newUrl);
            }
        }

        String rewrittenHtml = doc.body().html(); // Get inner HTML of body

        PolicyFactory policy = new HtmlPolicyBuilder()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowStandardUrlProtocols()
                .allowElements("img")
                .allowAttributes("src").onElements("img")
                .allowAttributes("alt").onElements("img")
                .allowAttributes("class").globally()
                .toFactory();

        HtmlChangeListener<String> failFastListener = new HtmlChangeListener<String>() {
            @Override
            public void discardedTag(String context, String elementName) {
                throw new IllegalArgumentException("HTML validation failed. Disallowed tag: " + elementName);
            }
            @Override
            public void discardedAttributes(String context, String tagName, String... attributeNames) {
                throw new IllegalArgumentException("HTML validation failed. Disallowed attribute(s) on " + tagName);
            }
        };

        return policy.sanitize(rewrittenHtml, failFastListener, "Context");
    }
}
