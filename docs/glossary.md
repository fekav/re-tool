## Roles

| Term | Aliases | Used In | Description |
|---|---|---|---|
| Requirement Analyst | Analyst | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for validating requirement interpretation and extraction correctness. |
| Test Engineer | Tester | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing quality, condition, and constraint node decisions when policy assigns those decisions. |
| Software Developer | Developer | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing decisions involving implementation-facing concepts such as system components. |
| Domain Modeler | Ontology curator; domain expert | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for concept quality, concept creation approval, and ontology consistency. |

## Artifacts

| Term | Aliases | Used In | Description |
|---|---|---|---|
| RawText | Requirement text; raw requirement | `INTAKE_REQUIREMENT` | Durable representation of raw requirement text. |
| Classification | Classification result | `CLASSIFY_REQUIREMENT` | Assignment of raw text to a supported KG concept type and property, with confidence score and rationale. |
| ClassificationService | Requirement classifier | `CLASSIFY_REQUIREMENT` | Application capability that classifies RawText. |
| ClassifyRequirementCommand | Classify requirement instruction | `CLASSIFY_REQUIREMENT` | Command requesting classification of RawText. |
| RequirementClassifiedEvent | Classification event | `CLASSIFY_REQUIREMENT` | Domain event raised after a Requirement has been classified. |
| Action | Candidate action; extracted action | `EXTRACT_REQUIREMENT_SYNTAX` | Semantic extraction result owned by a Requirement: subject, action text, target object, and optional condition and constraint qualifiers. |
| CandidateNodeMatchSet | Candidate nodes; retrieval results | `RETRIEVE_CANDIDATE_NODES` | Retrieval result containing one `CandidateNodeMatch` per selected term. Each match has zero or more retrieved candidates; an empty candidate list is the current lookup-miss representation. |
| CandidateNode | Retrieved graph node identity | `RETRIEVE_CANDIDATE_NODES` | KG node identity returned by retrieval, containing candidate key, label, and `nodeType` (`CONCEPT`, `PREDICATE`, or `QUALIFIER`). |
| CandidateNodeMatch | Term retrieval result | `RETRIEVE_CANDIDATE_NODES` | Pairing of one `SelectedTerm` with zero or more `RetrievedCandidateNode` values. |
| RetrievedCandidateNode | Retrieved candidate | `RETRIEVE_CANDIDATE_NODES` | Candidate node after retrieval policy processing, carrying non-empty candidate-specific retrieval evidence. |
| RetrievalEvidence | Candidate evidence | `RETRIEVE_CANDIDATE_NODES` | Evidence produced for a retrieved candidate. Current node-name retrieval records `policyName`, evidence text, and score `1.0`. |
| NodeMatchDecisionSet | Matching decisions | `DECIDE_NODE_MATCHES`; `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Decision result containing one `NodeMatchDecision` per selected term. |
| NodeMatchDecision | Selected-term node decision | `DECIDE_NODE_MATCHES`; `REQUEST_HUMAN_REVIEW`; `PERSIST_GRAPH_CHANGES` | Decision payload with selected term, enum `status`, non-null `candidates`, and non-blank `rationale`. |
| NodeMatchDecisionStatus | Matching decision status | `DECIDE_NODE_MATCHES` | Enum outcome for one selected term: `AUTO_MAP_EXISTING`, `PROPOSE_EXISTING`, `REVIEW_REQUIRED`, or `AUTO_CREATE_NEW`. |
| NewNodeProposal | New node proposal | `DECIDE_NODE_MATCHES`; `PERSIST_GRAPH_CHANGES` | Proposed new KG node with label and selected term. In v1, lookup misses create one proposal from `selectedTerm.text` and the selected term type. |
| Raw Requirement Text | Source text; RawText | `INTAKE_REQUIREMENT` | Original textual software requirement preserved for provenance and auditability. |
| Provenance | Source metadata; traceability | `INTAKE_REQUIREMENT`; `PERSIST_GRAPH_CHANGES` | Metadata linking KG facts back to the raw requirement source, submitter, and ingestion event. |
| SyntaxExtractionOutput | Requirement syntax DTO | `EXTRACT_REQUIREMENT_SYNTAX`; `docs/json-contracts.md` | Boundary DTO representing structured model output before it is validated and mapped to the Action domain model. |
| StructuredOutputContract | DTO validation contract | `docs/json-contracts.md` | App-owned description of required DTO fields used by the handwritten structured-output validator. |
| StructuredOutputValidator | Handwritten DTO validator | `docs/json-contracts.md` | Small reusable validator that checks DTOs against StructuredOutputContract instances before mapping to domain types. |
| StructuredOutputValidationException | Structured output validation failure | `docs/json-contracts.md` | Failure raised when model output DTOs do not satisfy their StructuredOutputContract. |
| InvalidStructuredOutputException | Invalid model output | `docs/json-contracts.md` | Failure raised when model output cannot be parsed, validated, or mapped into an application-owned DTO. |
| SelectedTerm | Selected requirement element term | `RETRIEVE_CANDIDATE_NODES` | Requirement element type and text value selected for node retrieval. Command callers provide no ids. |
| RetrievalScore | Candidate retrieval score | `RETRIEVE_CANDIDATE_NODES`; `NODE_RETRIEVAL_POLICY` | Numeric score on retrieval evidence. Current node-name retrieval assigns score `1.0` to exact stripped graph-node name matches. |

## Domain Terms

| Term | Aliases | Used In | Description |
|---|---|---|---|
| Requirement | Software requirement | `CLASSIFY_REQUIREMENT`; Required Artifacts | KG concept representing a binding system or product obligation. |
| SyntaxExtraction | Syntax extraction capability | `EXTRACT_REQUIREMENT_SYNTAX` | Application capability that extracts the Action domain model from RawText. |
| ExtractSyntaxResponse | Syntax extraction response | `EXTRACT_REQUIREMENT_SYNTAX` | Application response DTO that preserves the public `requirementElements` shape while the domain uses Action. |
| ElementId | Domain element id | `EXTRACT_REQUIREMENT_SYNTAX`; `CLASSIFY_REQUIREMENT` | UUID-backed identifier used by domain entities such as Requirement, Action, Subject, and TargetObject. |
| InvalidRawRequirementTextException | Invalid source text | `INTAKE_REQUIREMENT` | Domain rule violation raised when RawText is absent or blank. |
| Goal | Objective | `CLASSIFY_REQUIREMENT`; Open Questions | KG concept representing a desired outcome or stakeholder objective. |
| Need | Stakeholder need; capability gap | `CLASSIFY_REQUIREMENT`; Open Questions | KG concept representing what a stakeholder needs before it is expressed as a binding system obligation. |
| RequirementType | Classification type | `CLASSIFY_REQUIREMENT` | Closed set of supported KG concept types for classification: `GOAL`, `NEED`, and `REQUIREMENT`. |
| RequirementProperty | Classification property | `CLASSIFY_REQUIREMENT` | Cross-cutting property assigned to eligible `GOAL`, `NEED`, and `REQUIREMENT` classifications: `FUNCTIONAL` or `QUALITY`. |
| ConfidenceScore | Classification confidence | `CLASSIFY_REQUIREMENT` | Numeric score from `0.0` to `1.0` expressing classifier certainty in the complete classification result. |
| Rationale | Classification rationale | `CLASSIFY_REQUIREMENT` | Short explanation of why the raw text received the selected concept type and property. |
| InvalidConfidenceScoreException | Invalid classification confidence | `CLASSIFY_REQUIREMENT` | Domain rule violation raised when a confidence score is not between `0.0` and `1.0` inclusive. |
| InvalidClassificationRationaleException | Invalid classification reason | `CLASSIFY_REQUIREMENT` | Domain rule violation raised when a classification rationale is absent or blank. |
| Functional | Behavioral property | `CLASSIFY_REQUIREMENT` | Property for behavior, capability, workflow, or operation. |
| Quality | Quality property | `CLASSIFY_REQUIREMENT` | Property for quality attributes and constraints such as performance, security, availability, usability, reliability, or compliance. |
| SystemComponent | Backend component; service; module | `RETRIEVE_CANDIDATE_NODES`; Human Decision Points | KG concept representing an implementation-facing system part. |
| UIComponent | User interface component; screen element | `RETRIEVE_CANDIDATE_NODES`; Human Decision Points | KG concept representing a visible or interactive user interface part. |
| NodeRetrievalService | Retrieval domain service | `RETRIEVE_CANDIDATE_NODES` | Domain service that applies the active `NodeRetrievalPolicy` to selected terms and returns a `CandidateNodeMatchSet`. It rejects empty selected-term requests and policies that return a match for a different selected term. |
| NodeRetrievalPolicy | Retrieval policy port | `RETRIEVE_CANDIDATE_NODES`; `NODE_RETRIEVAL_POLICY` | Domain policy interface that retrieves candidates for one `SelectedTerm` and returns a `CandidateNodeMatch`. |
| CandidateLookup | Node-name lookup port | `RETRIEVE_CANDIDATE_NODES` | Domain port used by `NodeNameRetrievalPolicy` to find KG candidate nodes for one selected term. It does not assign matching outcomes or concept creation decisions. |
| Neo4jNodeNameLookup | Neo4j node-name lookup | `RETRIEVE_CANDIDATE_NODES` | Neo4j adapter for `CandidateLookup`. It maps `SUBJECT`/`OBJECT` to `Concept`, `ACTION` to `Predicate`, and `CONDITION`/`CONSTRAINT` to `Qualifier`, using exact stripped name text. |
| nodeName | Node-name evidence policy name | `RETRIEVE_CANDIDATE_NODES`; `NODE_RETRIEVAL_POLICY` | Retrieval evidence policy name emitted for node-name matches. |
| node-matching | Node matching slice | `DECIDE_NODE_MATCHES` | Slice that consumes `CandidateNodeMatchSet` data and emits a `NodeMatchDecisionSet` without writing graph changes. |
| NodeMatchingService | Matching domain service | `DECIDE_NODE_MATCHES` | Domain service that applies the active `NodeMatchingPolicy` to each retrieved candidate match and returns one decision per selected term. |
| NodeMatchingPolicy | Matching policy port | `DECIDE_NODE_MATCHES`; `MATCHING_POLICY` | Domain policy interface that converts one `CandidateNodeMatch` into one `NodeMatchDecision`. |
| ThresholdNodeMatchingPolicy | V1 matching policy | `DECIDE_NODE_MATCHES`; `MATCHING_POLICY` | Current policy with hardcoded `auto_map_threshold = 1.0`; it auto-maps a unique top candidate at threshold, requires review for tied top candidates at threshold, proposes the best below-threshold existing candidate, and auto-creates a new node for lookup misses. |
| Subject | Actor; grammatical subject | `EXTRACT_REQUIREMENT_SYNTAX` | Domain entity identifying who or what performs or owns an action. |
| TargetObject | Object; target | `EXTRACT_REQUIREMENT_SYNTAX` | Domain entity identifying what an action affects. |
| Action | Behavior; verb | `EXTRACT_REQUIREMENT_SYNTAX` | Domain entity identifying required behavior or operation and owning its subject, target object, conditions, and constraints. |
| Condition | Precondition; trigger; circumstance | `EXTRACT_REQUIREMENT_SYNTAX` | Domain value object identifying when or under what situation the requirement applies. |
| Constraint | Rule; limit; restriction | `EXTRACT_REQUIREMENT_SYNTAX` | Domain value object identifying a required limitation, quality, or boundary. |

## States

| Term | Aliases | Used In | Description |
|---|---|---|---|
| INTAKE_REQUIREMENT | Intake | State Table | Start state that captures raw requirement text and provenance. |
| CLASSIFY_REQUIREMENT | Classify | State Table | State that assigns an initial KG type and property to the requirement. |
| EXTRACT_REQUIREMENT_SYNTAX | Extract syntax | State Table | State that extracts candidate terms from requirement syntax. |
| RETRIEVE_CANDIDATE_NODES | Retrieve matches | State Table | State that searches the KG for node-name candidate nodes for selected requirement element terms. |
| DECIDE_NODE_MATCHES | Decide node matches | State Table | State that converts retrieval results into a `NodeMatchDecisionSet` through the `node-matching` slice. |
| REQUEST_HUMAN_REVIEW | Human review | State Table | State that collects required review decisions for ambiguous or governance-sensitive node decisions. |
| PERSIST_GRAPH_CHANGES | Persist graph | State Table | State that writes requirement, provenance, mentions, assertions, qualifiers, and approved canonical nodes to the KG. |
| DONE | Complete | State Table | Terminal state for successful KG persistence. |
| ABORTED | Failed; stopped | State Table | Terminal state for unsafe or impossible continuation. |

## Decisions

| Term | Aliases | Used In | Description |
|---|---|---|---|
| AUTO_CREATE | Automatic creation | Human Decision Points | Policy option allowing new nodes to be created without human approval under configured conditions. |
| PROPOSE_ONLY | Proposal only | Human Decision Points | Policy option that keeps a node decision non-final until review or persistence policy resolves it. |
| REQUIRE_APPROVAL | Manual approval | Human Decision Points | Policy option requiring a human decision before node creation or certain node decisions. |
| BLOCK | Stop | Human Decision Points | Policy option that prevents persistence or node creation for unresolved cases. |

## Policies

| Term | Aliases | Used In | Description |
|---|---|---|---|
| RAW_TEXT_POLICY | Raw text validation | State Table policy column | Requires non-blank RawText and recorded provenance before derived artifacts are created. |
| CLASSIFICATION_POLICY | Classification output policy | State Table policy column | Treats RequirementType and RequirementProperty as classifier outputs from supported enum sets and treats low confidence as a review signal. |
| SYNTAX_EXTRACTION_POLICY | Syntax extraction policy | State Table policy column | Requires subject, action text, and target object, while allowing condition and constraint only when expressed by the raw text. |
| NODE_RETRIEVAL_POLICY | Retrieval policy | State Table policy column | Domain policy that defines how selected terms become candidate node matches. Current implementation is `NodeNameRetrievalPolicy`. |
| NodeNameRetrievalPolicy | Node-name retrieval policy | `NODE_RETRIEVAL_POLICY`; `RETRIEVE_CANDIDATE_NODES` | Current retrieval policy that calls `CandidateLookup` once per selected term, de-duplicates duplicate candidate keys keeping the first occurrence, and records `nodeName` evidence with score `1.0`. |
| MATCHING_POLICY | Matching decision policy | State Table policy column | Defines how retrieved candidates become node match decisions. V1 is `ThresholdNodeMatchingPolicy` with hardcoded `auto_map_threshold = 1.0`. |
| HUMAN_REVIEW_POLICY | Review routing policy | State Table policy column | Defines reviewer ownership, allowed response shape, and due condition for policy-directed review items. |
| GRAPH_PERSISTENCE_POLICY | Graph write policy | State Table policy column | Requires idempotent graph writes, policy-approved node creation, and explicit permission to persist pending review states. |
| COMPLETION_POLICY | Workflow completion policy | State Table policy column | Requires a committed KG transaction with persisted identifiers before the workflow can reach DONE. |
| STOP_POLICY | Workflow stop policy | State Table policy column | Stops the workflow for missing required input, unavailable dependency, invalid policy or reviewer response, or violated invariant. |
