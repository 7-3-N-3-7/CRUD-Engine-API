package com.org73n37.crudapp.stepdefinitions;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;

@Component
@ScenarioScope
public class TestContext {
    public HttpResponse<String> lastResponse;
    public String lastCreatedProductId;
    public String currentUserToken;
}
