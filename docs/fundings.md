# Architecture Findings

## Scope

This assessment covers the current workspace, including its uncommitted changes,
against pragmatic DDD, Clean Architecture, and Hexagonal Architecture criteria.
It evaluates the actual runtime path as well as the written architecture.

The following prototype choices are accepted and are not findings:

- In-memory stores and projections may lose state on restart.
- The Neo4j graph may be reset or seeded during local development.
- There is no recommendation here to add an outbox, durable event store, event
  sourcing, or separate read/write databases.

Those exclusions do not cover incorrect behavior while the process is running,
unclear domain identity, or contradictory model semantics.

`./gradlew test` could not be run in this execution environment because neither
`JAVA_HOME` nor a `java` executable is available. This is an environment
verification gap, not a reported project test failure.

## Current Shape

The codebase is a pragmatic, event-driven vertical-slice application rather
than a strict four-layer Clean Architecture implementation.

```text
REST / Chainlit
  -> generic command or query bus
  -> ingestion
  -> CDI application events
  -> orchestration process manager
  -> classification + extraction
  -> node resolution
  -> review when required
  -> completed-analysis graph change
  -> Neo4j graph and search read adapter
```

The main areas are:

| Area | Responsibility | Assessment |
|---|---|---|
| `platform` | REST, CQRS dispatch, CDI events, LLM client, structured output, observability | Outer technical layer; coherent for a Quarkus prototype. |
| `classification`, `extraction`, `resolution` | Analyze and normalize a requirement into graph-ready terms | Good vertical slices; resolution keeps Neo4j behind domain-owned lookup ports. |
| `orchestration` | Replays correlation-scoped events and drives the workflow | A process manager, not a `Requirement` aggregate. |
| `review` | Maintains pending human decisions | Useful CQRS-style projection, but it currently breaks correlation isolation. |
| `graphchange`, `search` | Write the knowledge graph and expose a query model | Ports and Neo4j adapters are well placed; duplicate-assertion semantics conflict with the graph model. |
| `shared` | Cross-slice vocabulary and events | Has grown beyond a small shared kernel and contains infrastructure and an unused aggregate. |

The domain packages for the individual slices do not import Quarkus, CDI, or
Neo4j. Application code also does not directly import slice infrastructure.
Those are meaningful strengths. The application layer does use CDI annotations
and producers, so this is framework-coupled application code, not a fully
framework-independent Clean Architecture core.

## Strengths

- The LLM boundary is solid. `LlmSyntaxExtraction` and
  `LlmRequirementClassificationService` translate provider JSON through schemas,
  DTOs, validation, and domain values rather than letting LLM output enter the
  domain unchecked.
- Resolution is the clearest Hexagonal slice. `CandidateLookup` and
  `CompatibleCandidateLookup` are owned by the resolution domain, while
  `Neo4jNodeNameLookup` implements them outside that domain.
- Value objects and result types validate themselves and use immutable copies
  consistently. This is especially visible in node decisions, graph references,
  and query results.
- The workflow uses explicit correlation and event IDs, and the in-memory event
  store prevents duplicate delivery of the same event ID within a running
  process.
- Reads are separated from graph mutation through `RequirementsFinder` and the
  Neo4j query adapter. This is a proportional use of CQRS, not premature
  separate infrastructure.

## Findings

### High: an existing assertion suppresses a distinct requirement

The stated graph model says that multiple `Requirement` nodes can point to the
same subject-predicate-object `Assertion`, even when they have different
qualifiers. See `README.md:65-67` and `docs/graph-model.md:46-48`.

The implementation instead checks for an existing assertion before it writes
the new requirement. `Neo4jRequirementGraphChangeAdapter` returns `known` at
`src/main/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapter.java:74-79`.
That path writes neither the incoming `Requirement` and `Provenance` nor its
qualifiers nor a new `ASSERTS` relationship. The adapter test explicitly
asserts this no-write behavior at
`src/test/java/io/fekav/req/graphchange/infrastructure/Neo4jRequirementGraphChangeAdapterTest.java:91-113`.

This is not restart data loss. While the process is healthy, a later
requirement with the same core assertion is discarded from the knowledge graph,
despite having its own source, classification, raw text, and possibly new
conditions or constraints. Search can consequently never show that later
requirement.

The repository has two incompatible semantic contracts. The graph-change slice
README describes the current no-write behavior, while the root and graph-model
documentation describe shared assertions with multiple requirements. Resolve
that product decision first:

1. Recommended for the stated graph model: always persist a new `Requirement`
   and `Provenance`, reuse the existing `Assertion`, and attach any qualifier
   semantics intentionally.
2. Alternative: declare identical subject-predicate-object assertions to be
   canonical duplicates that must be rejected, then update the root model,
   graph model, API result language, and tests to say that explicitly.

### High: review projection conflates independent workflows

`NodeMatchReviewId` includes `correlationId`, element type, and element text at
`src/main/java/io/fekav/req/review/domain/NodeMatchReviewId.java:21-31`. That
correctly identifies a review within one ingestion workflow.

`NodeMatchReviewProjection` ignores that identity when serving and closing
reviews. It returns only the earliest open review for each
`RequirementElement` at
`src/main/java/io/fekav/req/review/application/NodeMatchReviewProjection.java:42-59`.
It closes every review with the same element at lines `33-39`, without checking
the correlation ID or review ID.

For example, two unrelated requirements that both need a decision for
`SUBJECT: payment service` cannot both appear in the pending-review list.
Deciding one can close the other. This is a same-process correctness defect,
not an in-memory durability concern.

Key the projection's open and close behavior by `reviewId`, or by the tuple of
correlation ID and requirement element. Add tests with two distinct correlation
IDs and the same element. The current projection tests use a single correlation
ID (`NodeMatchReviewProjectionTest.java:29-36`), so they do not protect this
boundary.

### Medium: `Requirement` is a dead aggregate with a competing identity model

`req.shared.model.Requirement` presents itself as the aggregate root. It owns a
`RequirementId`, lifecycle status, provenance, extraction, classification, and
a domain-event collection. In production, no handler creates or uses it: the
workflow instead exchanges application events containing `CorrelationId`, raw
text, provenance, classification, and action.

The graph-change factory makes this explicit by using the correlation UUID as
the persisted requirement ID at
`src/main/java/io/fekav/req/graphchange/application/CompletedAnalysisGraphChangeFactory.java:54-61`.
The `RequirementId` created in
`src/main/java/io/fekav/req/shared/model/Requirement.java:27-33` has no role in
the live workflow. Its `DomainEvent` collection is also unused; no production
class implements `DomainEvent`.

This creates two competing meanings of identity:

| Concept | Current implementation |
|---|---|
| Requirement aggregate identity | Generated but unused `RequirementId` |
| Workflow identity | `CorrelationId` |
| Persisted graph requirement identity | `CorrelationId` string |

Pick one deliberate model. Either make `Requirement` the real aggregate and
carry its ID through all workflow events, or remove it and name the
correlation-scoped workflow state as the actual model. For this prototype, the
second option is likely simpler. The critical point is one source of truth, not
adding persistence.

`docs/ARCHITECTURE.md:50-78` and `:83-106` still describe handlers creating and
mutating this aggregate, which no longer matches the code.

### Medium: the shared kernel has leaked slice and infrastructure concerns

`req/shared` is imported by 74 production Java files, so it is the effective
shared kernel. That is reasonable for stable vocabulary such as `RawText`,
`Provenance`, correlation-aware events, and `RequirementElement`.

It currently also contains:

- the unused `Requirement` aggregate, which imports both classification and
  extraction types (`Requirement.java:7-19`), making shared code depend on its
  consumers;
- `Neo4jSchemaInitializer` under `req.shared.kg`, which directly imports
  Quarkus, CDI, configuration, and the Neo4j driver
  (`Neo4jSchemaInitializer.java:3-11`).

This does not violate the clean domain boundaries inside the individual slices,
but it weakens the meaning of `shared` and raises the cost of changing any
slice. Keep stable cross-slice vocabulary there, move graph bootstrap to a
graph/Neo4j infrastructure owner, and remove or relocate the unused aggregate
after the identity decision.

### Medium: ingestion is a synchronous request-response workflow disguised as asynchronous events

`IngestRequirementCommandHandler` publishes an ingestion event and immediately
consumes an outcome at
`src/main/java/io/fekav/req/ingestion/application/IngestRequirementCommandHandler.java:42-59`.
The active CDI adapter invokes observers synchronously through `Event.fire` at
`src/main/java/io/fekav/platform/adapter/CdiEventPublisher.java:85-102`.
Graph Change then writes its result directly into the ingestion-owned
`IngestionOutcomeStore` at
`src/main/java/io/fekav/req/graphchange/application/RequirementGraphChangeService.java:43-46`.

This works as an in-process synchronous pipeline today. It will not retain the
same API behavior if `EventPublisher` later becomes asynchronous, because the
ingestion handler will consume before a final result exists and return
`INTERNAL_ERROR`. The `EventPublisher` port does not document synchronous
delivery as a contract.

Do not add messaging infrastructure for the prototype. Instead, document the
current design as synchronous process orchestration. If asynchronous processing
is introduced later, separate workflow status from the original command result:
return a correlation ID and query a workflow-status read model rather than using
an ingestion-local response store as a cross-slice callback.

### Low: architecture documentation is stale and internally inconsistent

`docs/ARCHITECTURE.md` describes only classification and the old
`syntaxextraction` package, while the current application also has ingestion,
resolution, review, orchestration, graph change, and search. It describes the
dead aggregate flow rather than the event-driven process manager. It also links
to `docs/json-contracts.md`, which does not exist.

The extraction README retains the old test-package names. The graph-change
README agrees with the adapter's no-write-on-known-assertion behavior, while
the root README and graph-model documentation promise multiple requirements
sharing one assertion. Update the architecture documents after resolving the
duplicate-assertion rule; otherwise documentation will keep producing invalid
implementation decisions.

### Low: tests cover local behavior but do not enforce architecture or the risky cross-workflow semantics

The repository currently has 63 Java test files, including 7 `*TestIT` files.
That is a useful base. There is no ArchUnit dependency or architecture test to
protect the intended rules that domain code is framework-free and application
code must not depend directly on infrastructure.

The most valuable additions after the semantic decisions are:

1. Two same-element reviews with different correlations remain independently
   visible and are independently closed.
2. The chosen behavior for a second requirement with an existing assertion is
   tested end to end, including provenance and qualifiers.
3. An architecture test guards the current inward dependency direction and
   prevents `shared` from gaining new infrastructure imports.

## Recommended Evolution Order

1. Decide whether a repeated subject-predicate-object statement is a new
   traceable requirement or a rejected duplicate. Align the graph write path,
   API result, and documentation with that choice.
2. Correct review identity to be correlation-scoped and add the missing
   cross-workflow tests.
3. Choose the authoritative identity model and remove the dead aggregate or
   make it real.
4. Reduce `shared` to a genuine shared kernel and move Neo4j bootstrap to
   infrastructure.
5. Refresh the architecture documentation and add narrow architecture tests.

## Overall Assessment

The project has a promising prototype architecture. Its strongest areas are
the LLM anti-corruption boundary, the resolution ports, and the graph read/write
adapter separation. It should remain a single in-process application for now;
CQRS is already proportionate, and event sourcing, an outbox, and durable
workflow infrastructure would be premature.

Before adding more slices, resolve the two high-severity semantic issues. They
affect the core promise of a traceable requirements graph and correct human
review behavior even within the accepted in-memory prototype model.
