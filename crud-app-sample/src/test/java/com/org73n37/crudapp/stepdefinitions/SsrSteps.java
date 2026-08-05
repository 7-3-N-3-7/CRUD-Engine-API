package com.org73n37.crudapp.stepdefinitions;

import com.org73n37.crudapp.data.IngestedHtmlRepository;
import com.org73n37.crudapp.data.TranslationDocumentRepository;
import com.org73n37.crudapp.domain.IngestedHtml;
import com.org73n37.crudapp.domain.TranslationDocument;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class SsrSteps {

    @Autowired
    private CucumberSpringConfiguration config;

    @Autowired
    private TestContext testContext;

    @Autowired
    private IngestedHtmlRepository ingestedHtmlRepository;

    @Autowired
    private TranslationDocumentRepository translationRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    @Given("a processed HTML page exists with slug {string} and content {string}")
    public void aProcessedHtmlPageExistsWithSlugAndContent(String slug, String content) {
        IngestedHtml html = new IngestedHtml("tracking-" + slug, "system", slug, content, "SUCCESS");
        ingestedHtmlRepository.save(html);
    }

    @Given("an English translation exists for {string} with title {string}")
    public void anEnglishTranslationExistsForWithTitle(String slug, String title) {
        TranslationDocument doc = new TranslationDocument();
        doc.setSlug(slug);
        doc.setLanguage("en");
        doc.setTranslations(Map.of("title", title));
        Mockito.when(translationRepository.findBySlugAndLanguage(slug, "en")).thenReturn(Optional.of(doc));
    }

    @Given("a French translation exists for {string} with title {string}")
    public void aFrenchTranslationExistsForWithTitle(String slug, String title) {
        TranslationDocument doc = new TranslationDocument();
        doc.setSlug(slug);
        doc.setLanguage("fr");
        doc.setTranslations(Map.of("title", title));
        Mockito.when(translationRepository.findBySlugAndLanguage(slug, "fr")).thenReturn(Optional.of(doc));
    }

    @When("I request the page {string} with Accept-Language {string}")
    public void iRequestThePageWithAcceptLanguage(String path, String language) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + path))
                .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                .GET()
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
