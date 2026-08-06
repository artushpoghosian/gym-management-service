Feature: Querying a trainer's workload summary
  The REST read endpoint returns a stored summary and 404s for an unknown trainer.

  Background:
    Given a valid service token
    And trainer "nora.fit" ("Nora" "Fit") already has 60 minutes in 2026/7

  Scenario: Fetching an existing trainer returns their workload
    When the workload summary for "nora.fit" is requested
    Then the response status is 200

  Scenario: Fetching an unknown trainer returns 404
    When the workload summary for "ghost.trainer" is requested
    Then the response status is 404
