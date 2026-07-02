# Requirement Workflow Orchestration

## Purpose

This document captures the current planning baseline for the requirement
workflow orchestrator. The orchestrator is the binding component between the
existing atomic slices:

- `classification`
- `syntaxextraction`
- `conceptretrieval`
- `conceptmatching`

The slices should remain independently callable. The orchestrator consumes the
events published by these slices and dispatches follow-up commands where the
workflow requires it.

## Scope

This plan focuses only on orchestration.

Out of scope for this plan:

- ingestion and provenance modeling
- persistent requirement identity
- graph writes
- duplicate, similarity, and contradiction decisions
- durable messaging infrastructure
- final persistence into Neo4j

An ingestion slice is expected later as the official workflow entry point. That
slice can publish the first workflow event and provide provenance. The
orchestrator design should be ready to consume that event, but this document
does not specify ingestion itself.

## Core Decisions

Use `correlationId` for workflow correlation.

The workflow needs an identifier before a persisted `RequirementId` exists.
This identifier is not a graph identity and not a persistent requirement
identity. It only connects all commands and events that belong to the same
running workflow.

Do not introduce `RequirementId` into the existing atomic slices.

The system should first classify, extract, retrieve candidates, evaluate matches,
and decide whether a requirement may be persisted. A persisted requirement id
belongs to a later persist slice.

Keep `RequirementElement` id-free.

Retrieval intentionally searches by `type` and `text`. The purpose is to find
whether a concrete element from an incoming requirement, such as "Login-Formular",
already maps to a graph concept. Element ids are not needed for this lookup.

Do not introduce a mapper abstraction initially.

The orchestrator can read the extracted `Action` and create the
`RequirementElement` values it needs to dispatch retrieval commands. If this
translation grows more complex, it can be extracted later.

Use an `EventStore` port and an `InMemoryEventStore` adapter for the first
implementation.

This keeps the solution hexagonal and simple while leaving a path toward a
durable event store. The first adapter may be an in-memory implementation keyed
by `correlationId`.

Use `WorkflowState` as a replayed projection, not as separately persisted state.

The event store records immutable workflow events. `WorkflowState` is rebuilt
from those events to answer orchestration questions such as which steps have
completed and which follow-up commands still need to be dispatched.

## Event Store And Workflow State

The event store and workflow state are different concepts.

```text
EventStore
  stores workflow events by correlationId

WorkflowState
  derives the current workflow status by replaying stored events
```

The first port can stay general:

```java
interface EventStore {
    void append(CorrelationId correlationId, ApplicationEvent event);
    List<ApplicationEvent> load(CorrelationId correlationId);
}
```

The first adapter can be in-memory:

```java
@ApplicationScoped
class InMemoryEventStore implements EventStore {
    private final Map<CorrelationId, List<ApplicationEvent>> events =
        new ConcurrentHashMap<>();
}
```

`WorkflowState` is a reducer or projection:

```java
final class WorkflowState {

    static WorkflowState replay(List<ApplicationEvent> events) {
        WorkflowState state = new WorkflowState();
        events.forEach(state::apply);
        return state;
    }

    private void apply(ApplicationEvent event) {
        // update state from known workflow event types
    }
}
```

The orchestrator can then follow this pattern:

```java
eventStore.append(event.correlationId(), event);

WorkflowState state =
    WorkflowState.replay(eventStore.load(event.correlationId()));

dispatchMissingFollowUpCommands(state);

if (state.isComplete()) {
    eventPublisher.publish(RequirementAnalysisCompletedEvent.create(...));
}
```

## Why Events Alone Are Not Enough

Single events often contain enough data for the next local step:

- `RequirementElementsExtractedEvent` contains the extracted `Action`.
- `ConceptCandidatesRetrievedEvent` contains the `CandidateConceptMatch`.
- `ConceptMatchEvaluatedEvent` contains the `ConceptMatchDecision`.

The orchestration problem appears when several events must be combined.

Example: one extracted action may produce multiple retrieval commands for
subject, action, object, conditions, and constraints. Matching then produces one
decision per retrieved element. To publish a final completion event, the
orchestrator must know:

- which elements were expected
- which retrieval results have arrived
- which match decisions have arrived
- whether classification has arrived
- whether the full workflow is complete

That information is not contained in one event. It is derived from all workflow
events with the same `correlationId`.

## Expected Workflow

The exact start event will be defined with the ingestion slice. The orchestration
shape is:

```text
start event with correlationId and raw text
  -> dispatch ClassifyRequirementCommand
  -> dispatch ExtractSyntaxCommand

RequirementClassifiedEvent
  -> append to EventStore
  -> replay WorkflowState
  -> maybe complete

RequirementElementsExtractedEvent
  -> append to EventStore
  -> derive RequirementElement values from Action
  -> dispatch RetrieveCandidateConceptsCommand per element

ConceptCandidatesRetrievedEvent
  -> append to EventStore
  -> dispatch EvaluateConceptMatchCommand

ConceptMatchEvaluatedEvent
  -> append to EventStore
  -> replay WorkflowState
  -> maybe publish RequirementAnalysisCompletedEvent
```

Commands and events participating in this workflow need to carry the same
`correlationId`.

## First Implementation Constraints

The initial implementation may be in-process and in-memory.
Only synchronous event publishing.

The orchestrator should depend on ports:

- `CommandBus` for dispatching slice commands
- `EventPublisher` for publishing orchestration events
- `EventStore` for storing and loading workflow events

The first `EventStore` adapter can be `InMemoryEventStore`. A later persistent
adapter should not require changes to the atomic slices.

The existing slices should remain atomic and independently callable. Their
domain logic should not know the orchestrator, event store, graph persistence, or
future duplicate checks.

## Open Design Points

- Which exact event type starts the workflow after ingestion is introduced.
- Which `Action` parts are relevant for retrieval in v1.
- Whether classification confidence should influence orchestration or only the
  later persist/review decision.
- What the final completion event should contain for the future persist slice.
- How to represent human-review outcomes for `PROPOSE_EXISTING` and
  `REVIEW_REQUIRED`.
