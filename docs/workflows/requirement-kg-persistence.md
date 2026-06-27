# Requirement KG Persistence Workflow Contract

## Purpose
Persist textual requirements into a knowledge graph while preserving provenance, extracting requirement syntax, classifying requirements, and mapping them to KG concepts, while applying configured policies that control behaviour.

## Version

Note that this workflow describes the very first iterations of the implementation.
## Scope

### In Scope
- Persisting raw software requirements, business goals, stakeholder needs, text, and source provenance.
- Extracting requriement syntax elements (subject, object, action, condition, constraint)
- Retrieving matching candidate concepts from the KG.
- Creating term-to-concept mappings.
- Applying domain policy, describing domain decisions, for example: when to create graph nodes, when a concept match occurs, or when llm-backed requirement classification/syntaxextraction should be applied on aggregate root `Requirement`

### Non-goals
- Backwards-compatibility or production-ready concerns
- Defining the full enterprise ontology.
- Implementing natural-language parsing algorithms.

## State Table
| state | goal | input | function | output | policy |
|---|---|---|---|---|---|
| `INTAKE_REQUIREMENT` (START) | Create the requirement boundary and preserve its source. | Raw requirement text; source metadata. | `create_requirement(rawText, sourceMetadata)` | `Requirement` with `ElementId` and `RawText`; `Provenance`. | `RawText` is non-blank and provenance is recorded before any derived artifact exists. |
| `CLASSIFY_REQUIREMENT` | Classify raw text on intent and property axes. | `RawText` from `Requirement`. | `classify_requirement(rawText)` and `Requirement.applyClassification(classification)` | `Classification` on `Requirement`: `RequirementType`, `RequirementProperty`, `ConfidenceScore`, `Rationale`. | `CLASSIFICATION_POLICY`: low confidence is a review signal |
| `EXTRACT_REQUIREMENT_SYNTAX` | Extract the requirement action model from raw text. | `RawText` from `Requirement`. | `extract_requirement_syntax(rawText)` and `Requirement.applyExtraction(action)` | `Action` on `Requirement`: `Subject`, action text, `TargetObject`, optional `Condition`, optional `Constraint`. | `SYNTAX_EXTRACTION_POLICY`: low confidence is a review signal. |
| `RETRIEVE_CANDIDATE_CONCEPTS` | Retrieve KG concept candidates for each element selected for matching. | `Requirement.ElementId`; `Action` element ids and text values. | `retrieve_candidate_concepts(elementIds)` | `CandidateConceptMatchSet` with ranked matches or explicit no-match evidence per element. | `CONCEPT_RETRIEVAL_POLICY`: hybrid approach with graph traversal, alias queries, similarity search with embeddings/vectors   |
| `CREATE_MATCHING_CANDIDATES` | Create term-to-concept matchings. | `CandidateConceptMatchSet` | `create_matching_candidates(candidateConceptMatchSet)` | TBD | `MATCHING_POLICY` : exact, probable, ambiguous, and no-match outcomes resolve to approve, reject, propose, create, review, or block. |
| `REQUEST_HUMAN_REVIEW` | Resolve policy-directed review items. | `DecisionSet` entries marked `PENDING_REVIEW`; reviewer role; review payload. | `request_human_review(pendingDecisions)` | Reviewed `MappingDecisionSet`; approved, rejected, merged, deferred, or corrected `ConceptProposalSet`. | `HUMAN_REVIEW_POLICY`: review owner, allowed response shape, and due condition come from the active policy. |
| `PERSIST_GRAPH_CHANGES` | Persist approved requirement graph changes atomically. | `Requirement`; `Provenance`; `Classification`; `Action`; resolved `MappingDecisionSet`. | `persist_graph_changes(requirementGraphWriteSet)` | KG transaction result containing persisted identifiers and relationships. | `GRAPH_PERSISTENCE_POLICY`: writes are idempotent; new concepts require policy approval; pending review state is persisted only when policy allows it. |
| `DONE` (TERMINAL) | End after committed KG persistence. | KG transaction result. | `complete_workflow(kgTransactionResult)` | Completed workflow summary. | `COMPLETION_POLICY`: completion requires a committed KG transaction with persisted identifiers. |
| `ABORTED` (TERMINAL) | End when the workflow cannot preserve its invariants. | Abort reason; last valid state. | `abort_workflow(abortReason, lastValidState)` | Aborted workflow summary. | `STOP_POLICY`: stop only for missing required input, unavailable dependency, invalid policy or reviewer response, or violated invariant. |

## Transition Table

| from | condition | to |
|---|---|---|
| `INTAKE_REQUIREMENT` | `RawText` policy: raw text is non-blank and provenance is recorded. | `CLASSIFY_REQUIREMENT` |
| `CLASSIFY_REQUIREMENT` | `CLASSIFICATION_POLICY`: `classificationConfidence >= autoAcceptThreshold` and the classification has `RequirementType`, `RequirementProperty`, `ConfidenceScore`, and `Rationale`. | `EXTRACT_REQUIREMENT_SYNTAX` |
| `CLASSIFY_REQUIREMENT` | `CLASSIFICATION_POLICY`: `classificationConfidence < reviewThreshold` or the classifier returns an incomplete classification. | `REQUEST_HUMAN_REVIEW` |
| `EXTRACT_REQUIREMENT_SYNTAX` | `SYNTAX_EXTRACTION_POLICY`: `extractionConfidence >= autoAcceptThreshold` and `Action` contains `Subject`, action text, and `TargetObject`. | `RETRIEVE_CANDIDATE_CONCEPTS` |
| `EXTRACT_REQUIREMENT_SYNTAX` | `SYNTAX_EXTRACTION_POLICY`: `extractionConfidence < reviewThreshold` or a required `Action` element is missing. | `REQUEST_HUMAN_REVIEW` |
| `RETRIEVE_CANDIDATE_CONCEPTS` | `CONCEPT_RETRIEVAL_POLICY`: every selected element has ranked candidate concepts or explicit no-match evidence. | `CREATE_MATCHING_CANDIDATES` |
| `CREATE_MATCHING_CANDIDATES` | `MATCHING_POLICY`: every selected candidate has an automatic decision, for example `matchConfidence >= autoMapThreshold` creates a match or `matchConfidence < rejectThreshold` rejects the match. | `PERSIST_GRAPH_CHANGES` |
| `CREATE_MATCHING_CANDIDATES` | `MATCHING_POLICY`: any selected candidate requires review, for example `reviewThreshold <= matchConfidence < autoMapThreshold`, multiple candidates exceed the ambiguity threshold, or a no-match result requires concept creation approval. | `REQUEST_HUMAN_REVIEW` |
| `REQUEST_HUMAN_REVIEW` | `HUMAN_REVIEW_POLICY`: reviewer approves or corrects pending classification. | `EXTRACT_REQUIREMENT_SYNTAX` |
| `REQUEST_HUMAN_REVIEW` | `HUMAN_REVIEW_POLICY`: reviewer approves, edits, deletes, or adds extracted syntax elements. | `RETRIEVE_CANDIDATE_CONCEPTS` |
| `REQUEST_HUMAN_REVIEW` | `HUMAN_REVIEW_POLICY`: reviewer supplies an allowed decision for every pending concept matching or concept proposal. | `PERSIST_GRAPH_CHANGES` |
| `PERSIST_GRAPH_CHANGES` | `GRAPH_PERSISTENCE_POLICY`: KG transaction commits and returns persisted identifiers. | `DONE` |
| `ANY_STATE` | `STOP_POLICY`: required input is missing, dependency is unavailable, active policy is invalid, reviewer response is invalid, or a workflow invariant is violated. | `ABORTED` |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| `Provenance` | `INTAKE_REQUIREMENT` | All later states | KG node or transaction payload | Contains id, raw text, source identifier, ingestion timestamp. |
| `Classification` | `CLASSIFY_REQUIREMENT` | `PERSIST_GRAPH_CHANGES` | KG relationship or transaction payload | `conceptType` is one of the configured `RequirementType` values, `property` is one of the configured `RequirementProperty` values, `confidenceScore` is between `0.0` and `1.0`, and `rationale` is non-blank. |
| `Action` | `EXTRACT_REQUIREMENT_SYNTAX` | `RETRIEVE_CANDIDATE_CONCEPTS`; `PERSIST_GRAPH_CHANGES` | Domain model linked to requirement id | Contains `Subject`, action text, `TargetObject`, and optional `Condition` and `Constraint` values. |
| `CandidateConceptMatchSet` | `RETRIEVE_CANDIDATE_CONCEPTS` | `CREATE_MATCHING_CANDIDATES` | Structured collection linked to term mention ids | Every term has zero or more ranked matches and retrieval evidence. |
| `TermConceptMappingSet` | `CREATE_MATCHING_CANDIDATES` | `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to terms and candidate concepts | Every selected element has a mapping status and rationale. |
| `ConceptCreationPolicy` | Configuration | `CREATE_MATCHING_CANDIDATES` | Configured policy document or runtime configuration | Defines behavior for no-match, low-confidence, ambiguous, and concept-type-specific cases. |
| `MappingDecisionSet` | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW` | `PERSIST_GRAPH_CHANGES` | Structured collection linked to mappings | No item remains undecided before persistence, unless persisted as a pending review state by policy. |
| `ConceptProposalSet` | `CREATE_MATCHING_CANDIDATES` | `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to term mentions | Every proposal has label, proposed type, provenance, confidence, and no-match evidence. |
| KG transaction result | `PERSIST_GRAPH_CHANGES` | `DONE` | Neo4j transaction result or equivalent KG write receipt | Contains persisted identifiers for requirement and all written graph entities. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| Resolve ambiguous mapping | A term has multiple plausible concept matches above threshold. | Domain modeler, requirement analyst, software developer, or test engineer as configured by policy | `APPROVE_MATCH`, `REJECT_MATCH`, `SELECT_DIFFERENT_CONCEPT`, `PROPOSE_NEW_CONCEPT` | Term id, selected option, concept id if applicable, and short rationale. |
| Approve new concept proposal | No matching concept is found and policy requires approval. | Domain modeler by default, unless policy assigns another role. | `APPROVE_CREATE`, `REJECT_CREATE`, `MERGE_WITH_EXISTING`, `DEFER` | Proposal id, selected option, concept id for merge, and concept type if approved. |
| Override extraction | Extracted syntax is wrong or incomplete during review. | Requirement analyst or domain modeler | `ACCEPT_EXTRACTION`, `EDIT_TERM`, `DELETE_TERM`, `ADD_TERM` | Term id or new term text, syntax role, source span, and rationale. |

## Abort Conditions
- Abort if raw requirement text or source provenance is missing.
- Abort if the KG is unavailable during retrieval or persistence.
- Abort if a required active policy is missing or contradictory.
- Abort if a required human reviewer cannot provide a response in the required shape.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Which concept creation policy should be the default for v1? | The workflow cannot safely decide whether no-match terms become new concepts, proposals, pending review items, or blocked items. | `PROPOSE_ONLY` for all new concepts; `REQUIRE_APPROVAL` for domain concepts and `AUTO_CREATE` for aliases; `BLOCK` for unknown concept types. | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` |
| Which non-classification concept types are supported in the first KG schema? | Extraction and mapping need a bounded target vocabulary beyond the fixed classification concept types. | Classification starts with `REQUIREMENT`, `GOAL`, and `NEED`; retrieval may also need implementation-facing concepts such as `SystemComponent` and `UIComponent`. | `RETRIEVE_CANDIDATE_CONCEPTS` |
| Who owns review for each decision type? | Human review transitions need a concrete owner role. | Domain modeler owns concept creation; requirement analyst owns extraction corrections; software developer owns system component mappings; test engineer owns quality and constraint mappings. | `REQUEST_HUMAN_REVIEW` |
| Are pending mappings allowed to be persisted? | Persistence policy must decide whether incomplete decisions are durable review states or blockers. | Persist pending review state so the requirement is available for later completion. | `PERSIST_GRAPH_CHANGES` |

## Assumption Log

| Assumption | Source | Risk Level | Impact If Wrong | How To Validate |
|---|---|---|---|---|
| The primary users are requirement analysts, test engineers, software developers, and domain modelers. | User input. | LOW | Review roles and query needs may be incomplete. | Confirm role ownership in the concept creation policy. |
| The first success criterion is committed KG persistence of the requirement and its derived graph artifacts. | User input. | LOW | Completion could target the wrong outcome. | Confirm the KG transaction output required by `DONE`. |
| New concept creation behavior should be configurable by policy. | User input. | MEDIUM | A hard-coded behavior could violate governance expectations. | Create and review `ConceptCreationPolicy` before persistence behavior is implemented. |
| The KG already contains or will contain classification concepts such as requirement, goal, and need, plus retrieval concepts such as system component and UI component. | User input. | MEDIUM | Retrieval and classification may fail if these concept types are absent or differently named. | Validate supported concept types against the KG schema. |
| Requirement syntax can be represented as subject, object, action, condition, and constraint. | User-provided workflow outline. | MEDIUM | Important requirement semantics may be missed, such as actor, trigger, exception, priority, or acceptance criteria. | Test extraction against representative real requirements. |
| No-match retrieval is evidence for proposal, not proof that a concept does not exist. | Inference from KG matching risk. | MEDIUM | Duplicate concepts may be created if no-match is treated as definitive. | Require reviewer approval or stronger retrieval before concept creation. |
