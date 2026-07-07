# Graph Change Slice

The Graph Change slice persists a completed requirement analysis as a Knowledge
Graph change. It turns final workflow decisions into graph node references,
constructs the assertion identity, writes Neo4j data, and records the final
ingestion outcome.

## Business Responsibility

Graph Change receives `RequirementAnalysisCompletedEvent`. At that point the
workflow has:

- original provenance,
- requirement classification,
- extracted Subject-Predicate-Object action,
- final node match decisions for all expected requirement elements.

The slice converts this completed analysis into `PersistRequirementGraphChange`
and persists it through `RequirementGraphChangePort`.

Relevant classes:

- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangeService.java`
- `src/main/java/io/fekav/req/graphchange/application/CompletedAnalysisGraphChangeFactory.java`
- `src/main/java/io/fekav/req/graphchange/application/PersistRequirementGraphChange.java`
- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangePort.java`

## Assertion Identity

The central graph fact is an assertion:

```text
Subject -> Predicate -> Object
```

`AssertionIdentity` requires:

- subject references a `CONCEPT`,
- predicate references a `PREDICATE`,
- object references a `CONCEPT`.

Its assertion key is:

```text
subjectKey|predicateKey|objectKey
```

Conditions and constraints are not part of assertion identity. They are
qualifiers attached to the assertion.

Relevant classes:

- `src/main/java/io/fekav/req/graphchange/domain/AssertionIdentity.java`
- `src/main/java/io/fekav/req/graphchange/domain/GraphQualifier.java`

## Decision Mapping

`CompletedAnalysisGraphChangeFactory` maps node match decisions into graph node
references:

- existing-node decisions use the selected candidate key, label, and node type,
- create-new decisions derive a new graph node from the requirement element
  text,
- `SUBJECT` and `OBJECT` must become `CONCEPT`,
- `ACTION` must become `PREDICATE`,
- `CONDITION` and `CONSTRAINT` must become `QUALIFIER`.

Missing or duplicate required decisions are rejected with
`InvalidGraphChangeException`.

## Persistence Behavior

`Neo4jRequirementGraphChangeAdapter` applies three business outcomes:

- existing requirement id: no write, result `unchanged`,
- new requirement but existing assertion identity: no write, result `known`,
- new assertion identity: write requirement, provenance, concepts, predicate,
  assertion, qualifiers, and relationships.

Neo4j nodes and relationships include:

- `(Requirement)-[:HAS_PROVENANCE]->(Provenance)`
- `(Requirement)-[:ASSERTS]->(Assertion)`
- `(Assertion)-[:HAS_SUBJECT]->(Concept)`
- `(Assertion)-[:HAS_PREDICATE]->(Predicate)`
- `(Assertion)-[:HAS_OBJECT]->(Concept)`
- `(Assertion)-[:HAS_QUALIFIER]->(Qualifier)`

Relevant class:

- `src/main/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapter.java`

## Results and Follow-Up

`RequirementGraphChangeResult` maps persistence results back to ingestion
outcomes:

- `created` -> `RECORDED`,
- `known` -> `ALREADY_EXISTS` and publishes `RequirementKnownEvent`,
- `unchanged` -> `ALREADY_EXISTS`.

The service is disabled unless `req.graphchange.enabled=true`.

Relevant classes:

- `src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangeResult.java`
- `src/main/java/io/fekav/req/shared/event/RequirementKnownEvent.java`

## Scope Boundary

Graph Change does not decide node matches and does not open reviews. It only
persists completed, already-decided analysis results.

## Tests

The behavior is covered by:

- `src/test/java/io/fekav/req/graphchange/application/*Test.java`
- `src/test/java/io/fekav/req/graphchange/domain/AssertionIdentityTest.java`
- `src/test/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapterTest.java`

The tests verify decision-to-node mapping, assertion identity validation,
qualifier handling, disabled mode, known requirements, and Neo4j write shape.

## Technical Interface

This slice has no direct REST command or query.

Consumed event:

- `RequirementAnalysisCompletedEvent`

Published event:

- `RequirementKnownEvent`, only when the assertion already exists.

Port call:

- `RequirementGraphChangePort.persist(PersistRequirementGraphChange)`

Port results and follow-up behavior:

- `RequirementGraphChangeResult.created()`: records
  `IngestRequirementResult.recorded(...)`; publishes no follow-up event.
- `RequirementGraphChangeResult.known(AssertionIdentity)`: records
  `IngestRequirementResult.alreadyExists(...)`; publishes
  `RequirementKnownEvent`.
- `RequirementGraphChangeResult.unchanged()`: records
  `IngestRequirementResult.alreadyExists(...)`; publishes no follow-up event.

Other output:

- Records `IngestRequirementResult.recorded(...)` when a new graph change is
  persisted.
- Records `IngestRequirementResult.alreadyExists(...)` when the requirement or
  assertion is already known.

Known-event shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "subject": {
    "nodeType": "CONCEPT",
    "key": "login form",
    "label": "login form"
  },
  "predicate": {
    "nodeType": "PREDICATE",
    "key": "must validate",
    "label": "must validate"
  },
  "object": {
    "nodeType": "CONCEPT",
    "key": "credentials",
    "label": "credentials"
  }
}
```
