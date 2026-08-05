Feature: Asynchronous HTML Ingestion
  As a content creator
  I want to submit raw HTML asynchronously
  So that it can be sanitized and processed in the background

  Scenario: Ingest valid HTML
    Given the user "adminUser" has the role "ADMIN"
    When I submit raw HTML "<p>Hello World</p>" with slug "test-ingest-valid"
    Then the response status should be 202
    And the HTML processing should eventually complete for slug "test-ingest-valid"

  Scenario: Ingest invalid HTML
    Given the user "adminUser" has the role "ADMIN"
    When I submit raw HTML "<form>Test</form>" with slug "test-ingest-invalid"
    Then the response status should be 202
    And the HTML processing should eventually fail for slug "test-ingest-invalid"
