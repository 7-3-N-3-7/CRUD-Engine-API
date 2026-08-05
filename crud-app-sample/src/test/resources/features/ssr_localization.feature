Feature: SSR and Localization
  As a client
  I want to fetch server-side rendered pages with translations
  So that I can view the UI in my preferred language

  Scenario: Fetch page with unsupported language defaults to English
    Given a processed HTML page exists with slug "home" and content "<h1>{{title}}</h1>"
    And an English translation exists for "home" with title "Welcome Home"
    When I request the page "/pages/home" with Accept-Language "es"
    Then the response status should be 200
    And the response body should contain "<h1>Welcome Home</h1>"

  Scenario: Fetch page with supported language
    Given a processed HTML page exists with slug "home" and content "<h1>{{title}}</h1>"
    And a French translation exists for "home" with title "Bienvenue"
    When I request the page "/pages/home" with Accept-Language "fr"
    Then the response status should be 200
    And the response body should contain "<h1>Bienvenue</h1>"
