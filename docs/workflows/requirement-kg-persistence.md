# Requirement KG Persistence Workflow Contract

## Purpose
Persist textual requirements into a knowledge graph while preserving provenance, extracting requirement syntax, mapping candidate terms to KG concepts, and applying configured policies that control behaviour.

## Scope

### In Scope
- Persisting raw software requirements, business goals, stakeholder needs, text, and source provenance.
- Extracting candidate requirement terms from subject, object, action, condition, and constraint syntax.
- Retrieving matching candidate concepts from the KG.
- Creating term-to-concept candidate mappings.
- Applying a concept creation policy when no suitable match exists.
- Making the requirement queryable from the KG by raw text, extracted terms, approved mappings, and pending review state.

### Non-goals
- Defining the full enterprise ontology.
- Generating test cases from requirements.
- Implementing natural-language parsing algorithms.
- Deciding business approval authority outside the configured concept creation policy.
- Updating source requirement documents after KG persistence.

## State Table
| State | Goal | Required Inputs | Allowed Actions | Outputs | Abort If |
|---|---|---|---|---|---|
| `INTAKE_REQUIREMENT` (START) | Capture the raw text and its provenance. | Raw requirement text; source identifier; submitter or ingestion actor. | Validate non-empty text; assign requirement id; record source metadata; normalize line endings. | `Provenance` with `RawText`, `sourceIdentifier`, `ingestedAt`. | Raw text or source identifier is missing. |
| `CLASSIFY_REQUIREMENT` | Identify the requirement's initial KG type, property, confidence, and rationale. | `Provenance`; allowed `RequirementType` and `RequirementProperty` values. | Classify as `GOAL`, `NEED`, or `REQUIREMENT`; classify property as `FUNCTIONAL` or `QUALITY` when eligible; assign confidence score; attach rationale and context tags if available. | `Classification` linked to `Provenance`. | No allowed `RequirementType` value exists in KG configuration. |
| `EXTRACT_REQUIREMENT_SYNTAX` | Extract syntax elements as candidate terms. | `Provenance`; syntax extraction rules or parser. | Extract subject, object, action, condition, and constraint mentions; preserve text spans; assign extraction confidence. | `RequirementSyntax` linked to `Provenance`. | Parser or extraction rules are unavailable; no extractable term mention is found and empty extraction is disallowed by policy. |
| `RETRIEVE_CANDIDATE_CONCEPTS` | Find KG concepts that may match each candidate term. | `RequirementSyntax`; KG concept index; matching strategy. | Query KG by label, alias, type hint, lexical similarity, semantic similarity, or configured matcher; rank matches. | `CandidateConceptMatchSet` with scores and matched evidence. | KG connection fails; concept index is unavailable. |
| `CREATE_MAPPING_CANDIDATES` | Convert retrieval results into explicit mapping candidates. | `RequirementSyntax`; `CandidateConceptMatchSet`; mapping threshold configuration. | Create exact, probable, ambiguous, or no-match mapping candidates; attach confidence and rationale. | `TermConceptMappingSet` linked to mentions and candidate concepts. | Mapping thresholds are missing or contradictory. |
| `REQUEST_POLICY_DECISION` | Obtain the governing concept creation policy when it is not already configured. | `TermConceptMappingSet`; policy owner role. | Present policy options, affected concept types, confidence thresholds, and impact of each option; collect policy selection. | `ConceptCreationPolicy` with scope, thresholds, and reviewer roles. | Policy owner is unavailable; selected policy omits required reviewer roles or thresholds. |
| `EVALUATE_CONCEPT_POLICY` | Decide whether each term can be mapped, proposed, created, or blocked. | `TermConceptMappingSet`; `ConceptCreationPolicy`; supported concept types. | Apply policy per term and concept type; mark mapping decision as approved, pending review, propose new concept, auto-create concept, or rejected. | `MappingDecisionSet`; optional `ConceptProposalSet`. | Policy is contradictory; policy requires a human role that is not configured. |
| `REQUEST_HUMAN_REVIEW` | Obtain required review decisions for ambiguous mappings or new concept proposals. | `MappingDecisionSet` containing `PENDING_REVIEW`; reviewer role from policy. | Present term, source text span, candidate concepts, no-match evidence, proposed concept type, and confidence; collect reviewer decision. | Reviewed `MappingDecisionSet`; approved or rejected `ConceptProposalSet`. | Required reviewer is unavailable; reviewer response does not match allowed options. |
| `PERSIST_GRAPH_CHANGES` | Write durable requirement, provenance, terms, mappings, and approved concepts to the KG. | `Provenance`; `Classification`; `RequirementSyntax`; reviewed or policy-approved `MappingDecisionSet`. | Create or update graph nodes and relationships; create approved new concepts; link requirement to raw text provenance; link terms to concepts or review states. | KG transaction containing requirement, provenance, term mentions, mapping decisions, and created concepts. | KG transaction fails; required reviewed decisions are absent; idempotency key cannot be computed. |
| `VERIFY_QUERYABILITY` | Confirm the persisted requirement can be queried from the KG. | KG transaction result; required query contract. | Query by requirement id, raw text provenance, extracted term, mapped concept, and pending review state; compare query results with expected artifacts. | `PersistenceVerificationReport` with pass/fail checks. | Any required query returns no result or an inconsistent result. |
| `DONE` (TERMINAL) | End after successful KG persistence and verification. | Passing `PersistenceVerificationReport`. | Report persisted requirement id, concept mappings, proposals, created concepts, and verification result. | Completed workflow summary. | - |
| `ABORTED` (TERMINAL) | End when execution cannot safely continue. | Abort reason and last valid state. | Report abort reason, missing inputs, failed checks, and recovery action. | Aborted workflow summary. | - |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| `INTAKE_REQUIREMENT` | `Provenance` exists with raw text and source provenance. | `CLASSIFY_REQUIREMENT` | No | - |
| `CLASSIFY_REQUIREMENT` | Requirement type is one of the configured `RequirementType` values, property is one of the configured `RequirementProperty` values, confidence score is valid, and rationale is present. | `EXTRACT_REQUIREMENT_SYNTAX` | No | V1 `RequirementType` values are `GOAL`, `NEED`, and `REQUIREMENT`. `RequirementProperty` values are `FUNCTIONAL` and `QUALITY`. |
| `EXTRACT_REQUIREMENT_SYNTAX` | `RequirementSyntax` exists and every mention has a text span, syntax role, and confidence. | `RETRIEVE_CANDIDATE_CONCEPTS` | No | Empty extraction may continue only if allowed by policy. |
| `RETRIEVE_CANDIDATE_CONCEPTS` | Candidate retrieval completed for every term mention. | `CREATE_MAPPING_CANDIDATES` | No | A no-match result is valid evidence, not a failure. |
| `CREATE_MAPPING_CANDIDATES` | Every term mention has exact, probable, ambiguous, or no-match mapping status and `ConceptCreationPolicy` exists. | `EVALUATE_CONCEPT_POLICY` | No | - |
| `CREATE_MAPPING_CANDIDATES` | Every term mention has exact, probable, ambiguous, or no-match mapping status, `ConceptCreationPolicy` is missing, and a policy owner role is configured. | `REQUEST_POLICY_DECISION` | Yes: policy owner must select policy. | Use when policy cannot be loaded from configuration. |
| `REQUEST_POLICY_DECISION` | Policy owner provides policy name, concept-type scope, confidence thresholds, and reviewer roles. | `EVALUATE_CONCEPT_POLICY` | Yes: policy owner response required. | The selected policy becomes the `ConceptCreationPolicy` artifact. |
| `EVALUATE_CONCEPT_POLICY` | Policy resolves all decisions without human review. | `PERSIST_GRAPH_CHANGES` | No | Applies to approved mappings, rejected mappings, and allowed auto-create decisions. |
| `EVALUATE_CONCEPT_POLICY` | One or more decisions require review. | `REQUEST_HUMAN_REVIEW` | Yes: configured reviewer role must decide. | Review may be owned by requirement analyst, domain modeler, test engineer, or software developer based on policy. |
| `REQUEST_HUMAN_REVIEW` | Reviewer provides allowed decision for every pending item. | `PERSIST_GRAPH_CHANGES` | Yes: reviewer response required. | Allowed decisions are defined in Human Decision Points. |
| `PERSIST_GRAPH_CHANGES` | KG transaction commits and returns persisted identifiers. | `VERIFY_QUERYABILITY` | No | - |
| `VERIFY_QUERYABILITY` | All required query checks pass. | `DONE` | No | - |
| `ANY_STATE` | Required input is missing, KG is unavailable, no policy owner exists for a missing policy, or an invariant is violated. | `ABORTED` | No | Abort summary must include last valid state and recovery action. |
| `ANY_STATE` | Execution can continue only after an individual mapping or concept proposal review decision. | `REQUEST_HUMAN_REVIEW` | Yes: required role from policy. | Use only when the required decision owner is known. |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| `Provenance` | `INTAKE_REQUIREMENT` | All later states | KG node or transaction payload | Contains id, raw text, source identifier, ingestion timestamp. |
| `Classification` | `CLASSIFY_REQUIREMENT` | `PERSIST_GRAPH_CHANGES` | KG relationship or transaction payload | `conceptType` is one of the configured `RequirementType` values, `property` is one of the configured `RequirementProperty` values, `confidenceScore` is between `0.0` and `1.0`, and `rationale` is non-blank. |
| `RequirementSyntax` | `EXTRACT_REQUIREMENT_SYNTAX` | `RETRIEVE_CANDIDATE_CONCEPTS`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to requirement id | Every term has text, syntax role, source span, and confidence. |
| `CandidateConceptMatchSet` | `RETRIEVE_CANDIDATE_CONCEPTS` | `CREATE_MAPPING_CANDIDATES` | Structured collection linked to term mention ids | Every term has zero or more ranked matches and retrieval evidence. |
| `TermConceptMappingSet` | `CREATE_MAPPING_CANDIDATES` | `EVALUATE_CONCEPT_POLICY` | Structured collection linked to terms and candidate concepts | Every term has a mapping status and rationale. |
| `ConceptCreationPolicy` | `REQUEST_POLICY_DECISION` or configuration | `EVALUATE_CONCEPT_POLICY` | Configured policy document or runtime configuration | Defines behavior for no-match, low-confidence, ambiguous, and concept-type-specific cases. |
| `MappingDecisionSet` | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW` | `PERSIST_GRAPH_CHANGES` | Structured collection linked to mappings | No item remains undecided before persistence, unless persisted as a pending review state by policy. |
| `ConceptProposalSet` | `EVALUATE_CONCEPT_POLICY` | `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Structured collection linked to term mentions | Every proposal has label, proposed type, provenance, confidence, and no-match evidence. |
| KG transaction result | `PERSIST_GRAPH_CHANGES` | `VERIFY_QUERYABILITY` | Neo4j transaction result or equivalent KG write receipt | Contains persisted identifiers for requirement and all written graph entities. |
| `PersistenceVerificationReport` | `VERIFY_QUERYABILITY` | `DONE` | Markdown, JSON, or workflow log | Required queries pass for id, provenance, extracted terms, mappings, and review states. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| Select concept creation policy | `EVALUATE_CONCEPT_POLICY` starts without a configured policy. | Product owner or domain modeler | `AUTO_CREATE`, `PROPOSE_ONLY`, `REQUIRE_APPROVAL`, `BLOCK` | Policy name plus scope by concept type and confidence threshold. |
| Resolve ambiguous mapping | A term has multiple plausible concept matches above threshold. | Domain modeler, requirement analyst, software developer, or test engineer as configured by policy | `APPROVE_MATCH`, `REJECT_MATCH`, `SELECT_DIFFERENT_CONCEPT`, `PROPOSE_NEW_CONCEPT` | Term id, selected option, concept id if applicable, and short rationale. |
| Approve new concept proposal | No matching concept is found and policy requires approval. | Domain modeler by default, unless policy assigns another role. | `APPROVE_CREATE`, `REJECT_CREATE`, `MERGE_WITH_EXISTING`, `DEFER` | Proposal id, selected option, concept id for merge, and concept type if approved. |
| Override extraction | Extracted syntax is wrong or incomplete during review. | Requirement analyst or domain modeler | `ACCEPT_EXTRACTION`, `EDIT_TERM`, `DELETE_TERM`, `ADD_TERM` | Term id or new term text, syntax role, source span, and rationale. |

## Abort Conditions
- Abort if raw requirement text or source provenance is missing.
- Abort if the KG is unavailable during retrieval, persistence, or verification.
- Abort if no concept creation policy is configured and no policy owner can select one.
- Abort if a required human reviewer cannot provide a response in the required shape.
- Abort if verification queries cannot retrieve the persisted requirement by id and provenance.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Which concept creation policy should be the default for v1? | The workflow cannot safely decide whether no-match terms become new concepts, proposals, pending review items, or blocked items. | `PROPOSE_ONLY` for all new concepts; `REQUIRE_APPROVAL` for domain concepts and `AUTO_CREATE` for aliases; `BLOCK` for unknown concept types. | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` |
| Which non-classification concept types are supported in the first KG schema? | Extraction and mapping need a bounded target vocabulary beyond the fixed classification concept types. | Classification starts with `REQUIREMENT`, `GOAL`, and `NEED`; retrieval may also need implementation-facing concepts such as `SystemComponent` and `UIComponent`. | `RETRIEVE_CANDIDATE_CONCEPTS` |
| What query contract defines "a requirement is queryable"? | Verification needs concrete queries, not a vague success statement. | Query by id, raw text provenance, term mention, mapped concept, and review state. | `VERIFY_QUERYABILITY` |
| Who owns review for each decision type? | Human review transitions need a concrete owner role. | Domain modeler owns concept creation; requirement analyst owns extraction corrections; software developer owns system component mappings; test engineer owns quality and constraint mappings. | `REQUEST_HUMAN_REVIEW` |
| Are pending mappings allowed to be persisted? | Queryability may require storing incomplete decisions instead of blocking persistence. | Persist pending review state so the requirement is queryable immediately. | `PERSIST_GRAPH_CHANGES`; `VERIFY_QUERYABILITY` |

## Assumption Log

| Assumption | Source | Risk Level | Impact If Wrong | How To Validate |
|---|---|---|---|---|
| The primary users are requirement analysts, test engineers, software developers, and domain modelers. | User input. | LOW | Review roles and query needs may be incomplete. | Confirm role ownership in the concept creation policy. |
| The first success criterion is that a software requirement is queryable from a KG. | User input. | LOW | Verification could target the wrong outcome. | Define the required query contract before implementation. |
| New concept creation behavior should be configurable by policy. | User input. | MEDIUM | A hard-coded behavior could violate governance expectations. | Create and review `ConceptCreationPolicy` before persistence behavior is implemented. |
| The KG already contains or will contain classification concepts such as requirement, goal, and need, plus retrieval concepts such as system component and UI component. | User input. | MEDIUM | Retrieval and classification may fail if these concept types are absent or differently named. | Validate supported concept types against the KG schema. |
| Requirement syntax can be represented as subject, object, action, condition, and constraint. | User-provided workflow outline. | MEDIUM | Important requirement semantics may be missed, such as actor, trigger, exception, priority, or acceptance criteria. | Test extraction against representative real requirements. |
| No-match retrieval is evidence for proposal, not proof that a concept does not exist. | Inference from KG matching risk. | MEDIUM | Duplicate concepts may be created if no-match is treated as definitive. | Require reviewer approval or stronger retrieval before concept creation. |

## Classification Semantics

`CLASSIFY_REQUIREMENT` classifies raw text on two axes: concept type and property. The concept type records intent and commitment level and is represented by `RequirementType` in source code. The property records whether the classified concept is primarily behavioral or quality-related and is represented by `RequirementProperty`. The `Classification` result also records one overall `ConfidenceScore` and a short `Rationale` for review and persistence.

Low confidence is a consumer review and triage signal, not a classifier failure. Actually the workflow still returns the best-fit `GOAL`, `NEED`, or `REQUIREMENT` classification because it does not define an `UNKNOWN` concept type.

| Concept Type | Intent / Commitment Level | Classifier Signal | Example |
|---|---|---|---|
| `GOAL` | Desired outcome or business objective. Explains why change matters. | Outcome language, benefit, target state, strategic result. Usually not directly testable as one system behavior. | "Reduce failed customer onboarding by 30%." |
| `NEED` | Stakeholder need or capability gap. Explains what someone needs before it is expressed as a binding system obligation. | Stakeholder-centered language such as "needs", "wants", "must be able to", or problem statements. Often generates multiple requirements. | "Support agents need visibility into failed payment attempts." |
| `REQUIREMENT` | Binding product or system obligation. Specifies what the system must do or satisfy. | "shall", "must", "is required to", concrete behavior, or measurable constraint. Should be verifiable. | "The billing service must log failed payment attempts with reason codes." |

| Property | Meaning | Classifier Signal | Example |
|---|---|---|---|
| `FUNCTIONAL` | Behavior, capability, workflow, operation, or interaction. | Action or capability language describing something the system, user, or stakeholder can do. | "The dashboard shall export monthly usage metrics." |
| `QUALITY` | Quality attribute or constraint. | Performance, security, availability, usability, reliability, compliance, scalability, or measurable constraint language. | "The dashboard export must complete within 2 seconds." |

| Evidence Field | Meaning | Validation |
|---|---|---|
| `confidenceScore` | Overall classifier certainty in the concept type and property assignment. | Required number from `0.0` to `1.0`. |
| `rationale` | Short explanation grounded in the raw text. | Required non-blank text. |
