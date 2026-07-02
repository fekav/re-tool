 # Ingestion Slice Implementation Plan

  ## Summary

  Add a new atomic ingestion slice that starts the requirement lifecycle without persistence or
  workflow orchestration. POST /app/ingest accepts raw text, the REST layer assigns source metadata
  API-Request, and the application handler creates a Requirement, creates a Provenance entity,
  applies it to the aggregate, publishes the aggregate-raised RequirementIngestedEvent, and returns
  the same event.

  ## Public APIs And Domain Contracts

  - Add POST /app/ingest on the existing /app REST controller.
      - Request body: { "rawText": "..." }
      - The client cannot override source metadata in v1.
      - The controller defines an internal source metadata DTO and maps it to fixed domain metadata
        API-Request.

      - Response: HTTP 201 with RequirementIngestedEvent.

  - Add IngestRequirementCommand(rawText, sourceMetadata) implements
    Command<RequirementIngestedEvent> in io.fekav.req.ingestion.application.

  - Add RequirementIngestedEvent in req.shared.event with:
      - eventId
      - occurredAt
      - requirementId
      - rawText from the created Requirement
      - full provenance

  - Add shared domain types because Requirement already lives in req.shared.model:
      - OriginalText: preserves submitted text exactly, rejects null/blank, does not trim or
        normalize.

      - SourceMetadata: minimal typed v1 metadata with source API-Request.
      - Provenance: DDD entity with ElementId id, OriginalText originalText, SourceMetadata
        sourceMetadata, and Instant ingestedAt.

  ## Implementation Changes

  - Extend Requirement with a nullable internal Provenance field, a getter, and
    applyProvenance(Provenance).
      - applyProvenance rejects null provenance.
      - It rejects applying provenance twice.
      - It sets status to new RequirementStatus.INGESTED.
      - It creates, stores, and returns a RequirementIngestedEvent.

  - Keep text transformation out of ingestion.
      - The handler passes the submitted raw text into existing domain construction.
      - Ingestion must not trim, clean, normalize, classify, parse, or otherwise interpret the text.
      - Existing RawText behavior remains owned by RawText, not by the ingestion slice.

  - IngestRequirementCommandHandler flow:
      - Build OriginalText from command raw text.
      - Create Requirement using the existing aggregate factory.
      - Build Provenance with ElementId.create(), original text, source metadata, and one ingestion
        timestamp.

      - Call requirement.applyProvenance(provenance).
      - Publish the returned event via EventPublisher.publish(event).
      - Return the exact same event instance.

  - Use the provenance ingestedAt as the event occurredAt for RequirementIngestedEvent, so the
    receipt has one ingestion time.

  - Do not add repositories, Neo4j writes, schema changes, workflow orchestration, LLM contracts, or
    changes to existing classification/extraction handlers.

  ## Test Plan

  - Domain tests:
      - OriginalText preserves submitted text exactly and rejects null/blank.
      - SourceMetadata.apiRequest() yields API-Request.
      - Provenance requires id, original text, source metadata, and timestamp.
      - Requirement.applyProvenance stores provenance, sets status INGESTED, raises
        RequirementIngestedEvent, includes the created requirement’s RawText, preserves exact
        original text in provenance, and rejects duplicate provenance.

  - Application tests:
      - Handler returns RequirementIngestedEvent and publishes the same instance.
      - Event contains generated requirementId, requirement rawText, and provenance with exact
        original text plus API-Request.

      - Blank command raw text fails before publishing.

  - REST integration tests:
      - POST /app/ingest with valid raw text returns 201.
      - Response contains event id, requirement id, rawText, provenance originalText.text,
        provenance source API-Request, and non-null timestamps.

      - Blank or missing raw text returns 400.

  ## Assumptions

  - Use existing ElementId for Provenance.id.
  - Ingestion preserves submitted text in OriginalText; it does not own or assert text
    normalization.

  - /app/c remains unchanged; /app/ingest is the official v1 ingestion API.
  - Provenance supports only API-Request in v1; sender, filename, ChatUI, and file import metadata
    are future extensions.