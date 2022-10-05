@CentralServer
@Member
Feature: Central Server Add Member

  Background:
    Given CentralServer login page is open
    And Page is prepared to be tested
    And User xrd logs in to CentralServer with password secret

  Scenario: Member is created and listed
    Given CentralServer Settings tab is selected
    And System settings tab is selected
    And A new member class E2E is added
    And Members tab is selected
    When A new member with name: E2E Test Member, code: e2e-test-member & member class: E2E is added
    Then A new member is listed
