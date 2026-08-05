package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductSteps {

    @Autowired
    private CucumberSpringConfiguration config;

    @Autowired
    private TestContext testContext;
    
    private final HttpClient client = HttpClient.newHttpClient();

    private String generateToken(String username, String role) {
        Map<String, Object> realmAccess = Map.of("roles", List.of(role));
        return io.jsonwebtoken.Jwts.builder()
                .header()
                    .keyId("test-key-id")
                    .and()
                .subject(username)
                .claim("preferred_username", username)
                .claim("realm_access", realmAccess)
                .signWith(CucumberSpringConfiguration.keyPair.getPrivate(), io.jsonwebtoken.Jwts.SIG.RS256)
                .compact();
    }

    @Given("the user {string} has the role {string}")
    public void theUserHasTheRole(String username, String role) {
        testContext.currentUserToken = generateToken(username, role);
    }

    @When("the user creates a product with name {string} and price {double}")
    public void theUserCreatesAProductWithNameAndPrice(String name, double price) throws Exception {
        String jsonPayload = String.format("{\"name\": \"%s\", \"price\": %s}", name, price);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + "/api/products"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testContext.currentUserToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (testContext.lastResponse.statusCode() == 201) {
            // Extract ID
            String body = testContext.lastResponse.body();
            int idIndex = body.indexOf("\"id\":");
            if (idIndex != -1) {
                int idEndIndex = body.indexOf(",", idIndex + 5);
                testContext.lastCreatedProductId = body.substring(idIndex + 5, idEndIndex);
            }
        }
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, testContext.lastResponse.statusCode(), "Response body: " + testContext.lastResponse.body());
    }

    @And("the response should contain the product name {string}")
    public void theResponseShouldContainTheProductName(String name) {
        assertTrue(testContext.lastResponse.body().contains("\"name\":\"" + name.toUpperCase() + "\""), "Response body was: " + testContext.lastResponse.body());
    }

    @Given("a product exists with name {string} and price {double}")
    public void aProductExistsWithNameAndPrice(String name, double price) throws Exception {
        theUserCreatesAProductWithNameAndPrice(name, price);
        assertEquals(201, testContext.lastResponse.statusCode());
    }

    @When("the user fetches the product by its ID")
    public void theUserFetchesTheProductByItsID() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + "/api/products/" + testContext.lastCreatedProductId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testContext.currentUserToken)
                .GET()
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @When("the user updates the product price to {double}")
    public void theUserUpdatesTheProductPriceTo(double newPrice) throws Exception {
        String jsonPayload = String.format("{\"name\": \"Cucumber Product\", \"price\": %s}", newPrice);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + "/api/products/" + testContext.lastCreatedProductId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testContext.currentUserToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @And("the response should reflect the updated price {double}")
    public void theResponseShouldReflectTheUpdatedPrice(double newPrice) {
        assertTrue(testContext.lastResponse.body().contains("\"price\":" + newPrice), "Response body was: " + testContext.lastResponse.body());
    }

    @When("the user deletes the product")
    public void theUserDeletesTheProduct() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + config.getPort() + "/api/products/" + testContext.lastCreatedProductId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testContext.currentUserToken)
                .DELETE()
                .build();
                
        testContext.lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
