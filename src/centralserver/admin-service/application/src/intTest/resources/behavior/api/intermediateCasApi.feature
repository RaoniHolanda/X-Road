@IntermediateCas
Feature: Intermediate CAS API

  Scenario: Adding OCSP responder to intermediate CAS
    Given Certification service is created
    And intermediate CAS added to certification service
    Then OCSP responder is added to intermediate CAS
