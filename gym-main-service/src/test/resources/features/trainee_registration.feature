Feature: Trainee registration
  New trainees can self-register; incomplete registrations are rejected.

  Scenario: Register a trainee successfully
    When a trainee is registered with first name "Ken" and last name "Gym"
    Then the response status is 200

  Scenario: Registration without a first name is rejected
    When a trainee is registered with first name "" and last name "Gym"
    Then the response status is 400

  Scenario: Registration without a last name is rejected
    When a trainee is registered with first name "Ken" and last name ""
    Then the response status is 400
