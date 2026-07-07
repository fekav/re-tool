# Extraction Slice

The Extraction slice turns raw requirement text into a structured linguistic
statement. It identifies the core Subject-Predicate-Object statement and the
qualifiers that constrain or activate that statement.

## Business Responsibility

The slice extracts one `Action` from a requirement:

- `Subject`: the grammatical subject, meaning who or what performs, owns, or is
  responsible for the predicate.
- `actionText`: the full predicate text, including modal or auxiliary verbs
  such as `must`, `shall`, `muss`, or `soll`.
- `TargetObject`: the object affected, created, read, notified, stored, or
  governed by the predicate.
- `Condition`: when, if, or under which trigger the whole predication applies.
- `Constraint`: how, how fast, how often, in which format, through which
  channel, or under which boundary the action must be fulfilled.

`Subject`, `actionText`, and `TargetObject` are mandatory. Conditions and
constraints are optional sets.

Relevant classes:

- `src/main/java/io/fekav/req/extraction/domain/Action.java`
- `src/main/java/io/fekav/req/extraction/domain/Subject.java`
- `src/main/java/io/fekav/req/extraction/domain/TargetObject.java`
- `src/main/java/io/fekav/req/extraction/domain/Condition.java`
- `src/main/java/io/fekav/req/extraction/domain/Constraint.java`

## Decision Rules

The extraction prompt in `LlmSyntaxExtraction` carries the detailed business
rules:

- Extract `SUBJECT`, `ACTION`, and `OBJECT` from the main predication.
- Scan remaining phrases before, inside, and after the main predication.
- Put triggers, preconditions, states, events, and activation windows into
  `CONDITION`.
- Put limits, deadlines, durations, formats, channels, quality levels, manner
  words, permissions, locations, and target groups into `CONSTRAINT`.
- Do not drop a qualifier just because the core sentence is already complete.
- Combine multiple conditions or constraints in reading order.
- Normalize extracted values to lowercase and singular form.
- Keep modal or auxiliary verbs together with the action verb.

Tie-breakers are explicit: temporal triggers such as "after login" are
conditions; fulfillment limits such as "within 10 seconds" are constraints.

Relevant class:

- `src/main/java/io/fekav/req/extraction/infrastructure/LlmSyntaxExtraction.java`

## Processing

`ExtractSyntaxCommandHandler` receives raw text, wraps it as `RawText`, calls
`SyntaxExtraction.extractSyntax(...)`, creates a
`RequirementElementsExtractedEvent`, and publishes it.

Relevant classes:

- `src/main/java/io/fekav/req/extraction/application/ExtractSyntaxCommand.java`
- `src/main/java/io/fekav/req/extraction/application/ExtractSyntaxCommandHandler.java`
- `src/main/java/io/fekav/req/extraction/application/SyntaxExtraction.java`
- `src/main/java/io/fekav/req/shared/event/RequirementElementsExtractedEvent.java`

## AI Output and Validation

LLM output is expected as structured JSON with `SUBJECT`, `ACTION`, `OBJECT`,
`CONDITION`, and `CONSTRAINT` fields. Required fields are validated before
mapping to domain objects. Blank condition or constraint values become empty
sets.

The schema is located at:

- `src/main/resources/contracts/ai/v1/requirement-syntax.schema.json`

DTOs used for translation:

- `src/main/java/io/fekav/req/extraction/infrastructure/SyntaxExtractionOutput.java`
- `src/main/java/io/fekav/req/extraction/infrastructure/RequirementElementsOutput.java`

## Scope Boundary

Extraction does not classify the requirement, resolve graph nodes, or persist
anything. It produces the linguistic elements that Orchestration later turns
into `RequirementElement` values for Resolution.

## Tests

The behavior is covered by:

- `src/test/java/io/fekav/req/syntaxextraction/application/ExtractEntitiesCommandHandlerTest.java`
- `src/test/java/io/fekav/req/syntaxextraction/domain/*Test.java`
- `src/test/java/io/fekav/req/syntaxextraction/infrastructure/LlmSyntaxExtractionTest.java`
- `src/test/java/io/fekav/platform/api/ExtractSyntaxCommandRestControllerTestIT.java`

The test package still uses the older `syntaxextraction` name, but the tested
production code is the current `io.fekav.req.extraction` slice.

## Technical Interface

Command endpoint:

```http
POST /app/c
```

Request body:

```json
{
  "command": "ExtractSyntaxCommand",
  "payload": {
    "rawText": "If a customer cancels an order before shipment, the commerce system must refund the payment within 24 hours."
  }
}
```

Published event and command response:

- `RequirementElementsExtractedEvent`

Response shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "rawText": {
    "text": "If a customer cancels an order before shipment, the commerce system must refund the payment within 24 hours."
  },
  "action": {
    "id": { "value": "..." },
    "actionText": "must refund",
    "subject": {
      "id": { "value": "..." },
      "text": "commerce system"
    },
    "targetObject": {
      "id": { "value": "..." },
      "text": "payment"
    },
    "conditions": [
      { "text": "customer cancels an order before shipment" }
    ],
    "constraints": [
      { "text": "within 24 hours" }
    ]
  }
}
```
