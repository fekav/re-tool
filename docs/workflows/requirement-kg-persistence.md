# Requirement KG Persistence Workflow Contract

## Purpose
Persist textual requirements into a knowledge graph while preserving provenance, extracting requirement syntax, classifying requirements, and retrieving concept-name KG candidates for selected syntax terms. The current concept retrieval slice treats graph labels, text values, aliases, and alias lists as equivalent concept-name matches.

## Scope

### In Scope
- Persisting raw software requirements, business goals, stakeholder needs, text, and source provenance.
- Extracting requirement syntax elements: subject, object, action, condition, and constraint.
- Retrieving KG candidate concepts for selected syntax terms through the current concept-name retrieval policy.
- Creating term-to-concept mappings from candidate retrieval output.
- Applying domain policy to graph writes, concept creation proposals, human review, syntax extraction, classification, and concept retrieval.

### Non-goals
- Backwards compatibility with retired ordered-weighted retrieval internals.
- Defining the full enterprise ontology.
- Implementing natural-language parsing algorithms.
- Implementing concept matching or graph persistence inside the concept retrieval slice.

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Abort If |
|---|---|---|---|---|---|
| `INTAKE_REQUIREMENT` (START) | Create the requirement boundary and preserve its source. | Raw requirement text; source metadata. | Validate non-blank `RawText`; create `Requirement`; attach provenance. | `Requirement` with `ElementId` and `RawText`; `Provenance`. | Raw text is blank, source provenance is missing, or requirement identity cannot be created. |
| `CLASSIFY_REQUIREMENT` | Classify raw text on intent and property axes. | `RawText` from `Requirement`; active classifier. | Call `classify_requirement(rawText)`; apply `Requirement.applyClassification(classification)`. | `Classification` on `Requirement`: `RequirementType`, `RequirementProperty`, `ConfidenceScore`, `Rationale`. | Classifier is unavailable, classification is incomplete, or policy requires review before continuing. |
| `EXTRACT_REQUIREMENT_SYNTAX` | Extract the requirement action model from raw text. | `RawText` from `Requirement`; active syntax extractor. | Call `extract_requirement_syntax(rawText)`; apply `Requirement.applyExtraction(action)`. | `Action` on `Requirement`: `Subject`, action text, `TargetObject`, optional `Condition`, optional `Constraint`. | Extractor is unavailable, required action fields are missing, or policy requires review before continuing. |
| `RETRIEVE_CANDIDATE_CONCEPTS` | Return one candidate match result for each selected syntax term. | Unique selected syntax term role/text pairs; active `ConceptRetrievalService`; active `ConceptRetrievalPolicy`; available `CandidateLookup`. | Validate selected terms; call `RetrieveCandidateConceptsCommandHandler`; map inputs to `SelectedTerm`; call `ConceptRetrievalService.retrieveCandidates(selectedTerms)`; apply `ConceptNameRetrievalPolicy`; call `CandidateLookup.findCandidates(selectedTerm)` once per term; de-duplicate duplicate candidate keys keeping the first occurrence. | `CandidateConceptMatchSet` containing exactly one `CandidateConceptMatch` per `SelectedTerm`; each match has zero or more `RetrievedCandidateConcept` values. | Selected terms are empty, blank, duplicated, or null; lookup dependency is unavailable; retrieval policy returns a match for a different selected term; candidate data lacks candidate key or label. |
| `CREATE_MATCHING_CANDIDATES` | Interpret retrieved candidates into mapping outcomes, review items, and concept proposals. | `CandidateConceptMatchSet`; active matching and concept creation policy. | Evaluate retrieved candidates; treat empty candidate lists as lookup misses; create mapping decisions or concept proposals. | `MappingDecisionSet`; optional `ConceptProposalSet`. | Matching policy is missing, contradictory, or cannot classify empty-candidate cases. |
| `REQUEST_HUMAN_REVIEW` | Resolve policy-directed review items. | Pending `MappingDecisionSet` entries, pending `ConceptProposalSet` entries, reviewer role, review payload. | Request reviewer decision; validate response shape; apply approved corrections, rejections, merges, deferrals, or concept proposal decisions. | Reviewed `MappingDecisionSet`; approved, rejected, merged, deferred, or corrected `ConceptProposalSet`. | Required reviewer is unavailable, response shape is invalid, or required rationale is missing. |
| `PERSIST_GRAPH_CHANGES` | Persist approved requirement graph changes atomically. | `Requirement`; `Provenance`; `Classification`; `Action`; resolved `MappingDecisionSet`; approved `ConceptProposalSet` if any. | Build graph write set; write idempotently to KG; commit transaction. | KG transaction result containing persisted identifiers and relationships. | KG is unavailable, unresolved mapping decisions remain, concept creation lacks required approval, or the transaction cannot commit. |
| `DONE` (TERMINAL) | End after committed KG persistence. | KG transaction result. | Summarize persisted identifiers and completed states. | Completed workflow summary. | Not applicable. |
| `ABORTED` (TERMINAL) | End when the workflow cannot preserve its invariants. | Abort reason; last valid state. | Record abort reason and last valid state. | Aborted workflow summary. | Not applicable. |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| `INTAKE_REQUIREMENT` | `RawText` is non-blank and provenance is recorded. | `CLASSIFY_REQUIREMENT` | No | - |
| `CLASSIFY_REQUIREMENT` | Classification has `RequirementType`, `RequirementProperty`, `ConfidenceScore`, and `Rationale`, and policy allows automatic continuation. | `EXTRACT_REQUIREMENT_SYNTAX` | No | Low confidence may still route to review by policy. |
| `CLASSIFY_REQUIREMENT` | Classification is incomplete or policy requires review. | `REQUEST_HUMAN_REVIEW` | Yes: configured reviewer must resolve classification. | Reviewer response must include corrected classification or a decision to stop. |
| `EXTRACT_REQUIREMENT_SYNTAX` | `Action` contains `Subject`, action text, and `TargetObject`, and policy allows automatic continuation. | `RETRIEVE_CANDIDATE_CONCEPTS` | No | Optional `Condition` and `Constraint` values can be absent. |
| `EXTRACT_REQUIREMENT_SYNTAX` | Required `Action` element is missing or policy requires review. | `REQUEST_HUMAN_REVIEW` | Yes: configured reviewer must resolve extraction. | Reviewer may accept, edit, delete, or add terms. |
| `RETRIEVE_CANDIDATE_CONCEPTS` | `CandidateConceptMatchSet` contains one match for every selected term. | `CREATE_MATCHING_CANDIDATES` | No | Empty candidate lists represent current lookup misses. Retrieval does not emit separate term-level miss evidence. |
| `CREATE_MATCHING_CANDIDATES` | Matching policy resolves every selected term automatically. | `PERSIST_GRAPH_CHANGES` | No | Includes accepted matches and policy-approved empty-candidate handling. |
| `CREATE_MATCHING_CANDIDATES` | Any candidate match, ambiguity, or empty-candidate case requires review. | `REQUEST_HUMAN_REVIEW` | Yes: configured reviewer must resolve pending mapping or proposal. | - |
| `REQUEST_HUMAN_REVIEW` | Reviewer approves or corrects pending classification. | `EXTRACT_REQUIREMENT_SYNTAX` | Yes: reviewer response required before transition. | - |
| `REQUEST_HUMAN_REVIEW` | Reviewer approves, edits, deletes, or adds extracted syntax elements. | `RETRIEVE_CANDIDATE_CONCEPTS` | Yes: reviewer response required before transition. | - |
| `REQUEST_HUMAN_REVIEW` | Reviewer supplies an allowed decision for every pending concept matching or concept proposal. | `PERSIST_GRAPH_CHANGES` | Yes: reviewer response required before transition. | - |
| `PERSIST_GRAPH_CHANGES` | KG transaction commits and returns persisted identifiers. | `DONE` | No | - |
| `ANY_STATE` | Required input is missing, dependency is unavailable, active policy is invalid, reviewer response is invalid, or a workflow invariant is violated. | `ABORTED` | No | Abort reason and last valid state must be recorded. |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| `Provenance` | `INTAKE_REQUIREMENT` | All later states | KG node or transaction payload | Contains id, raw text, source identifier, and ingestion timestamp. |
| `Classification` | `CLASSIFY_REQUIREMENT` | `PERSIST_GRAPH_CHANGES` | KG relationship or transaction payload | `conceptType` is one of the configured `RequirementType` values, `property` is one of the configured `RequirementProperty` values, `confidenceScore` is between `0.0` and `1.0`, and `rationale` is non-blank. |
| `Action` | `EXTRACT_REQUIREMENT_SYNTAX` | `RETRIEVE_CANDIDATE_CONCEPTS`; `PERSIST_GRAPH_CHANGES` | Domain model linked to requirement id | Contains `Subject`, action text, `TargetObject`, and optional `Condition` and `Constraint` values. |
| `SelectedTerm` | `RETRIEVE_CANDIDATE_CONCEPTS` | `ConceptRetrievalService`; `ConceptRetrievalPolicy`; `CandidateLookup` | Domain value object with `syntaxRole` and `text` | `syntaxRole` and `text` are non-blank after trimming; duplicate command inputs are rejected before retrieval. |
| `CandidateConceptMatchSet` | `RETRIEVE_CANDIDATE_CONCEPTS` | `CREATE_MATCHING_CANDIDATES` | Structured collection returned by `ConceptRetrievalService.retrieveCandidates` | Contains one `CandidateConceptMatch` per selected term; selected terms are unique; each match may contain zero or more retrieved candidates. |
| `RetrievedCandidateConcept` | `RETRIEVE_CANDIDATE_CONCEPTS` | `CREATE_MATCHING_CANDIDATES` | Domain value containing `CandidateConcept` and retrieval evidence | Each retrieved candidate has non-empty evidence; current concept-name evidence uses `policyName = "conceptName"` and score `1.0`. |
| `CandidateLookup` | Infrastructure adapter | `ConceptNameRetrievalPolicy` | Domain port implemented by `Neo4jConceptNameLookup` | `findCandidates(selectedTerm)` returns graph candidates for one selected term; it must not decide mapping outcomes. |
| `TermConceptMappingSet` | `CREATE_MATCHING_CANDIDATES` | `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to terms and candidate concepts | Every selected element has a mapping status and rationale. |
| `ConceptCreationPolicy` | Configuration | `CREATE_MATCHING_CANDIDATES` | Configured policy document or runtime configuration | Defines behavior for empty-candidate, low-confidence, ambiguous, and concept-type-specific cases. |
| `MappingDecisionSet` | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW` | `PERSIST_GRAPH_CHANGES` | Structured collection linked to mappings | No item remains undecided before persistence, unless persisted as a pending review state by policy. |
| `ConceptProposalSet` | `CREATE_MATCHING_CANDIDATES` | `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to term mentions | Every proposal has label, proposed type, provenance, confidence, and lookup-miss rationale. |
| KG transaction result | `PERSIST_GRAPH_CHANGES` | `DONE` | Neo4j transaction result or equivalent KG write receipt | Contains persisted identifiers for requirement and all written graph entities. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| Resolve ambiguous mapping | A term has multiple plausible concept matches above threshold. | Domain modeler, requirement analyst, software developer, or test engineer as configured by policy | `APPROVE_MATCH`, `REJECT_MATCH`, `SELECT_DIFFERENT_CONCEPT`, `PROPOSE_NEW_CONCEPT` | Term id, selected option, concept id if applicable, and short rationale. |
| Approve new concept proposal | A selected term has an empty candidate list and policy requires approval for concept creation. | Domain modeler by default, unless policy assigns another role. | `APPROVE_CREATE`, `REJECT_CREATE`, `MERGE_WITH_EXISTING`, `DEFER` | Proposal id, selected option, concept id for merge, and concept type if approved. |
| Override extraction | Extracted syntax is wrong or incomplete during review. | Requirement analyst or domain modeler | `ACCEPT_EXTRACTION`, `EDIT_TERM`, `DELETE_TERM`, `ADD_TERM` | Term id or new term text, syntax role, source span, and rationale. |

## Abort Conditions
- Abort if raw requirement text or source provenance is missing.
- Abort if selected terms for concept retrieval are empty, blank, duplicated, or null.
- Abort if the KG is unavailable during concept-name lookup or graph persistence.
- Abort if `ConceptRetrievalPolicy` returns a `CandidateConceptMatch` for a different `SelectedTerm`.
- Abort if candidate data returned by lookup lacks a non-blank candidate key or label.
- Abort if a required active policy is missing or contradictory.
- Abort if a required human reviewer cannot provide a response in the required shape.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Which concept creation policy should be the default for v1? | The workflow cannot safely decide whether empty-candidate terms become new concepts, proposals, pending review items, or blocked items. | `PROPOSE_ONLY` for all new concepts; `REQUIRE_APPROVAL` for domain concepts; `BLOCK` for unknown concept types. | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` |
| Should lookup misses remain represented only as empty candidate lists, or should a later matching stage create explicit miss rationale? | Current retrieval returns empty `candidates` for a miss and does not emit term-level miss evidence. Downstream matching still needs an auditable reason for proposals or review items. | Keep retrieval simple and let matching create proposal rationale; add explicit miss evidence in a later retrieval policy; persist empty candidates as-is. | `CREATE_MATCHING_CANDIDATES`; `ConceptCreationPolicy` |
| Which non-classification concept types are supported in the first KG schema? | Extraction and mapping need a bounded target vocabulary beyond the fixed classification concept types. | Classification starts with `REQUIREMENT`, `GOAL`, and `NEED`; retrieval may also need implementation-facing concepts such as `SystemComponent` and `UIComponent`. | `RETRIEVE_CANDIDATE_CONCEPTS`; `CREATE_MATCHING_CANDIDATES` |
| Who owns review for each decision type? | Human review transitions need a concrete owner role. | Domain modeler owns concept creation; requirement analyst owns extraction corrections; software developer owns system component mappings; test engineer owns quality and constraint mappings. | `REQUEST_HUMAN_REVIEW` |
| Are pending mappings allowed to be persisted? | Persistence policy must decide whether incomplete decisions are durable review states or blockers. | Persist pending review state so the requirement is available for later completion. | `PERSIST_GRAPH_CHANGES` |

## Assumption Log

| Assumption | Source | Risk Level | Impact If Wrong | How To Validate |
|---|---|---|---|---|
| The current concept retrieval implementation is the source of truth for this workflow's retrieval state. | `src/main/java/io/fekav/req/conceptretrieval` | LOW | The workflow may describe behavior that no longer exists. | Re-run the retired-identifier scan from the verification checklist for this documentation change. |
| Concept-name retrieval treats `candidate.label`, `candidate.text`, `candidate.alias`, and `candidate.aliases` as equivalent name matches. | `Neo4jConceptNameLookup` | LOW | A later matching policy could incorrectly distinguish alias and label scores. | Verify the Neo4j lookup predicate and adapter tests before changing score semantics. |
| Every retrieved concept-name candidate receives one `RetrievalEvidence` entry with `policyName = "conceptName"` and score `1.0`. | `ConceptNameRetrievalPolicy` | LOW | Downstream matching thresholds could be calibrated against retired weighted lookup scores. | Verify `ConceptNameRetrievalPolicyTest` when matching thresholds are introduced. |
| Duplicate candidate keys are de-duplicated by the retrieval policy, keeping the first candidate in lookup order. | `ConceptNameRetrievalPolicy` | LOW | Duplicate graph results could produce ambiguous duplicate mapping options. | Verify de-duplication tests and Neo4j ordering when changing lookup queries. |
| `CandidateLookup` is intentionally a retrieval port, not a mapping-decision port. | `CandidateLookup`; `ConceptRetrievalPolicy` | MEDIUM | New adapters could start embedding matching policy and make behavior inconsistent. | Keep mapping thresholds and concept creation behavior in `CREATE_MATCHING_CANDIDATES` or explicit matching policy code. |
| Empty candidate lists are the current lookup-miss representation. | `ConceptNameRetrievalPolicy`; `CandidateConceptMatch` | MEDIUM | Concept proposal audit trails may be incomplete if no later stage adds rationale. | Resolve the open question about no-match rationale before implementing persistence of proposals. |
| The primary users are requirement analysts, test engineers, software developers, and domain modelers. | User input. | LOW | Review roles and query needs may be incomplete. | Confirm role ownership in the concept creation policy. |
| The first success criterion is committed KG persistence of the requirement and its derived graph artifacts. | User input. | LOW | Completion could target the wrong outcome. | Confirm the KG transaction output required by `DONE`. |
| New concept creation behavior should be configurable by policy. | User input. | MEDIUM | A hard-coded behavior could violate governance expectations. | Create and review `ConceptCreationPolicy` before persistence behavior is implemented. |
| The KG already contains or will contain classification concepts such as requirement, goal, and need, plus retrieval concepts such as system component and UI component. | User input. | MEDIUM | Retrieval and classification may fail if these concept types are absent or differently named. | Validate supported concept types against the KG schema. |
| Requirement syntax can be represented as subject, object, action, condition, and constraint. | User-provided workflow outline. | MEDIUM | Important requirement semantics may be missed, such as actor, trigger, exception, priority, or acceptance criteria. | Test extraction against representative real requirements. |
