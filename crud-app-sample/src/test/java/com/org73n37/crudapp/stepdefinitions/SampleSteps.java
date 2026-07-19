package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SampleSteps {

    @Given("the application is running")
    public void theApplicationIsRunning() {
        // SpringBootTest will boot the context. We verify it didn't crash.
    }

    @When("I trigger a sample step")
    public void iTriggerASampleStep() {
        // Just a dummy step
    }

    @Then("it should complete successfully")
    public void itShouldCompleteSuccessfully() {
        Assertions.assertTrue(true, "Cucumber step executed correctly");
    }
}
