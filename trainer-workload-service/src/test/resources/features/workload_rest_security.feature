Feature: Securing the workload REST API
  Every endpoint requires a valid Bearer token; missing or bad tokens are rejected.

  Scenario: Requesting a summary without a token is rejected
    When the workload summary for "nora.fit" is requested without a token
    Then the response status is 401

  Scenario: Requesting a summary with a tampered token is rejected
    When the workload summary for "nora.fit" is requested with a tampered token
    Then the response status is 401
