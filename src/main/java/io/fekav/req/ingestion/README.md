# Ingestion Slice

The Ingestion slice records the original requirement text as the start of a
traceable requirement analysis. Its responsibility is not classification,
extraction, node resolution, or graph persistence. It creates provenance and
opens the workflow for the downstream slices.

## Domain Responsibility

Ingestion treats the submitted text as the source statement that all later
business facts must remain traceable to. The slice creates a `Provenance`
object with:

- an `ElementId` for the provenance record,
- the original text as `OriginalText`,
- source metadata from `SourceMetadata.apiRequest()`,
- the ingestion timestamp.

The source metadata currently identifies API input as `API-Request`.

Relevant classes:

- `src/main/java/io/fekav/req/ingestion/application/IngestRequirementCommand.java`
- `src/main/java/io/fekav/req/ingestion/application/IngestRequirementCommandHandler.java`
- `src/main/java/io/fekav/req/shared/model/Provenance.java`
- `src/main/java/io/fekav/req/shared/model/OriginalText.java`
- `src/main/java/io/fekav/req/shared/model/SourceMetadata.java`

## Processing

`IngestRequirementCommandHandler` validates and wraps the submitted text as
`OriginalText`, creates `Provenance`, publishes a `RequirementIngestedEvent`,
and then waits for the final workflow outcome through `IngestionOutcomeStore`.

The handler returns an `IngestRequirementResult`, not the raw event. This keeps
the API response business-facing while the event remains the workflow signal.

Possible ingestion outcomes are:

- `RECORDED`: the requirement was recorded as a new graph change.
- `ALREADY_EXISTS`: the same requirement or assertion is already known.
- `REVIEW_REQUIRED`: downstream node resolution needs human input.
- `INTERNAL_ERROR`: publication or downstream outcome recording failed.

Relevant classes:

- `src/main/java/io/fekav/req/ingestion/application/IngestRequirementResult.java`
- `src/main/java/io/fekav/req/ingestion/application/IngestionOutcomeStore.java`
- `src/main/java/io/fekav/req/shared/event/RequirementIngestedEvent.java`

## Workflow Contract

The slice publishes `RequirementIngestedEvent`. The Orchestration slice consumes
that event and dispatches classification and syntax extraction with the same
correlation id.

`IngestionOutcomeStore` connects the asynchronous workflow back to the command
response. Graph Change records `RECORDED` or `ALREADY_EXISTS`; Orchestration
records `REVIEW_REQUIRED` when node resolution opens a review.

## Scope Boundary

Ingestion preserves the source requirement and provenance only. It does not
interpret the sentence, classify intent, extract semantic parts, resolve graph
nodes, or decide whether the requirement is new. Those decisions are owned by
downstream slices.

## Tests

The business behavior is covered by:

- `src/test/java/io/fekav/req/ingestion/application/IngestRequirementCommandHandlerTest.java`
- `src/test/java/io/fekav/platform/api/IngestRequirementCommandRestControllerTestIT.java`

The tests verify provenance creation, API source metadata, result statuses,
failure handling, and the public command response shape.

## Technical Interface

Command endpoint:

```http
POST /app/c
```

Request body:

```json
{
  "command": "IngestRequirementCommand",
  "payload": {
    "originalText": "The checkout service must support guest checkout."
  }
}
```

Published event:

- `RequirementIngestedEvent`

Consumed indirectly through `IngestionOutcomeStore`:

- `IngestRequirementResult.recorded(...)`
- `IngestRequirementResult.alreadyExists(...)`
- `IngestRequirementResult.reviewRequired(...)`
- `IngestRequirementResult.internalError(...)`

Command responses:

Recorded:

```json
{
  "correlationId": { "value": "..." },
  "status": "RECORDED",
  "message": "Requirement recorded."
}
```

Already exists:

```json
{
  "correlationId": { "value": "..." },
  "status": "ALREADY_EXISTS",
  "message": "Requirement already exists."
}
```

Review required:

```json
{
  "correlationId": { "value": "..." },
  "status": "REVIEW_REQUIRED",
  "message": "Requirement requires review."
}
```

Internal error:

```json
{
  "correlationId": { "value": "..." },
  "status": "INTERNAL_ERROR",
  "message": "Requirement ingestion failed internally."
}
```

When no final workflow outcome was recorded, the internal-error message is:

```json
{
  "correlationId": { "value": "..." },
  "status": "INTERNAL_ERROR",
  "message": "No final ingestion outcome was recorded."
}
```

The response intentionally omits event metadata and provenance. Those details
are carried by `RequirementIngestedEvent`.
