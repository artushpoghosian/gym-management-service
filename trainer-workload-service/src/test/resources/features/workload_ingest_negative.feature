Feature: Rejecting invalid or unauthenticated workload events
  Messages that fail validation or carry a bad JWT must never reach MongoDB.

  Scenario: A message missing the trainer username is not applied
    When a workload message with no trainer username arrives on "2026-07-15" for 60 minutes
    Then no workload documents are stored

  Scenario: A message with a non-positive duration is not applied
    When an ADD workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 0 minutes
    Then trainer "nora.fit" has no workload document

  Scenario: A message carrying an invalid JWT is discarded
    When an ADD workload message with an invalid token arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 60 minutes
    Then trainer "nora.fit" has no workload document
