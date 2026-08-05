Feature: Application Health Check
  In order to ensure the application is running correctly
  As a monitoring system
  I want to check the health status of the application

  Scenario: Health endpoint returns OK
    When I request the health endpoint "/health/liveness"
    Then the response status should be 200
    And the response body should contain "UP"
