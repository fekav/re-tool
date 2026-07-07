# Classification Slice

The Classification slice assigns a business classification to raw requirement
text. It does not decide which terms will later become Knowledge Graph nodes.
Instead, it identifies the requirement engineering role of the text and the
primary kind of statement it carries.

## Business Responsibility

A classification has two independent axes:

- `RequirementType`: the business role and commitment level of the text.
- `RequirementProperty`: the primary character of the statement.

This separation matters because a goal, a need, and a binding requirement can
each be functional or quality-related. A performance goal is therefore not a
separate requirement type; it is a `GOAL` with property `QUALITY`.

## Requirement Type

`RequirementType` contains the supported business roles:

- `GOAL`: a desired outcome or business/product objective. The text explains
  why a change matters, but is typically not testable as one concrete system
  behavior.
- `NEED`: a stakeholder need or capability gap. The text describes what someone
  needs before it is expressed as a binding system obligation.
- `REQUIREMENT`: a binding product or system obligation. The text assigns a
  verifiable obligation to a system, service, product, component, or team.

The enum values are defined in
`src/main/java/io/fekav/req/classification/domain/RequirementType.java` and are
also listed as allowed values in the AI output schema.

## Requirement Property

`RequirementProperty` describes the second classification axis:

- `FUNCTIONAL`: behavior, capability, workflow, operation, or interaction.
- `QUALITY`: quality attribute or constraint, such as performance, security,
  availability, usability, reliability, compliance, or scalability.

For mixed statements, the business rule is: prefer `QUALITY` when the quality
constraint is the distinguishing obligation; otherwise prefer `FUNCTIONAL`.

The allowed values are defined in
`src/main/java/io/fekav/req/classification/domain/RequirementProperty.java`.

## Classification Result

`Classification` bundles the classification result:

- `conceptType`: the `RequirementType`.
- `property`: the `RequirementProperty`.
- `confidenceScore`: classification confidence between `0.0` and `1.0`.
- `rationale`: a short business rationale.

The domain model requires all four pieces of information. `ConfidenceScore`
accepts only values in the inclusive range from `0.0` to `1.0`. `Rationale`
must not be blank and is stripped.

Relevant classes:

- `src/main/java/io/fekav/req/classification/domain/Classification.java`
- `src/main/java/io/fekav/req/classification/domain/ConfidenceScore.java`
- `src/main/java/io/fekav/req/classification/domain/Rationale.java`

Low confidence is not a technical failure. It remains part of the business
result so downstream processes can identify uncertain classifications and route
them for review when needed.

## Decision Rules

The business decision rules are encoded in the prompt used by
`LlmRequirementClassificationService` and covered by tests. The main rules are:

- Intent and commitment level determine the `RequirementType`.
- The primary business concern determines the `RequirementProperty`.
- Keywords are evidence cues, not sufficient rules by themselves.
- Stakeholder-centered wording points toward `NEED`.
- Verifiable system or product obligations point toward `REQUIREMENT`.
- Broad outcome wording points toward `GOAL`.
- Confidence must not be inflated when signals are weak or mixed.

The concrete prompt rules, tie-breakers, and few-shot examples are in
`src/main/java/io/fekav/req/classification/infrastructure/LlmRequirementClassificationService.java`.
Fixture-backed classification cases are in
`src/test/resources/fixtures/requirement-classification-cases.json`.

## Processing

The slice is invoked through `ClassifyRequirementCommand`. The command contains
the raw text and a `CorrelationId` so the classification can be traced through
the wider workflow.

`ClassifyRequirementCommandHandler`:

1. normalizes the raw text as `RawText`,
2. calls `ClassificationService.classifyRequirement(...)`,
3. creates a `RequirementClassifiedEvent`,
4. publishes that event.

Relevant classes:

- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommand.java`
- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommandHandler.java`
- `src/main/java/io/fekav/req/classification/application/ClassificationService.java`
- `src/main/java/io/fekav/req/shared/event/RequirementClassifiedEvent.java`

## AI Output and Validation

LLM output is not accepted directly as business truth. The infrastructure
expects structured JSON, validates required fields, and only then translates
the output into domain objects.

The expected output has this business shape:

```json
{
  "classification": {
    "conceptType": "REQUIREMENT",
    "property": "QUALITY",
    "confidenceScore": 0.95,
    "rationale": "The text assigns a verifiable performance constraint to the page."
  }
}
```

The JSON schema is located at
`src/main/resources/contracts/ai/v1/requirement-classification.schema.json`.
The DTOs used for translation are:

- `src/main/java/io/fekav/req/classification/infrastructure/RequirementClassificationOutput.java`
- `src/main/java/io/fekav/req/classification/infrastructure/RequirementClassificationFieldsOutput.java`

`RequirementClassificationOutput` checks that enum values are supported, that
confidence fits the domain range, and that a business rationale is present.

## Scope Boundary

The slice classifies the whole requirement text only. It does not extract
subjects, actions, objects, conditions, or constraints, and it does not decide
Graph node mappings. Those responsibilities belong to the downstream
Extraction, Resolution, Review, and Graph Change slices.

The Classification slice result is therefore business context for the wider
workflow: what kind of requirement is present, how certain that classification
is, and which evidence supports it.

## Technical Interface

Command endpoint:

```http
POST /app/c
```

Request body:

```json
{
  "command": "ClassifyRequirementCommand",
  "payload": {
    "rawText": "The checkout page must load within 2 seconds on a 4G connection."
  }
}
```

Published event and command response:

- `RequirementClassifiedEvent`

Response shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "rawText": {
    "text": "The checkout page must load within 2 seconds on a 4G connection."
  },
  "classification": {
    "conceptType": "REQUIREMENT",
    "property": "QUALITY",
    "confidenceScore": { "value": 0.95 },
    "rationale": {
      "text": "The text assigns a verifiable performance constraint to the page."
    }
  }
}
```
