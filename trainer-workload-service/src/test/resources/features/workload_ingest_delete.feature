Feature: Ingesting DELETE workload events
  A DELETE message subtracts minutes from the stored month, and the atomic
  pipeline update never lets a bucket go negative.

  Scenario: A DELETE message reduces the stored minutes
    Given trainer "nora.fit" ("Nora" "Fit") already has 90 minutes in 2026/7
    When a DELETE workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 30 minutes
    Then within 5 seconds trainer "nora.fit" has 60 minutes in 2026/7

  Scenario: A DELETE never drives the stored minutes below zero
    Given trainer "nora.fit" ("Nora" "Fit") already has 30 minutes in 2026/7
    When a DELETE workload message arrives for "nora.fit" ("Nora" "Fit") on "2026-07-15" for 90 minutes
    Then within 5 seconds trainer "nora.fit" has 0 minutes in 2026/7
