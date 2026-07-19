Feature: Product CRUD API
  As an admin
  I want to be able to create, read, update, and delete products
  So that I can manage the inventory

  Scenario: Create a new product as an ADMIN
    Given the user "adminUser" has the role "ADMIN"
    When the user creates a product with name "Cucumber Product" and price 29.99
    Then the response status should be 201
    And the response should contain the product name "Cucumber Product"

  Scenario: Attempt to create a product as a regular user
    Given the user "regularUser" has the role "USER"
    When the user creates a product with name "Forbidden Product" and price 10.00
    Then the response status should be 403

  Scenario: Fetch the created product
    Given the user "adminUser" has the role "ADMIN"
    And a product exists with name "Cucumber Product" and price 29.99
    When the user fetches the product by its ID
    Then the response status should be 200
    And the response should contain the product name "Cucumber Product"

  Scenario: Update the product
    Given the user "adminUser" has the role "ADMIN"
    And a product exists with name "Cucumber Product" and price 29.99
    When the user updates the product price to 39.99
    Then the response status should be 200
    And the response should reflect the updated price 39.99

  Scenario: Delete the product
    Given the user "adminUser" has the role "ADMIN"
    And a product exists with name "Cucumber Product" and price 29.99
    When the user deletes the product
    Then the response status should be 204
    When the user fetches the product by its ID
    Then the response status should be 404
