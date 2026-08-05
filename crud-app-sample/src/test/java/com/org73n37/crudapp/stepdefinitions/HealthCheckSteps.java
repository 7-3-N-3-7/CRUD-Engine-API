package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HealthCheckSteps {

    @Autowired
    private CucumberSpringConfiguration config;

    @Autowired
    private TestContext testContext;

    private final HttpClient client = HttpClient.newHttpClient();

    @When("I request the health endpoint {string}")
    public void i_request_the_health_endpoint(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + endpoint))
                .GET()
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String expectedBody) {
        assertTrue(testContext.lastResponse.body().contains(expectedBody));
    }
}
