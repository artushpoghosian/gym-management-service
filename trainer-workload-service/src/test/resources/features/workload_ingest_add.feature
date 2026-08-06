Feature: Ingesting ADD workload events
  A valid ADD message from the main service is applied to the trainer's monthly
  workload in MongoDB, accumulating within a month and splitting across months.

  Scenario: An ADD message for a new trainer creates a workload document
    When an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 60 minutes
    Then within 5 seconds trainer "nora.fit" has 60 minutes in 2026/7

  Scenario: Two ADD messages in the same month accumulate
    When an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 60 minutes
    And an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-20" for 30 minutes
    Then within 5 seconds trainer "nora.fit" has 90 minutes in 2026/7

  Scenario: ADD messages in different months are stored in separate buckets
    When an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 60 minutes
    And an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-08-05" for 45 minutes
    Then within 5 seconds trainer "nora.fit" has 60 minutes in 2026/7
    And within 5 seconds trainer "nora.fit" has 45 minutes in 2026/8
