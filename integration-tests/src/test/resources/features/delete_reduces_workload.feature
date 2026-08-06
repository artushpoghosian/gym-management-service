Feature: Deleting a trainee reduces workload across services
  Deleting a trainee cascades its trainings and publishes DELETE events that the
  workload service applies, reducing the trainer's minutes.

  Scenario: Deleting the trainee reduces the workload minutes
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    And a trainee "Ken Gym" is registered
    And the trainer is logged in
    When the trainer creates a training named "Cardio A" on "2026-07-15" for 60 minutes
    Then within 10 seconds the workload service reports 60 minutes for the trainer in 2026/7
    When the trainee is deleted
    Then within 10 seconds the workload service reports 0 minutes for the trainer in 2026/7
