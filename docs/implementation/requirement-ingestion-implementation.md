# Implementation Plan: Requirement Ingestion

## Overview

Define the first workflow state as `INGEST_REQUIREMENT`. This state establishes
provenance and prepares a policy-normalized handoff for downstream processing.
It does not persist a requirement, deduplicate, resolve identity, classify,
extract syntax, retrieve KG concepts, or map terms.

## Confirmed Intent

- Outcome: Define the first workflow state as `INGEST_REQUIREMENT`, replacing intake naming.
- User: Downstream workflow states that need a clean, provenance-backed handoff.
- Why now: The current workflow doc incorrectly frames the first state as creating or persisting a requirement boundary.
- Success: Ingestion outputs a `RequirementIngestion` handoff with required provenance: `sourceType`, `sourceId`, and text.
- Constraint: Ingestion must not decide whether the requirement is new, already persisted, deduplicated, classified, extracted, or mapped.
- Normalization: Ingestion may produce normalized/canonical text, but the behavior is controlled by domain policy and must preserve provenance.
- Out of scope: KG persistence, requirement identity resolution, classification, syntax extraction, concept retrieval, and matching.

## Architecture Decisions

- Rename `INTAKE_REQUIREMENT` to `INGEST_REQUIREMENT`.
- Introduce `RequirementIngestion` as the handoff artifact, not as the canonical `Requirement`.
- Require `sourceType`, `sourceId`, and `rawText` for ingestion.
- Preserve original text and produce normalized text through domain policy.
- Leave any already-persisted versus not-yet-persisted requirement handling to later states.

## Task List

### Task 1: Fix Workflow Terminology And State Contract

**Description:** Update `docs/workflows/requirement-kg-persistence.md` so the
start state is ingestion, provenance-focused, and no longer claims to create or
persist a `Requirement`.

**Acceptance criteria:**
- [ ] `INTAKE_REQUIREMENT` is replaced with `INGEST_REQUIREMENT`.
- [ ] State goal says provenance and handoff preparation, not requirement persistence.
- [ ] Output is `RequirementIngestion`, including original text, normalized text, and provenance.
- [ ] Transition to `CLASSIFY_REQUIREMENT` requires valid provenance and policy-normalized text.
- [ ] Persistence language is moved out of ingestion and left to later states.

**Verification:**
- [ ] `rg -n "INTAKE|intake" docs/workflows/requirement-kg-persistence.md` returns no stale intake references.
- [ ] Review confirms no ingestion row mentions KG writes, deduplication, or canonical requirement creation.

**Dependencies:** None

**Files likely touched:**
- `docs/workflows/requirement-kg-persistence.md`

**Estimated scope:** Small

### Task 2: Align Glossary With Ingestion Boundary

**Description:** Update glossary terms so `RawText`, `Provenance`, state names,
and policy names match the new ingestion semantics.

**Acceptance criteria:**
- [ ] `INTAKE_REQUIREMENT` glossary entry becomes `INGEST_REQUIREMENT`.
- [ ] `RawText` distinguishes original received text from normalized text.
- [ ] `Provenance` requires `sourceType` and `sourceId`.
- [ ] `RAW_TEXT_POLICY` is replaced or reframed as `INGESTION_POLICY`.

**Verification:**
- [ ] `rg -n "INTAKE|intake" docs` only returns historical notes if intentionally kept.
- [ ] Glossary descriptions do not imply ingestion persists KG artifacts.

**Dependencies:** Task 1

**Files likely touched:**
- `docs/glossary.md`

**Estimated scope:** Small

### Task 3: Add Ingestion Domain Types

**Description:** Add a small ingestion slice that models the first state without
mutating `Requirement`.

**Acceptance criteria:**
- [ ] Add `RequirementIngestion` with `provenance`, `originalText`, and `normalizedText`.
- [ ] Add provenance value type with required `sourceType` and `sourceId`.
- [ ] Blank or null `sourceType`, `sourceId`, or `rawText` fails fast.
- [ ] Existing `Requirement.create(...)` behavior is not changed in this task.

**Verification:**
- [ ] Unit tests cover valid ingestion, missing source type, missing source id, blank raw text, and preservation of original text.
- [ ] Existing `RawRequirementTextTest` and `RequirementTest` still pass.

**Dependencies:** Tasks 1-2

**Files likely touched:**
- `src/main/java/io/fekav/req/ingestion/domain/RequirementIngestion.java`
- `src/main/java/io/fekav/req/ingestion/domain/SourceProvenance.java`
- `src/test/java/io/fekav/req/ingestion/domain/*Test.java`

**Estimated scope:** Medium

### Task 4: Add Policy-Controlled Normalization Boundary

**Description:** Implement normalization as a domain/application policy boundary,
so ingestion does not hard-code cleanup semantics.

**Acceptance criteria:**
- [ ] Add a `RequirementTextNormalizationPolicy` or equivalent port.
- [ ] Ingestion calls the policy to produce `normalizedText`.
- [ ] The default v1 policy is explicit and conservative until configured otherwise.
- [ ] Original raw text remains unchanged in the handoff.

**Verification:**
- [ ] Tests prove policy output is used.
- [ ] Tests prove original text is preserved even when normalized text differs.

**Dependencies:** Task 3

**Files likely touched:**
- `src/main/java/io/fekav/req/ingestion/application/*`
- `src/main/java/io/fekav/req/ingestion/domain/*`
- `src/test/java/io/fekav/req/ingestion/application/*Test.java`

**Estimated scope:** Medium

### Task 5: Add Ingest Command Handler

**Description:** Add the application use case for `INGEST_REQUIREMENT`.

**Acceptance criteria:**
- [ ] `IngestRequirementCommand` accepts `sourceType`, `sourceId`, and `rawText`.
- [ ] Handler returns `RequirementIngestion`.
- [ ] Handler does not call repositories, KG adapters, classification services, or syntax extraction.
- [ ] Handler publishes no persistence event unless a separate non-persistence ingestion event is intentionally introduced.

**Verification:**
- [ ] Handler tests verify returned handoff fields.
- [ ] Handler tests verify no downstream service is invoked.

**Dependencies:** Task 4

**Files likely touched:**
- `src/main/java/io/fekav/req/ingestion/application/IngestRequirementCommand.java`
- `src/main/java/io/fekav/req/ingestion/application/IngestRequirementCommandHandler.java`
- `src/test/java/io/fekav/req/ingestion/application/IngestRequirementCommandHandlerTest.java`

**Estimated scope:** Medium

## Checkpoint

- [ ] `./gradlew test --tests '*ingestion*'`
- [ ] `./gradlew test`
- [ ] Documentation and code agree that ingestion is provenance plus normalized handoff only.

## Explicitly Out Of Scope

- Refactoring classification or extraction to consume `RequirementIngestion`.
- Adding KG persistence.
- Deduplication or identity resolution.
- Human review, concept matching, retrieval, or graph write policies.

## Risks And Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Ingestion drifts into persistence or identity resolution. | High | Keep repository/KG dependencies out of the ingestion handler. |
| Normalization changes source meaning. | High | Preserve original text and make normalization policy explicit and tested. |
| Existing `RawText` semantics conflict with original-text preservation. | Medium | Treat `RawText` as normalized processing text or introduce a separate original-text value. |
| Later states still create `Requirement` directly. | Medium | Keep that refactor out of this plan unless a later workflow task explicitly addresses handoff consumption. |

## Decisions From Review

- `RequirementIngestion` should raise a domain event.
- The first normalization policy should be conservative.
