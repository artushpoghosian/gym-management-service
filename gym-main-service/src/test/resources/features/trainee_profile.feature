Feature: Trainee profile retrieval
  An authenticated trainee can read a profile; an unknown target yields 404.

  Background:
    Given a trainee "Ken Gym" is registered
    And the trainee is logged in

  Scenario: Trainee reads their own profile
    When the trainee requests their own profile
    Then the response status is 200

  Scenario: Requesting an unknown trainee returns 404
    When the trainee requests the profile of "ghost.user"
    Then the response status is 404
