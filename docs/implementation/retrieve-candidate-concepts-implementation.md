# Implementation Plan: Retrieve Candidate Concepts Slice

## Overview

Implement the `RETRIEVE_CANDIDATE_CONCEPTS` workflow state as a focused candidate-retrieval slice. The slice accepts selected requirement syntax elements, applies a domain retrieval policy, queries the KG for candidate concepts, and returns a first-class `CandidateConceptMatchSet` containing retrieval evidence. V1 includes one policy implementation, `exactMatch`, which queries explicit graph-model nodes by exact text or label equality.

## Confirmed Boundary

`RETRIEVE_CANDIDATE_CONCEPTS` produces retrieval evidence for selected terms. `CREATE_MATCHING_CANDIDATES` consumes that evidence and creates mapping decisions, review items, or concept proposals.

## Architecture Decisions

- Add a new `conceptretrieval` slice using the existing `domain`, `application`, and `infrastructure` package pattern.
- Model `CandidateConceptMatchSet` as a domain artifact now so downstream states receive structured retrieval evidence instead of raw graph records.
- Represent the retrieval policy as an application/domain port with one CDI implementation named `exactMatch`.
- Keep Neo4j driver details in the infrastructure adapter.
- Treat exact graph candidate hits as retrieval evidence with deterministic rank/order. Matching interpretation belongs to the later matching slice.
- Represent no graph candidate hits as explicit no-match evidence for the selected element.

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
- [ ] `CandidateConcept` contains a graph concept id, label, and optional concept type.
- [ ] `RetrievalEvidence` contains retrieval policy name, evidence text, and optional numeric score.
- [ ] `CandidateConceptMatch` contains the selected term syntax role and text, zero or more candidates, and at least one evidence item.
- [ ] `CandidateConceptMatchSet` contains one match entry per selected term and rejects null entries.
- [ ] Domain tests cover trimming, required values, empty candidate lists for no-match evidence, and immutable collections.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*conceptretrieval.domain*'`

**Dependencies:** Task 1.

**Files likely touched:**
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
- [ ] `ConceptRetrievalPolicy` exposes a method such as `retrieveCandidates(Collection<RetrieveCandidateConceptsCommand.SelectedTermInput> terms)`.
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

**Description:** Implement `exactMatch` as the first retrieval policy. It queries the explicit graph model for candidates whose text, raw text, code, or label equals each selected term text and maps graph records to candidate evidence.

**Acceptance criteria:**
- [ ] `ExactMatchConceptRetrievalPolicy` is an `@ApplicationScoped` CDI bean implementing `ConceptRetrievalPolicy`.
- [ ] For each selected term, it queries Neo4j using explicit labels such as `SyntaxElement`, `Requirement`, `RequirementType`, `RequirementProperty`, `SyntaxRole`, and `RequirementRelationType`.
- [ ] Each graph hit becomes a `CandidateConcept` with evidence naming `exactMatch`.
- [ ] Each no-hit result becomes a `CandidateConceptMatch` with an empty candidate list and no-match evidence naming `exactMatch`.
- [ ] Query results are ordered deterministically by returned label then id.
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
- [ ] `RetrieveCandidateConceptsCommand` carries selected terms with syntax role and text only.
- [ ] The handler validates command payload terms and delegates them directly to `ConceptRetrievalPolicy`.
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
- [ ] The response includes candidate evidence for exact graph candidate hits.
- [ ] The response includes no-match evidence for terms without label hits.
- [ ] The integration test mocks `ConceptRetrievalPolicy`, not Neo4j.
- [ ] Existing classification and syntax extraction command tests continue to pass.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RestControllerTestIT*'`

**Dependencies:** Task 5.

**Files likely touched:**
- `src/test/java/io/fekav/platform/api/RestControllerTestIT.java`

**Estimated scope:** S.

### Task 7: Update KG Schema Support for Exact Candidate Lookup

**Description:** Ensure the graph has clear indexed or constrained lookup targets for retrieval. Add the smallest schema initializer change needed for exact matching against the explicit graph model.

**Acceptance criteria:**
- [ ] `Neo4jSchemaInitializer` creates indexes or uniqueness constraints that support the properties queried by `exactMatch`.
- [ ] The schema uses explicit graph labels and properties, not a generic `:Concept` bucket.
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
- [ ] A command can return exact graph candidates and no-match evidence through `/app/c`.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---:|---|
| Future domain concept labels are not finalized. | Medium | Query the current explicit graph model first and add new typed labels to `exactMatch` when the ontology vocabulary grows. |
| Existing Neo4j tests require a running container. | Medium | Keep adapter tests mocked for v1 and add container-backed coverage in a later persistence slice. |
| Retrieval evidence starts to encode matching decisions. | High | Keep decision statuses out of `CandidateConceptMatchSet` and verify this at the domain checkpoint. |
| Conditions and constraints may be optional or repeated. | Medium | V1 command accepts selected term role/text pairs directly; later workflow orchestration can decide which syntax elements are selected and how optional terms are represented. |

## Open Questions

- Which additional typed graph labels should become retrieval candidates after `SyntaxElement`, `Requirement`, and ontology vocabulary nodes?
- Should exact text/label matching be case-sensitive in v1? The plan assumes exact property equality unless the workflow policy says otherwise.
- Should the retrieval command be public through `/app/c` for manual testing, or only reachable by an internal orchestrator once workflow orchestration exists?
