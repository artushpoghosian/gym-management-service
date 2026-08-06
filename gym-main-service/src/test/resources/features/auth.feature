Feature: Authentication
  Registered users must be able to obtain a JWT, and requests with bad or missing
  credentials must be rejected by the security layer.

  Scenario: Registered trainer logs in successfully
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    When the user logs in with the trainer's generated credentials
    Then the response status is 200
    And the response contains a JWT token

  Scenario: Login with a wrong password is rejected
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    When the user logs in as that trainer with password "wrong-password"
    Then the response status is 401

  Scenario: A protected endpoint without a token is rejected
    When a protected endpoint is called without a token
    Then the response status is 401

  Scenario: A protected endpoint with a tampered token is rejected
    When a protected endpoint is called with a tampered token
    Then the response status is 401

  Scenario: A protected endpoint with an expired token is rejected
    When a protected endpoint is called with an expired token
    Then the response status is 401
