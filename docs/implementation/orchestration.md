# Requirement Orchestration Implementation Plan

## Overview

This plan describes the first implementation of the requirement workflow
orchestrator. The orchestrator is implemented as its own slice under
`io.fekav.req.orchestration`.

The existing slices remain independently callable and atomic:

- `ingestion`
- `classification`
- `syntaxextraction`
- `conceptretrieval`
- `conceptmatching`

The orchestrator consumes application events published by these slices, stores
workflow events by `correlationId`, replays a workflow projection, and dispatches
the next required commands.

## Architecture Decisions

- The orchestrator is a separate slice: `src/main/java/io/fekav/req/orchestration`.
- `RequirementIngestedEvent` is the workflow start event.
- `CorrelationId` is a shared value object in `io.fekav.platform.messaging`.
- Existing workflow commands and events carry the same `correlationId`.
- No `RequirementId` is introduced in this workflow. A persisted requirement
  identity belongs to a later persist slice.
- `RequirementElement` remains id-free. Retrieval searches by `type` and `text`.
- No mapper abstraction is introduced initially. The orchestrator can derive
  `RequirementElement` values directly from `Action`.
- `EventStore` is an orchestration port.
- `InMemoryEventStore` is the first adapter.
- `WorkflowState` is replayed from events and is not stored separately.
- V1 stores consumed workflow events only.
- CDI event consumption is guarded by `req.orchestration.enabled`. The default
  is `false` so existing slice entrypoints remain atomically callable unless
  orchestration is explicitly enabled for a runtime.

## Event Flow

```text
RequirementIngestedEvent
  -> append event
  -> dispatch ClassifyRequirementCommand
  -> dispatch ExtractSyntaxCommand

RequirementClassifiedEvent
  -> append event
  -> replay WorkflowState
  -> maybe complete

RequirementElementsExtractedEvent
  -> append event
  -> derive RequirementElement values from Action
  -> dispatch RetrieveCandidateConceptsCommand for every extracted RequirementElement

ConceptCandidatesRetrievedEvent
  -> append event
  -> dispatch EvaluateConceptMatchCommand for the retrieved CandidateConceptMatch

ConceptMatchEvaluatedEvent
  -> append event
  -> replay WorkflowState
  -> maybe publish RequirementAnalysisCompletedEvent
```

## Completion Event

`RequirementAnalysisCompletedEvent` is the handoff event for the later persist
slice.

The initial payload should contain:

- `correlationId`
- `provenance`
- `classification`
- `action`
- concept match results as pairs of `CandidateConceptMatch` and
  `ConceptMatchDecision`

The event must not contain a `RequirementId` and must not decide persistence,
graph writes, duplicate handling, similarity handling, or contradiction
handling.

## Phase 1: Correlation

### Task 1: Add CorrelationId

**Description:** Introduce a workflow correlation value object that can be used
before a persistent requirement identity exists.

**Files likely touched:**

- `src/main/java/io/fekav/platform/messaging/CorrelationId.java`
- `src/test/java/io/fekav/platform/messaging/CorrelationIdTest.java`

**Acceptance criteria:**

- `CorrelationId.create()` creates a UUID-based id.
- `CorrelationId` rejects null values.
- The type has no dependency on `RequirementId`.

**Verification:**

- `./gradlew test --tests "*CorrelationIdTest"`

**Dependencies:** None

**Estimated scope:** Small

### Task 2: Add Correlation To Workflow Commands And Events

**Description:** Extend all workflow-relevant commands and events so the
orchestrator can correlate independently published events.

**Files likely touched:**

- `src/main/java/io/fekav/req/shared/event/RequirementIngestedEvent.java`
- `src/main/java/io/fekav/req/shared/event/RequirementClassifiedEvent.java`
- `src/main/java/io/fekav/req/shared/event/RequirementElementsExtractedEvent.java`
- `src/main/java/io/fekav/req/shared/event/ConceptCandidatesRetrievedEvent.java`
- `src/main/java/io/fekav/req/shared/event/ConceptMatchEvaluatedEvent.java`
- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommand.java`
- `src/main/java/io/fekav/req/syntaxextraction/application/ExtractSyntaxCommand.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommand.java`
- `src/main/java/io/fekav/req/conceptmatching/application/EvaluateConceptMatchCommand.java`
- related tests under `src/test/java/io/fekav/req`

**Acceptance criteria:**

- `RequirementIngestedEvent` creates a fresh `correlationId`.
- Handler-created events preserve the command `correlationId`.
- Atomic command handlers remain directly callable.
- No workflow event introduces `RequirementId`.

**Verification:**

- `./gradlew test --tests "*RequirementReceiptApplicationEventTest"`
- `./gradlew test --tests "*ConceptApplicationEventTest"`

**Dependencies:** Task 1

**Estimated scope:** Medium

## Checkpoint 1: Correlation

- All adjusted command and event tests pass.
- The existing slices still compile and remain independently callable.

## Phase 2: Orchestration Foundation

### Task 3: Add EventStore Port And InMemoryEventStore Adapter

**Description:** Add the event storage abstraction used by the orchestrator and
provide the first in-process implementation.

**Files likely touched:**

- `src/main/java/io/fekav/req/orchestration/application/EventStore.java`
- `src/main/java/io/fekav/req/orchestration/infrastructure/InMemoryEventStore.java`
- `src/test/java/io/fekav/req/orchestration/infrastructure/InMemoryEventStoreTest.java`

**Acceptance criteria:**

- `appendIfAbsent(ApplicationEvent)` stores events by the event's correlation
  id and event id.
- `appendIfAbsent(ApplicationEvent)` returns whether the event was newly stored.
- `load(CorrelationId)` returns events in append order.
- `load(CorrelationId)` returns an immutable copy.
- The in-memory adapter is safe for the initial synchronous in-process workflow.

**Verification:**

- `./gradlew test --tests "*InMemoryEventStoreTest"`

**Dependencies:** Task 1

**Estimated scope:** Small

### Task 4: Add RequirementElementCollector And WorkflowState

**Description:** Implement replay-based workflow state and the small translation
from extracted `Action` to retrieval `RequirementElement` values.

**Files likely touched:**

- `src/main/java/io/fekav/req/orchestration/domain/RequirementElementCollector.java`
- `src/main/java/io/fekav/req/orchestration/domain/WorkflowState.java`
- `src/test/java/io/fekav/req/orchestration/domain/RequirementElementCollectorTest.java`
- `src/test/java/io/fekav/req/orchestration/domain/WorkflowStateTest.java`

**Acceptance criteria:**

- `RequirementElementCollector` emits `SUBJECT`, `ACTION`, `OBJECT`,
  `CONDITION`, and `CONSTRAINT` elements from `Action`.
- `WorkflowState.replay(...)` derives current workflow status from events.
- Replay can derive how many retrieval results are expected from the extracted
  action.
- Replay can derive how many match decisions are expected from received
  retrieval results.
- Replay can determine when the workflow is complete.

**Verification:**

- `./gradlew test --tests "*RequirementElementCollectorTest"`
- `./gradlew test --tests "*WorkflowStateTest"`

**Dependencies:** Task 2

**Estimated scope:** Medium

## Checkpoint 2: Foundation

- EventStore tests pass.
- WorkflowState replay tests pass.
- Element collection tests pass.

## Phase 3: Orchestrator

### Task 5: Add RequirementWorkflowOrchestrator

**Description:** Implement the event-consuming orchestrator that appends events,
replays state, and dispatches follow-up commands.

**Files likely touched:**

- `src/main/java/io/fekav/req/orchestration/application/RequirementWorkflowOrchestrator.java`
- `src/test/java/io/fekav/req/orchestration/application/RequirementWorkflowOrchestratorTest.java`

**Acceptance criteria:**

- On `RequirementIngestedEvent`, the orchestrator appends the event and
  dispatches one classification command and one syntax extraction command.
- On `RequirementElementsExtractedEvent`, the orchestrator derives
  `RequirementElement` values from `Action` and dispatches retrieval for every
  extracted element.
- Retrieval is dispatched for `SUBJECT`, `ACTION`, `OBJECT`, every
  `CONDITION`, and every `CONSTRAINT` emitted by the collector.
- On `ConceptCandidatesRetrievedEvent`, the orchestrator dispatches concept
  matching for the event's `CandidateConceptMatch`. The current matching slice
  evaluates the candidate list for one requirement element and returns one
  `ConceptMatchDecision`.
- On `RequirementClassifiedEvent` and `ConceptMatchEvaluatedEvent`, the
  orchestrator appends events and checks completion.
- If `appendIfAbsent(...)` reports that an event was already stored, the
  orchestrator does not dispatch follow-up commands for that re-delivered event.
- The CDI observer path is inactive unless `req.orchestration.enabled=true`.

**Verification:**

- `./gradlew test --tests "*RequirementWorkflowOrchestratorTest"`

**Dependencies:** Tasks 3, 4

**Estimated scope:** Medium

### Task 6: Add RequirementAnalysisCompletedEvent

**Description:** Add the event that represents a completed analysis workflow and
hands the result to the future persist slice.

**Files likely touched:**

- `src/main/java/io/fekav/req/shared/event/RequirementAnalysisCompletedEvent.java`
- `src/main/java/io/fekav/req/shared/model/ConceptMatchResult.java`
- `src/test/java/io/fekav/req/shared/event/RequirementAnalysisCompletedEventTest.java`

**Acceptance criteria:**

- The completion event carries `correlationId`, `provenance`,
  `classification`, `action`, and concept match results.
- Concept match results preserve both `CandidateConceptMatch` and
  `ConceptMatchDecision`.
- The completion event contains no `RequirementId`.
- The completion event does not perform or encode graph persistence decisions.

**Verification:**

- `./gradlew test --tests "*RequirementAnalysisCompletedEventTest"`

**Dependencies:** Task 4

**Estimated scope:** Small

### Task 7: Add In-Process Workflow Test

**Description:** Verify the first complete in-process orchestration path with
the in-memory event store and command dispatching.

**Files likely touched:**

- `src/test/java/io/fekav/req/orchestration/application/RequirementWorkflowIntegrationTest.java`
- possible test doubles under `src/test/java/io/fekav/req/orchestration`

**Acceptance criteria:**

- A `RequirementIngestedEvent` starts the workflow.
- Classification and syntax extraction are dispatched.
- Every extracted `RequirementElement` triggers retrieval.
- Every `ConceptCandidatesRetrievedEvent` triggers matching for its
  `CandidateConceptMatch`.
- A complete workflow publishes `RequirementAnalysisCompletedEvent`.
- Re-delivery of the same event id does not duplicate follow-up command
  dispatches.

**Verification:**

- `./gradlew test --tests "*RequirementWorkflowIntegrationTest"`
- `./gradlew test`

**Dependencies:** Tasks 5, 6

**Estimated scope:** Medium

## Checkpoint 3: Complete Orchestration

- All orchestration tests pass.
- The full test suite passes with `./gradlew test`.
- Existing slices remain atomically callable.
- Orchestrator behavior is idempotent for repeated in-process events.

## Risks And Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Synchronous CDI events can be reentrant | Medium | Append consumed events before dispatching follow-up commands. |
| Events may be delivered more than once | Medium | Use `appendIfAbsent(...)` to skip already stored event ids. |
| V1 does not record planned-but-not-dispatched work | Medium | Accept this as a v1 tradeoff; a later durable workflow can add an outbox. |
| Completion criteria can drift as new slices appear | Medium | Keep completion logic inside `WorkflowState` tests. |
| Completion payload may become too large | Low | Extract stable result value objects, starting with `ConceptMatchResult`. |
| In-memory storage is not durable | Low for v1 | Keep `EventStore` as a port so a durable adapter can replace it later. |

## Explicitly Out Of Scope

- Provenance modeling beyond consuming the existing `RequirementIngestedEvent`.
- Persistent requirement identity.
- Graph writes.
- Duplicate, similarity, and contradiction decisions.
- Durable messaging infrastructure.
- Durable event store adapter.
- Human-review outcomes for uncertain concept matches.

## Implementation Order

1. Add `CorrelationId`.
2. Add `correlationId` to workflow commands and events.
3. Add `EventStore` and `InMemoryEventStore`.
4. Add `RequirementElementCollector` and `WorkflowState`.
5. Add `RequirementWorkflowOrchestrator`.
6. Add `RequirementAnalysisCompletedEvent`.
7. Add the in-process workflow test.
