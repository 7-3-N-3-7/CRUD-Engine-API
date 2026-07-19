Feature: Sample Cucumber integration
  As a developer
  I want to have Cucumber integrated into the project
  So that I can write BDD tests

  Scenario: Application context loads and steps execute
    Given the application is running
    When I trigger a sample step
    Then it should complete successfully
