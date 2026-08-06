Feature: Training management and workload notifications
  Creating a training publishes an ADD workload message; deleting the trainee
  (which cascades its trainings) publishes a DELETE. Invalid requests publish nothing.

  Background:
    Given a trainer "Nora Fit" specializing in CARDIO is registered
    And a trainee "Ken Gym" is registered
    And the trainer is logged in

  Scenario: Trainer creates a training successfully
    When the trainer creates a training named "Cardio A" on "2026-07-15" for 60 minutes
    Then the response status is 200
    And a workload ADD message is published for the trainer with 60 minutes on "2026-07-15"

  Scenario: A training with a non-positive duration is rejected and publishes nothing
    When the trainer creates a training named "Bad Session" on "2026-07-15" for 0 minutes
    Then the response status is 400
    And no workload message is published

  Scenario: A training for an unknown trainee is rejected and publishes nothing
    When the trainer creates a training named "Ghost Session" on "2026-07-15" for 45 minutes for an unknown trainee
    Then the response status is 400
    And no workload message is published

  Scenario: Deleting the trainee cascades a DELETE workload message
    When the trainer creates a training named "Cardio A" on "2026-07-15" for 60 minutes
    Then the response status is 200
    And a workload ADD message is published for the trainer with 60 minutes on "2026-07-15"
    When the trainee is deleted
    Then the response status is 200
    And a workload DELETE message is published for the trainer with 60 minutes on "2026-07-15"
