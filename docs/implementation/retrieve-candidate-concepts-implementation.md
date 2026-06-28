# Implementation Plan: Retrieve Candidate Concepts Slice

## Overview

Implement the `RETRIEVE_CANDIDATE_CONCEPTS` workflow state as a focused candidate-retrieval slice. The slice accepts selected requirement syntax elements, applies a domain retrieval policy, queries the KG for candidate concepts, and returns a first-class `CandidateConceptMatchSet` containing retrieval evidence. V1 includes one policy implementation, `exactMatch`, which queries graph concepts by label.

## Confirmed Boundary

`RETRIEVE_CANDIDATE_CONCEPTS` produces retrieval evidence for selected terms. `CREATE_MATCHING_CANDIDATES` consumes that evidence and creates mapping decisions, review items, or concept proposals.

## Architecture Decisions

- Add a new `conceptretrieval` slice using the existing `domain`, `application`, and `infrastructure` package pattern.
- Model `CandidateConceptMatchSet` as a domain artifact now so downstream states receive structured retrieval evidence instead of raw graph records.
- Represent the retrieval policy as an application/domain port with one CDI implementation named `exactMatch`.
- Keep Neo4j driver details in the infrastructure adapter.
- Treat exact label hits as candidate evidence with deterministic rank/order. Matching interpretation belongs to the later matching slice.
- Represent no label hits as explicit no-match evidence for the selected element.

## Dependency Graph

```text
Workflow contract clarification
    -> Candidate retrieval domain model
        -> Retrieval policy port
            -> exactMatch Neo4j adapter
                -> RetrieveCandidateConcepts use case
                    -> REST command dispatch coverage
```

## Task List

### Task 1: Clarify Workflow State Boundaries

**Description:** Update `docs/workflows/requirement-kg-persistence.md` so the retrieval and matching states describe positive responsibilities. Replace ambiguous retrieval/matching language with direct statements of the intended boundary.

**Acceptance criteria:**
- [ ] `RETRIEVE_CANDIDATE_CONCEPTS` is described as producing ranked concept candidates or no-match evidence for each selected element.
- [ ] `CREATE_MATCHING_CANDIDATES` is described as interpreting candidate evidence into mapping outcomes, review items, and concept proposals.
- [ ] The `CandidateConceptMatchSet` artifact validation states that every selected element has retrieval evidence.
- [ ] The doc uses positive replacement language for the new boundary.

**Verification:**
- [ ] Review `docs/workflows/requirement-kg-persistence.md` for the updated state table, transition language, and artifact description.

**Dependencies:** None.

**Files likely touched:**
- `docs/workflows/requirement-kg-persistence.md`

**Estimated scope:** XS.

### Task 2: Add Candidate Retrieval Domain Model

**Description:** Add minimal domain records that capture selected term evidence without making mapping decisions.

**Acceptance criteria:**
- [ ] `SelectedConceptTerm` contains an `ElementId`, syntax role, and non-blank term text.
- [ ] `CandidateConcept` contains a graph concept id, label, and optional concept type.
- [ ] `RetrievalEvidence` contains retrieval policy name, evidence text, and optional numeric score.
- [ ] `CandidateConceptMatch` links one selected term to zero or more candidates and at least one evidence item.
- [ ] `CandidateConceptMatchSet` contains one match entry per selected term and rejects null entries.
- [ ] Domain tests cover trimming, required values, empty candidate lists for no-match evidence, and immutable collections.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*conceptretrieval.domain*'`

**Dependencies:** Task 1.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/domain/SelectedConceptTerm.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConcept.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/RetrievalEvidence.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatch.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatchSet.java`
- `src/test/java/io/fekav/req/conceptretrieval/domain/CandidateConceptMatchSetTest.java`

**Estimated scope:** M.

### Checkpoint: Domain Boundary

- [ ] Workflow doc states the retrieval/matching boundary in positive terms.
- [ ] Candidate retrieval domain tests pass.
- [ ] No matching decision statuses exist in the retrieval domain model.

### Task 3: Add Retrieval Policy Port

**Description:** Add the policy interface used by the retrieval use case. The interface receives selected terms and returns a `CandidateConceptMatchSet`.

**Acceptance criteria:**
- [ ] `ConceptRetrievalPolicy` exposes a method such as `retrieveCandidates(Collection<SelectedConceptTerm> terms)`.
- [ ] The port returns `CandidateConceptMatchSet`.
- [ ] The port accepts selected syntax terms without depending on `Action` directly.
- [ ] Application tests can mock the port without Neo4j.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RetrieveCandidateConceptsCommandHandler*'`

**Dependencies:** Task 2.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/ConceptRetrievalPolicy.java`

**Estimated scope:** XS.

### Task 4: Implement exactMatch Neo4j Policy

**Description:** Implement `exactMatch` as the first retrieval policy. It queries the graph for concepts whose label equals each selected term text and maps graph records to candidate evidence.

**Acceptance criteria:**
- [ ] `ExactMatchConceptRetrievalPolicy` is an `@ApplicationScoped` CDI bean implementing `ConceptRetrievalPolicy`.
- [ ] For each selected term, it queries Neo4j for concepts with `label = term.text()`.
- [ ] Each graph hit becomes a `CandidateConcept` with evidence naming `exactMatch`.
- [ ] Each no-hit result becomes a `CandidateConceptMatch` with an empty candidate list and no-match evidence naming `exactMatch`.
- [ ] Query results are ordered deterministically, for example by concept label then id.
- [ ] Neo4j records and driver types stay inside `conceptretrieval.infrastructure`.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*ExactMatchConceptRetrievalPolicy*'`
- [ ] Adapter tests use a mocked `Driver`/`Session` or a focused test double around query execution.

**Dependencies:** Task 3.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/infrastructure/ExactMatchConceptRetrievalPolicy.java`
- `src/test/java/io/fekav/req/conceptretrieval/infrastructure/ExactMatchConceptRetrievalPolicyTest.java`

**Estimated scope:** M.

### Task 5: Add Retrieve Candidate Concepts Use Case

**Description:** Add a command and handler that accepts selected term inputs, delegates to the retrieval policy, and returns the `CandidateConceptMatchSet`.

**Acceptance criteria:**
- [ ] `RetrieveCandidateConceptsCommand` carries selected terms with element id, syntax role, and text.
- [ ] The handler validates and maps command payload terms into `SelectedConceptTerm`.
- [ ] The handler delegates once to `ConceptRetrievalPolicy`.
- [ ] The handler returns `CandidateConceptMatchSet`.
- [ ] The handler keeps raw graph query data out of the response.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RetrieveCandidateConceptsCommandHandler*'`

**Dependencies:** Task 4.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommand.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandler.java`
- `src/test/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandlerTest.java`

**Estimated scope:** S.

### Task 6: Add REST Command Dispatch Coverage

**Description:** Prove the existing `/app/c` command endpoint can dispatch the new retrieval command through the command registry.

**Acceptance criteria:**
- [ ] Posting `RetrieveCandidateConceptsCommand` returns a serialized `CandidateConceptMatchSet`.
- [ ] The response includes candidate evidence for exact label hits.
- [ ] The response includes no-match evidence for terms without label hits.
- [ ] The integration test mocks `ConceptRetrievalPolicy`, not Neo4j.
- [ ] Existing classification and syntax extraction command tests continue to pass.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RestControllerTestIT*'`

**Dependencies:** Task 5.

**Files likely touched:**
- `src/test/java/io/fekav/platform/api/RestControllerTestIT.java`

**Estimated scope:** S.

### Task 7: Update KG Schema Support for Concept Labels

**Description:** Ensure the graph has a clear indexed label lookup target for retrieval. Add the smallest schema initializer change needed for exact label matching.

**Acceptance criteria:**
- [ ] `Neo4jSchemaInitializer` creates an index or uniqueness constraint that supports lookup by concept label.
- [ ] The schema uses the concept label property queried by `exactMatch`.
- [ ] Existing requirement, classification, and syntax schema initialization remains unchanged.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*Neo4jSchemaInitializer*'` if schema tests exist.
- [ ] Build succeeds: `./gradlew build`.

**Dependencies:** Task 4.

**Files likely touched:**
- `src/main/java/io/fekav/req/kg/Neo4jSchemaInitializer.java`

**Estimated scope:** XS.

### Checkpoint: End-to-End Slice

- [ ] `./gradlew test --tests '*conceptretrieval*'` passes.
- [ ] `./gradlew test --tests '*RestControllerTestIT*'` passes.
- [ ] `./gradlew build` succeeds.
- [ ] A command can return exact label candidates and no-match evidence through `/app/c`.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---:|---|
| The graph concept label schema is not finalized. | Medium | Use a minimal `:Concept {id, label, type}` query contract in the adapter and document it in Task 7. |
| Existing Neo4j tests require a running container. | Medium | Keep adapter tests mocked for v1 and add container-backed coverage in a later persistence slice. |
| Retrieval evidence starts to encode matching decisions. | High | Keep decision statuses out of `CandidateConceptMatchSet` and verify this at the domain checkpoint. |
| Conditions and constraints currently lack `ElementId`. | Medium | V1 command accepts selected terms directly; later workflow orchestration can decide which syntax elements are selected and how optional terms are identified. |

## Open Questions

- What exact graph label and properties represent domain concepts in the first KG schema: `:Concept {id, label, type}` or a more specific label per concept type?
- Should exact label matching be case-sensitive in v1? The plan assumes exact property equality unless the workflow policy says otherwise.
- Should the retrieval command be public through `/app/c` for manual testing, or only reachable by an internal orchestrator once workflow orchestration exists?
