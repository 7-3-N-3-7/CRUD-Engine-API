package com.org73n37.crudapp.stepdefinitions;

import com.org73n37.crudapp.data.IngestedHtmlRepository;
import com.org73n37.crudapp.domain.IngestedHtml;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.jspace.ActualField;
import org.jspace.Space;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlIngestionSteps {

    @Autowired
    private CucumberSpringConfiguration config;

    @Autowired
    private TestContext testContext;

    @Autowired
    private IngestedHtmlRepository ingestedHtmlRepository;

    @Autowired
    private Space crudSpace;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @When("I submit raw HTML {string} with slug {string}")
    public void iSubmitRawHtmlWithSlug(String rawHtml, String slug) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + "/api/html/ingest?slug=" + slug))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testContext.currentUserToken)
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(rawHtml))
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Then("the HTML processing should eventually complete for slug {string}")
    public void theHtmlProcessingShouldEventuallyCompleteForSlug(String slug) throws Exception {
        waitForStatus(slug, "SUCCESS");
    }

    @Then("the HTML processing should eventually fail for slug {string}")
    public void theHtmlProcessingShouldEventuallyFailForSlug(String slug) throws Exception {
        waitForStatus(slug, "FAILED");
    }

    private void waitForStatus(String slug, String expectedStatusPrefix) throws Exception {
        // Parse trackingId from the last response
        String responseBody = testContext.lastResponse.body().toString();
        JsonNode jsonNode = mapper.readTree(responseBody);
        String trackingId = jsonNode.get("trackingId").asText();

        // Wait for the HTML_PROCESSED tuple in the space
        Object[] tuple = crudSpace.get(new ActualField("HTML_PROCESSED"), new ActualField(trackingId), new org.jspace.FormalField(String.class));
        String status = (String) tuple[2];
        
        if (status == null || !status.startsWith(expectedStatusPrefix)) {
            fail("Expected status starting with " + expectedStatusPrefix + " but got " + status + " for slug: " + slug);
        }
    }
}
