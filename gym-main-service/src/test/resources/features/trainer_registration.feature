Feature: Trainer registration
  New trainers can self-register with a valid specialization; bad input is rejected.

  Scenario: Register a trainer successfully
    When a trainer is registered with first name "Nora" and last name "Fit" and specialization "CARDIO"
    Then the response status is 200

  Scenario: Registration without a first name is rejected
    When a trainer is registered with first name "" and last name "Fit" and specialization "CARDIO"
    Then the response status is 400

  Scenario: Registration with an unknown specialization is rejected
    When a trainer is registered with first name "Nora" and last name "Fit" and specialization "SWIMMING"
    Then the response status is 400

  Scenario: Registration without a specialization is rejected
    When a trainer is registered without a specialization
    Then the response status is 400
