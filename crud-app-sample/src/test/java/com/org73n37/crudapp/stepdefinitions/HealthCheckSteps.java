package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class HealthCheckSteps {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ResponseSpec responseSpec;

    @When("I request the health endpoint {string}")
    public void i_request_the_health_endpoint(String endpoint) {
        responseSpec = webTestClient.get()
                .uri(endpoint)
                .exchange();
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String expectedBody) {
        responseSpec.expectBody(String.class).value(body -> {
            assert body.contains(expectedBody);
        });
    }
}
