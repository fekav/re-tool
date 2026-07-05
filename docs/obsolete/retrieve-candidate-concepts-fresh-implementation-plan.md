# Implementation Plan: Retrieve Candidate Concepts

## Overview

Implement `RETRIEVE_CANDIDATE_CONCEPTS` as a domain-driven retrieval slice. The
slice accepts selected requirement element terms from extracted requirements and returns a
`CandidateConceptMatchSet` with ordered KG candidate concepts or explicit
no-match evidence for each selected term.

The central design is a domain service that applies a concrete
`ConceptRetrievalPolicy`. The policy owns the retrieval order, fallback rules,
and stop conditions. Lookup methods execute individual searches, such as exact
label lookup or alias lookup, but they are not policies by themselves.

## Architecture Decisions

- Use a new `conceptretrieval` slice with `domain`, `application`, and
  `infrastructure` packages, matching the command-handler and port/adapter
  style used by classification and syntax extraction.
- Keep `RetrieveCandidateConceptsCommandHandler` thin. It validates command
  input, converts it to domain values, calls `ConceptRetrievalService`, and
  returns the domain result.
- Put `ConceptRetrievalService` in the domain layer. It executes the retrieval
  operation for all selected terms and delegates policy-specific ordering to
  `ConceptRetrievalPolicy`.
- Model `ConceptRetrievalPolicy` as the business rule for lookup order,
  fallback, stop conditions, and final candidate ordering.
- Model individual searches as lookup methods, not policies. Examples:
  `ExactLabelCandidateLookup`, `AliasCandidateLookup`, later
  `GraphTraversalCandidateLookup` or `EmbeddingCandidateLookup`.
- Do not implement Specifications in this iteration. Role eligibility,
  fallback, and stop rules stay inside the concrete retrieval policy until they
  become reusable or complex enough to extract.
- Use weighted lookup order as the primary retrieval signal. Every lookup method
  has a numeric weight. Candidate scores are required and equal the sum of the
  weights for the lookup methods that matched the candidate.
- Treat `1.0` as the maximum retrieval score. Exact label lookup has weight
  `1.0`, so exact label hits are full matches for retrieval and stop further
  lookup. Lower-weight lookup combinations must remain below `1.0`.
- Retrieval does not create mappings, approve matches, reject candidates, create
  concepts, or request human review. It only produces ordered candidates and
  evidence for `DECIDE_CONCEPT_MATCHES`.

## Policy Design

### Concrete V1 Policy

Implement `OrderedWeightedConceptRetrievalPolicy`.

For each selected term:

1. Run lookup methods in descending weight order.
2. Run exact label lookup first because its weight is `1.0`.
3. If exact label lookup returns candidates, return those candidates ordered by
   the exact lookup result with score `1.0` and stop.
4. If exact label lookup returns no candidates, run alias lookup with weight
   `0.7`.
5. In v1, alias lookup is the only non-exact lookup method. If alias lookup
   returns candidates, return them with score `0.7` and deterministic alias
   lookup order.
6. When future lower-weight lookup methods are added, the policy may continue
   through those non-exact methods before returning. It aggregates candidates by
   concept id and sums the weights for every lookup method that matched the same
   candidate. Configured non-exact weights must keep every possible non-exact
   combination below `1.0`.
7. If no lookup method returns candidates, return an empty candidate list with
   no-match evidence describing the attempted lookup methods.

The policy may apply requirement-element-aware lookup scope. V1 uses the selected term
requirement element as lookup context and lets lookup adapters restrict candidate labels
or alias searches where the KG supports it.

### Future Concrete Policies

These are not implemented in v1, but the design should allow them without
changing the workflow:

- `ExactOnlyConceptRetrievalPolicy`: use only exact label lookup and produce
  no-match evidence otherwise.
- `ConservativeConceptRetrievalPolicy`: use deterministic lookups such as exact
  label and alias lookup, but skip embedding search.
- `ExploratoryConceptRetrievalPolicy`: use exact label, alias, graph traversal,
  and embedding lookup to maximize review candidates.
- `RoleAwareConceptRetrievalPolicy`: vary lookup order by requirement element.

## Domain Model

- `SelectedTerm`: requirement element and text. Command callers provide no ids.
- `CandidateConcept`: KG concept id, label, and optional concept type.
- `CandidateLookupHit`: one raw lookup hit from one lookup method, containing
  candidate concept, lookup method name, evidence text, and lookup rank.
- `RetrievedCandidateConcept`: candidate concept plus non-empty
  candidate-specific retrieval evidence produced by the retrieval policy.
- `RetrievalEvidence`: policy name, applied lookup method names, evidence text,
  required score, and optional lookup ranks.
- `CandidateConceptMatch`: selected term, ordered retrieved candidates, and
  non-empty term-level evidence.
- `CandidateConceptMatchSet`: one match entry per selected term.

No-match evidence is term-level because there is no candidate to attach it to.
Candidate-specific evidence is required for every real retrieved candidate.

## Dependency Graph

```text
Workflow contract
    -> Retrieval domain values
        -> Lookup method ports
            -> ConceptRetrievalPolicy
                -> ConceptRetrievalService
                    -> RetrieveCandidateConceptsCommandHandler
                        -> Neo4j lookup adapters
                            -> REST command dispatch coverage
```

## Task List

### Task 1: Add Retrieval Domain Values

**Description:** Add the domain records that represent selected terms, candidate
concepts, retrieved candidates, retrieval evidence, and match sets.

**Acceptance criteria:**
- [ ] `SelectedTerm` requires a present requirement element and non-blank text.
- [ ] `CandidateConcept` requires non-blank concept id and label, with optional
  concept type.
- [ ] `RetrievedCandidateConcept` requires a candidate and non-empty
  candidate-specific evidence.
- [ ] `CandidateConceptMatch` requires one selected term, ordered candidates,
  and non-empty term-level evidence.
- [ ] `CandidateConceptMatchSet` contains exactly one match per selected term.
- [ ] Domain records trim strings and defensively copy collections.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*conceptretrieval.domain*'`

**Dependencies:** None.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/domain/SelectedTerm.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConcept.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateLookupHit.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/RetrievedCandidateConcept.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/RetrievalEvidence.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatch.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatchSet.java`
- `src/test/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatchSetTest.java`

**Estimated scope:** Medium.

### Task 2: Add Lookup Method Ports

**Description:** Add application ports for individual candidate lookup methods.
Each lookup method performs one search type, exposes its configured retrieval
weight, and returns candidates in deterministic lookup order.

**Acceptance criteria:**
- [ ] `ConceptCandidateLookup` exposes a lookup method name and a method such as
  `findCandidates(SelectedTerm selectedTerm, CandidateLookupScope scope)`.
- [ ] `ConceptCandidateLookup` exposes a positive lookup weight.
- [ ] `exactLabel` lookup has weight `1.0`.
- [ ] `alias` lookup has weight `0.7`.
- [ ] `CandidateLookupScope` carries requirement-element context and allowed concept
  type hints when available.
- [ ] Lookup methods return `CandidateLookupHit` values without final retrieval
  scores.
- [ ] Lookup methods return an empty list when they find no candidates.
- [ ] Lookup ports contain no policy fallback or mapping-decision logic.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*conceptretrieval.application*'`

**Dependencies:** Task 1.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/ConceptCandidateLookup.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/CandidateLookupScope.java`

**Estimated scope:** Small.

### Task 3: Add Retrieval Policy

**Description:** Add `ConceptRetrievalPolicy` and implement
`OrderedWeightedConceptRetrievalPolicy` as the v1 business rule for weighted
lookup order, exact-match short-circuiting, and candidate score aggregation.

**Acceptance criteria:**
- [ ] `ConceptRetrievalPolicy` returns one `CandidateConceptMatch` for one
  `SelectedTerm`.
- [ ] `OrderedWeightedConceptRetrievalPolicy` invokes lookup methods in
  descending weight order.
- [ ] Exact label lookup runs first because its weight is `1.0`.
- [ ] Alias lookup runs only when exact label lookup returns no candidates.
- [ ] Exact label candidates receive required retrieval score `1.0`.
- [ ] The policy stops after exact label lookup returns candidates.
- [ ] Alias candidates receive required retrieval score `0.7` in v1.
- [ ] Future lower-weight lookup methods can be combined by summing matched
  lookup weights per candidate.
- [ ] The policy groups lookup hits by candidate concept id before summing
  lookup weights.
- [ ] The policy creates candidate-specific `RetrievalEvidence` with required
  final score after lookup aggregation.
- [ ] The policy rejects or fails fast when configured non-exact lookup weights
  can combine to `1.0` or more.
- [ ] Returned candidates preserve the lookup method order from the successful
  step.
- [ ] If no lookup method returns candidates, the policy returns no-match
  term-level evidence.
- [ ] The policy contains no persistence, mapping, proposal, review, or graph
  driver logic.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*OrderedWeightedConceptRetrievalPolicy*'`

**Dependencies:** Task 2.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/domain/ConceptRetrievalPolicy.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/OrderedWeightedConceptRetrievalPolicy.java`
- `src/test/java/io/fekav/req/conceptretrieval/domain/OrderedWeightedConceptRetrievalPolicyTest.java`

**Estimated scope:** Medium.

### Checkpoint: Domain Retrieval Rules

- [ ] Domain tests pass.
- [ ] The concrete policy owns lookup order, fallback, score aggregation, and
  stop behavior.
- [ ] Lookup methods do not decide fallback or mapping outcomes.
- [ ] No Specifications are implemented in this iteration.

### Task 4: Add Concept Retrieval Domain Service

**Description:** Add `ConceptRetrievalService` as the domain service that applies
the active retrieval policy to all selected terms and builds a
`CandidateConceptMatchSet`.

**Acceptance criteria:**
- [ ] The service accepts a non-empty collection of `SelectedTerm` values.
- [ ] The service calls the active `ConceptRetrievalPolicy` once per selected
  term.
- [ ] The service returns a `CandidateConceptMatchSet`.
- [ ] The service enforces the invariant that every selected term has one match
  entry.
- [ ] The service contains no Neo4j, REST, LLM, or command-bus code.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*ConceptRetrievalService*'`

**Dependencies:** Task 3.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/domain/ConceptRetrievalService.java`
- `src/test/java/io/fekav/req/conceptretrieval/domain/ConceptRetrievalServiceTest.java`

**Estimated scope:** Small.

### Task 5: Add Retrieve Candidate Concepts Use Case

**Description:** Add the command and command handler. The handler follows the
classification and syntax extraction pattern: validate command input, build
domain values, delegate to the service, and return a domain result.

**Acceptance criteria:**
- [ ] `RetrieveCandidateConceptsCommand` carries selected term inputs with
  requirement element and text only.
- [ ] The command rejects null, empty, or blank selected term inputs.
- [ ] `RetrieveCandidateConceptsCommandHandler` is an `@ApplicationScoped`
  `CommandHandler`.
- [ ] The handler delegates to `ConceptRetrievalService`.
- [ ] The handler does not call lookup methods or policies directly.
- [ ] The handler returns `CandidateConceptMatchSet`.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RetrieveCandidateConceptsCommandHandler*'`

**Dependencies:** Task 4.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommand.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandler.java`
- `src/test/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandlerTest.java`

**Estimated scope:** Small.

### Task 6: Add Neo4j Exact Label Lookup Adapter

**Description:** Implement exact label lookup as an infrastructure adapter. It
queries explicit KG nodes by exact selected-term text and returns lookup hits
for every graph hit.

**Acceptance criteria:**
- [ ] The adapter implements `ConceptCandidateLookup`.
- [ ] The lookup method name is `exactLabel`.
- [ ] It queries graph concepts whose label or text exactly equals the selected
  term text.
- [ ] Returned hits name `exactLabel` as lookup method.
- [ ] Returned hits include evidence text and deterministic lookup rank.
- [ ] The adapter does not assign final retrieval scores.
- [ ] Results are ordered deterministically by label then concept id.
- [ ] No-hit lookup returns an empty list.
- [ ] Neo4j driver types remain inside `conceptretrieval.infrastructure`.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*Neo4jExactLabelCandidateLookup*'`

**Dependencies:** Task 2.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/infrastructure/Neo4jExactLabelCandidateLookup.java`
- `src/test/java/io/fekav/req/conceptretrieval/infrastructure/Neo4jExactLabelCandidateLookupTest.java`

**Estimated scope:** Medium.

### Task 7: Add Neo4j Alias Lookup Adapter

**Description:** Implement alias lookup as an infrastructure adapter. It searches
candidate alias values when the active retrieval policy invokes it.

**Acceptance criteria:**
- [ ] The adapter implements `ConceptCandidateLookup`.
- [ ] The lookup method name is `alias`.
- [ ] It searches a graph alias property for the selected term text.
- [ ] It uses requirement element from `CandidateLookupScope` when the KG node shape
  supports role filtering.
- [ ] Returned hits name `alias` as lookup method.
- [ ] Returned hits include evidence text and deterministic lookup rank.
- [ ] The adapter does not assign final retrieval scores.
- [ ] Results are ordered deterministically by label then concept id.
- [ ] No-hit lookup returns an empty list.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*Neo4jAliasCandidateLookup*'`

**Dependencies:** Task 2.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/infrastructure/Neo4jAliasCandidateLookup.java`
- `src/test/java/io/fekav/req/conceptretrieval/infrastructure/Neo4jAliasCandidateLookupTest.java`

**Estimated scope:** Medium.

### Task 8: Wire Active Policy and Lookup Methods

**Description:** Add CDI wiring so the command handler receives a
`ConceptRetrievalService` configured with `OrderedWeightedConceptRetrievalPolicy`
and the available lookup methods.

**Acceptance criteria:**
- [ ] `OrderedWeightedConceptRetrievalPolicy` receives exact and alias lookup
  methods without the command handler knowing their order.
- [ ] CDI can construct the command handler and retrieval service.
- [ ] Replacing the active policy does not require changing command handler
  code.
- [ ] Missing required lookup method wiring fails fast during application
  startup or service construction.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*conceptretrieval*'`

**Dependencies:** Tasks 5, 6, and 7.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/ConceptRetrievalServiceFactory.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandler.java`
- `src/test/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandlerTest.java`

**Estimated scope:** Medium.

### Task 9: Add REST Command Dispatch Coverage

**Description:** Prove the existing command endpoint can dispatch
`RetrieveCandidateConceptsCommand` and serialize the retrieval result.

**Acceptance criteria:**
- [ ] Posting `RetrieveCandidateConceptsCommand` returns a serialized
  `CandidateConceptMatchSet`.
- [ ] Exact-label hits serialize as ordered retrieved candidates with
  candidate-specific evidence.
- [ ] Alias hits serialize as ordered retrieved candidates after exact lookup
  misses.
- [ ] No-match terms serialize as empty candidates with term-level no-match
  evidence.
- [ ] The integration test mocks the retrieval service or lookup ports without
  requiring Neo4j.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RestControllerTestIT*'`

**Dependencies:** Task 8.

**Files likely touched:**
- `src/test/java/io/fekav/platform/api/RestControllerTestIT.java`

**Estimated scope:** Small.

### Checkpoint: Complete Retrieval Slice

- [ ] `./gradlew test --tests '*conceptretrieval*'` passes.
- [ ] `./gradlew test --tests '*RestControllerTestIT*'` passes.
- [ ] Full test suite passes: `./gradlew test`.
- [ ] The workflow transition requirement is met: every selected term has
  ordered candidate concepts or explicit no-match evidence.

## Test Scenarios

| Scenario | Expected Result |
|---|---|
| One term has exact label candidates | Exact candidates are returned with score `1.0`, alias lookup is not called, and ordering is deterministic. |
| One term has no exact candidates but has alias candidates | Alias candidates are returned with score `0.7` and alias evidence. |
| One term has no exact or alias candidates | Candidate list is empty and term-level no-match evidence names both attempted lookup methods. |
| Multiple selected terms | Result contains exactly one match per selected term. |
| Multiple exact candidates | Candidates preserve exact lookup order. |
| Future lower-weight adapters match the same candidate | Candidate score is the sum of matched lookup weights and remains below `1.0`. |
| Non-exact lookup weights can sum to `1.0` or more | Policy construction or configuration validation fails. |
| Duplicate selected terms | Domain or command validation rejects the request. |
| Missing requirement element or blank text | Command/domain validation rejects the request. |
| Lookup adapter returns hit without evidence text | Domain validation rejects the lookup hit. |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---:|---|
| Policy logic leaks into the command handler. | High | Handler delegates only to `ConceptRetrievalService`; tests verify it does not call lookup ports directly. |
| Lookup methods become hidden policies. | Medium | Lookup method ports expose weight and ordered candidates only; fallback, score aggregation, and stop rules live in `ConceptRetrievalPolicy`. |
| Alias graph shape changes later. | Medium | Keep alias lookup behind its own adapter and evidence name. |
| Specifications are introduced too early. | Low | Keep v1 conditions inside `OrderedWeightedConceptRetrievalPolicy`; extract Specifications only after repeated rules appear. |
| Retrieval result is mistaken for a concept match decision. | High | Domain result contains candidates and evidence only; concept match outcomes remain in `DECIDE_CONCEPT_MATCHES`. |

## Open Questions

None for this iteration. Future work may add Specifications, graph traversal,
embedding lookup, or alternative concrete retrieval policies after the ordered
weighted policy is implemented and tested.
