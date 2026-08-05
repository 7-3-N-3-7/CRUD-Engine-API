package com.org73n37.crudapp.api;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.org73n37.crudapp.data.IngestedHtmlRepository;
import com.org73n37.crudapp.data.TranslationDocumentRepository;
import com.org73n37.crudapp.domain.IngestedHtml;
import com.org73n37.crudapp.domain.TranslationDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pages")
public class SsrController {

    private final IngestedHtmlRepository ingestedHtmlRepository;
    private final TranslationDocumentRepository translationRepository;
    private final Handlebars handlebars;

    public SsrController(IngestedHtmlRepository ingestedHtmlRepository,
                         TranslationDocumentRepository translationRepository,
                         Handlebars handlebars) {
        this.ingestedHtmlRepository = ingestedHtmlRepository;
        this.translationRepository = translationRepository;
        this.handlebars = handlebars;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<String> getPage(
            @PathVariable("slug") String slug,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {

        // 1. Fetch the raw HTML from Postgres
        Optional<IngestedHtml> htmlOptional = ingestedHtmlRepository.findBySlug(slug);
        if (htmlOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Page not found");
        }

        IngestedHtml ingestedHtml = htmlOptional.get();

        // If the HTML processing failed or is incomplete
        if (!"SUCCESS".equals(ingestedHtml.getStatus())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Page is still processing or failed");
        }

        // 2. Determine the requested language, default to "en"
        String language = "en";
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            // Very naive locale parsing for demo purposes
            language = Locale.LanguageRange.parse(acceptLanguage).get(0).getRange().split("-")[0];
        }

        // 3. Fetch translations from MongoDB
        Map<String, Object> translations = Collections.emptyMap();
        Optional<TranslationDocument> translationOptional = translationRepository.findBySlugAndLanguage(slug, language);
        
        if (translationOptional.isEmpty() && !"en".equals(language)) {
            // Fallback to English
            translationOptional = translationRepository.findBySlugAndLanguage(slug, "en");
        }

        if (translationOptional.isPresent()) {
            translations = translationOptional.get().getTranslations();
        }

        // 4. Compile the HTML with Handlebars
        try {
            Template template = handlebars.compileInline(ingestedHtml.getHtmlContent());
            String renderedHtml = template.apply(translations);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(renderedHtml);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error rendering page template");
        }
    }
}
