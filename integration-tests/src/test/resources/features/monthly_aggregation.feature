Feature: Monthly workload aggregation across services
  Multiple trainings accumulate within a month and split across months, end to end.

  Scenario: Trainings sum per month and split across months
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    And a trainee "Ken Gym" is registered
    And the trainer is logged in
    When the trainer creates a training named "Cardio A" on "2026-07-15" for 60 minutes
    And the trainer creates a training named "Cardio B" on "2026-07-20" for 30 minutes
    And the trainer creates a training named "Cardio C" on "2026-08-05" for 45 minutes
    Then within 10 seconds the workload service reports 90 minutes for the trainer in 2026/7
    And within 10 seconds the workload service reports 45 minutes for the trainer in 2026/8
