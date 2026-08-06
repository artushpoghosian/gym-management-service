Feature: End-to-end training to workload
  A training created in the main service is delivered over ActiveMQ and reflected
  in the trainer-workload service's summary.

  Scenario: A created training appears in the workload summary
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    And a trainee "Ken Gym" is registered
    And the trainer is logged in
    When the trainer creates a training named "Cardio A" on "2026-07-15" for 60 minutes
    Then the response status is 200
    And within 10 seconds the workload service reports 60 minutes for the trainer in 2026/7
