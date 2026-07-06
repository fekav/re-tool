# Implementation Plan: graphchange

## Overview

The `graphchange` slice consumes
`RequirementAnalysisCompletedEvent` after a requirement analysis reaches a
final decision. It writes a new semantic requirement into the Neo4j knowledge
graph only when the final subject-predicate-object assertion is not already
known. If the assertion is already known, the slice publishes a
`RequirementKnownEvent` with the subject, predicate, and object and performs no
further graph processing.

Review handling is outside this slice. The slice only consumes completed
analysis events whose node decisions are already final.

## Confirmed Intent

- Outcome: persist new analyzed requirements into the knowledge graph.
- Slice name: `graphchange`, not generic `persistence`.
- Semantic identity: an assertion is known when subject, predicate, and object
  point to the same graph nodes.
- Qualifiers: conditions and constraints are persisted when present, but do not
  participate in semantic identity.
- Known assertion behavior: publish `RequirementKnownEvent` with subject,
  predicate, and object; do not create a new `Requirement`.
- New assertion behavior: create a `Requirement` anchor and write provenance,
  classification properties, assertion, SPO nodes and relationships, and
  qualifiers.
- Technical idempotency: use deterministic graph keys and `MERGE` where
  possible. Full inbox/outbox semantics are out of scope for the MVP.

## Architecture Decisions

- Add code under `io.fekav.req.graphchange.{domain,application,infrastructure}`.
- Keep domain decisions out of the Neo4j adapter. The application layer builds a
  graph-change request from the completed analysis event; infrastructure only
  executes the graph operation.
- Use the existing graph model from
  `src/main/java/io/fekav/req/shared/kg/Neo4jSchemaInitializer.java`.
- Store classification as existing `Requirement` properties:
  `type` and `property`.
- Do not store classification confidence or rationale in the MVP graph write.
- Use `Requirement.id = correlationId.value().toString()` for technical
  workflow idempotency.
- Use `Assertion.assertionKey = subjectKey + "|" + predicateKey + "|" +
  objectKey` for MVP semantic idempotency.
- Resolve graph-node identities from `NodeMatchDecision`:
  `AUTO_MAP_EXISTING` references the selected candidate node, while
  `AUTO_CREATE_NEW` derives the canonical key and label from the requirement
  element text.
- Compute target identities before writing any graph nodes. The known check
  must not leave partial graph data behind.

## Dependency Graph

```text
shared RequirementKnownEvent
    |
graphchange domain model
    |
event-to-graph-change mapping
    |
application observer/service
    |
Neo4j graph-change adapter
    |
slice and regression tests
```

## Task 1: Model Graph Assertion Identity and Known Event

**Description:** Introduce the small domain vocabulary required for graph
changes and add the application event emitted when an assertion is already
known.

**Acceptance Criteria:**

- [ ] `GraphNodeReference` represents a resolved graph node with type, key, and
  label.
- [ ] `AssertionIdentity` represents the resolved subject, predicate, and object
  triple.
- [ ] `AssertionIdentity` accepts only `CONCEPT` subject, `PREDICATE`
  predicate, and `CONCEPT` object.
- [ ] `RequirementKnownEvent` implements `ApplicationEvent`.
- [ ] `RequirementKnownEvent` carries only correlation metadata plus subject,
  predicate, and object.

**Verification:**

- [ ] `./gradlew test --tests '*RequirementKnownEventTest'`
- [ ] `./gradlew test --tests 'io.fekav.req.graphchange.domain.*'`

**Dependencies:** None

**Files Likely Touched:**

- `src/main/java/io/fekav/req/graphchange/domain/GraphNodeReference.java`
- `src/main/java/io/fekav/req/graphchange/domain/AssertionIdentity.java`
- `src/main/java/io/fekav/req/shared/event/RequirementKnownEvent.java`
- `src/test/java/io/fekav/req/shared/event/RequirementKnownEventTest.java`
- `src/test/java/io/fekav/req/graphchange/domain/AssertionIdentityTest.java`

**Estimated Scope:** Medium

## Task 2: Translate Completed Analysis Into a Graph Change

**Description:** Build the application/domain translation from
`RequirementAnalysisCompletedEvent` to a graph-change command that is ready for
known-check and persistence.

**Acceptance Criteria:**

- [ ] The mapper extracts the required subject, action/predicate, and object
  decisions.
- [ ] `AUTO_MAP_EXISTING` decisions resolve to the exactly one selected
  candidate.
- [ ] `AUTO_CREATE_NEW` decisions resolve to canonical graph-node identities
  derived from the requirement element text.
- [ ] Missing required subject, action, or object decisions fail with a named
  graphchange exception.
- [ ] Conditions and constraints become optional qualifier write intents.
- [ ] Qualifiers do not affect `AssertionIdentity` or the assertion key.

**Verification:**

- [ ] Unit tests cover auto-map for existing SPO nodes.
- [ ] Unit tests cover auto-create for new SPO nodes.
- [ ] Unit tests cover optional condition and constraint qualifiers.
- [ ] Unit tests cover missing and malformed required decisions.

**Dependencies:** Task 1

**Files Likely Touched:**

- `src/main/java/io/fekav/req/graphchange/application/CompletedAnalysisGraphChangeFactory.java`
- `src/main/java/io/fekav/req/graphchange/application/PersistRequirementGraphChange.java`
- `src/main/java/io/fekav/req/graphchange/domain/GraphQualifier.java`
- `src/main/java/io/fekav/req/graphchange/domain/InvalidGraphChangeException.java`
- `src/test/java/io/fekav/req/graphchange/application/CompletedAnalysisGraphChangeFactoryTest.java`

**Estimated Scope:** Medium

## Task 3: Add Application Service and Event Observer

**Description:** Add the use case that reacts to completed analysis events,
delegates the graph decision/write to a port, and publishes the known event
when the graph says the assertion already exists.

**Acceptance Criteria:**

- [ ] A CDI observer consumes `RequirementAnalysisCompletedEvent`.
- [ ] The observer can be disabled with `req.graphchange.enabled`, defaulting to
  disabled unless the project chooses otherwise before implementation.
- [ ] New assertion results do not publish `RequirementKnownEvent`.
- [ ] Known assertion results publish exactly one `RequirementKnownEvent`.
- [ ] Technical retry/no-op results publish no event.
- [ ] The application service depends on an application port, not Neo4j
  directly.

**Verification:**

- [ ] Unit tests use a fake graph-change port and recording event publisher.
- [ ] Tests cover disabled observer, known result, new result, and no-op result.

**Dependencies:** Tasks 1 and 2

**Files Likely Touched:**

- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangeService.java`
- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangePort.java`
- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangeResult.java`
- `src/test/java/io/fekav/req/graphchange/application/RequirementGraphChangeServiceTest.java`

**Estimated Scope:** Medium

## Task 4: Implement Neo4j Graph Change Adapter

**Description:** Implement the infrastructure adapter that applies the graph
change in Neo4j using the existing schema and deterministic write behavior.

**Acceptance Criteria:**

- [ ] The adapter first checks for an existing `Requirement` with the
  correlation-based requirement id and returns no-op when found.
- [ ] The adapter then checks for an existing `Assertion` with the SPO
  assertion key and returns known when found.
- [ ] In the new assertion path, the adapter writes `Requirement`,
  `Provenance`, `Assertion`, SPO nodes, SPO relationships, and optional
  qualifiers in one write transaction.
- [ ] `Requirement.type` and `Requirement.property` are set from
  classification.
- [ ] `Provenance` stores the raw text/source data available from the event.
- [ ] Existing graph constraints and labels are reused.
- [ ] Review events and review requests are not read or written.

**Verification:**

- [ ] Adapter tests capture Cypher statements and parameters.
- [ ] Tests cover known assertion, existing requirement no-op, new assertion
  without qualifiers, and new assertion with qualifiers.
- [ ] `./gradlew test --tests 'io.fekav.req.graphchange.*'`

**Dependencies:** Task 3

**Files Likely Touched:**

- `src/main/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapter.java`
- `src/test/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapterTest.java`

**Estimated Scope:** Medium

## Task 5: Align Tests and Graph Model Expectations

**Description:** Run the slice and regression test suite and address any
intentional changes in graph semantics.

**Acceptance Criteria:**

- [ ] All graphchange tests pass.
- [ ] Existing orchestration and resolution tests pass.
- [ ] Existing shared event tests pass.
- [ ] Existing KG schema tests still describe the current schema accurately.
- [ ] The sample graph's duplicate SPO assertion is either documented as seed
  data that predates the new graphchange semantics or adjusted intentionally.

**Verification:**

- [ ] `./gradlew test`

**Dependencies:** Tasks 1 through 4

**Files Likely Touched:**

- `src/test/java/io/fekav/req/shared/kg/Neo4jSchemaInitializerTest.java`
- `docs/graph-model.md`

**Estimated Scope:** Small to Medium

## Checkpoints

### Checkpoint: Domain Contract

After Tasks 1 and 2:

- [ ] Graphchange domain vocabulary matches the confirmed intent.
- [ ] `RequirementKnownEvent` payload is minimal.
- [ ] SPO identity and qualifier exclusion are tested.

### Checkpoint: Core Flow

After Tasks 3 and 4:

- [ ] Completed analysis drives a graph-change port call.
- [ ] Known assertions publish `RequirementKnownEvent`.
- [ ] New assertions write the expected Neo4j shape.
- [ ] Retry/no-op behavior is explicit.

### Checkpoint: Complete

After Task 5:

- [ ] All tests pass.
- [ ] Graph model docs match implemented behavior.
- [ ] Human review approves the plan before implementation work starts.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Current sample graph contains duplicate SPO assertions | Medium | Decide during Task 5 whether seed data is historical example data or must be aligned with new semantics. |
| Known events can be re-published on redelivery | Medium | Use deterministic event payload and consider deterministic event id or later inbox/outbox support. |
| Concurrent writes of the same new SPO assertion race | Medium | Rely on Neo4j `Assertion.assertionKey` uniqueness for MVP; add stronger transaction/error handling in a later hardening task. |
| Candidate keys may differ from canonical names | Medium | Treat candidate key as identity and label as display text; adapter must map by node type and key consistently. |
| Graph write grows too broad | Medium | Keep MVP to Requirement, Provenance, Classification properties, Assertion, SPO nodes, and Qualifiers only. |

## Out of Scope

- Human review workflow.
- Full semantic requirement deduplication beyond SPO assertion identity.
- Qualifier-based identity.
- Classification confidence and rationale persistence.
- Versioned classifications.
- Mention nodes.
- Requirement relation inference such as `REFINES`, `SATISFIES`,
  `DEPENDS_ON`, or `CONFLICTS_WITH`.
- Inbox/outbox infrastructure.
