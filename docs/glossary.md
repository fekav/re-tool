## Workflows

| Term | Aliases | Used In | Description |
|---|---|---|---|
| Requirement KG Persistence | Requirement-to-KG ingestion | `docs/workflows/requirement-kg-persistence.md` | Workflow for persisting textual software requirements into a knowledge graph with provenance, extracted terms, mappings, and policy-governed concept creation. |

## Roles

| Term | Aliases | Used In | Description |
|---|---|---|---|
| Requirement Analyst | Analyst | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for validating requirement interpretation and extraction correctness. |
| Test Engineer | Tester | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing quality, condition, and constraint mappings when policy assigns those decisions. |
| Software Developer | Developer | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing mappings to implementation-facing concepts such as system components. |
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
| CandidateConceptMatchSet | Candidate concepts; retrieval results | `RETRIEVE_CANDIDATE_CONCEPTS` | Ordered KG concept candidates or no-match evidence for selected term mentions. Every real retrieved candidate has candidate-specific evidence with a required retrieval score. |
| CandidateConcept | Retrieved concept identity | `RETRIEVE_CANDIDATE_CONCEPTS` | KG concept identity returned by retrieval, containing concept id, label, and optional concept type. |
| CandidateLookupHit | Raw lookup hit | `RETRIEVE_CANDIDATE_CONCEPTS` | Candidate found by one lookup method before policy aggregation, with lookup method name, evidence text, and lookup rank. |
| RetrievedCandidateConcept | Weighted retrieved candidate | `RETRIEVE_CANDIDATE_CONCEPTS` | Candidate concept after policy aggregation, carrying candidate-specific retrieval evidence and required retrieval score. |
| RetrievalEvidence | Candidate evidence; no-match evidence | `RETRIEVE_CANDIDATE_CONCEPTS` | Evidence produced by retrieval. Candidate-specific evidence records applied lookup methods and required score; term-level evidence records retrieval attempts and no-match results. |
| TermConceptMappingSet | Mapping candidates | `CREATE_MATCHING_CANDIDATES` | Candidate mappings between extracted term mentions and KG concepts, with status and rationale. |
| ConceptCreationPolicy | Creation policy; matching policy | `CREATE_MATCHING_CANDIDATES` | Configured rules that decide whether no-match or ambiguous terms are auto-created, proposed, reviewed, blocked, or rejected. |
| MappingDecisionSet | Mapping decisions | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW` | Final or pending decisions for term-to-concept mappings and concept proposals. |
| ConceptProposalSet | New concept proposals | `CREATE_MATCHING_CANDIDATES`; `REQUEST_HUMAN_REVIEW` | Proposed new KG concepts generated when extracted terms do not sufficiently match existing concepts. |
| Raw Requirement Text | Source text; RawText | `INTAKE_REQUIREMENT` | Original textual software requirement preserved for provenance and auditability. |
| Provenance | Source metadata; traceability | `INTAKE_REQUIREMENT`; `PERSIST_GRAPH_CHANGES` | Metadata linking KG facts back to the raw requirement source, submitter, and ingestion event. |
| SyntaxExtractionOutput | Requirement syntax DTO | `EXTRACT_REQUIREMENT_SYNTAX`; `docs/json-contracts.md` | Boundary DTO representing structured model output before it is validated and mapped to the Action domain model. |
| StructuredOutputContract | DTO validation contract | `docs/json-contracts.md` | App-owned description of required DTO fields used by the handwritten structured-output validator. |
| StructuredOutputValidator | Handwritten DTO validator | `docs/json-contracts.md` | Small reusable validator that checks DTOs against StructuredOutputContract instances before mapping to domain types. |
| StructuredOutputValidationException | Structured output validation failure | `docs/json-contracts.md` | Failure raised when model output DTOs do not satisfy their StructuredOutputContract. |
| InvalidStructuredOutputException | Invalid model output | `docs/json-contracts.md` | Failure raised when model output cannot be parsed, validated, or mapped into an application-owned DTO. |
| SelectedTerm | Selected syntax term | `RETRIEVE_CANDIDATE_CONCEPTS` | Syntax role and text value selected for concept retrieval. Command callers provide no ids. |
| RetrievalScore | Candidate retrieval score | `RETRIEVE_CANDIDATE_CONCEPTS`; `CONCEPT_RETRIEVAL_POLICY` | Required numeric score for a retrieved candidate, equal to the sum of applied lookup method weights. Score `1.0` is the maximum and is reserved for exact label hits in v1. |

## Domain Terms

| Term | Aliases | Used In | Description |
|---|---|---|---|
| Requirement | Software requirement | `CLASSIFY_REQUIREMENT`; Required Artifacts | KG concept representing a binding system or product obligation. |
| SyntaxExtraction | Syntax extraction capability | `EXTRACT_REQUIREMENT_SYNTAX` | Application capability that extracts the Action domain model from RawText. |
| ExtractSyntaxResponse | Syntax extraction response | `EXTRACT_REQUIREMENT_SYNTAX` | Application response DTO that preserves the public `syntaxElements` shape while the domain uses Action. |
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
| SystemComponent | Backend component; service; module | `RETRIEVE_CANDIDATE_CONCEPTS`; Human Decision Points | KG concept representing an implementation-facing system part. |
| UIComponent | User interface component; screen element | `RETRIEVE_CANDIDATE_CONCEPTS`; Human Decision Points | KG concept representing a visible or interactive user interface part. |
| ConceptRetrievalService | Retrieval domain service | `RETRIEVE_CANDIDATE_CONCEPTS` | Domain service that applies the active ConceptRetrievalPolicy to selected terms and returns a CandidateConceptMatchSet. |
| ConceptCandidateLookup | Lookup method port | `RETRIEVE_CANDIDATE_CONCEPTS` | Application port for one candidate lookup method. It exposes a lookup method name, lookup weight, and deterministic lookup hits. |
| CandidateLookupScope | Lookup scope | `RETRIEVE_CANDIDATE_CONCEPTS` | Context passed to lookup methods, such as syntax role and allowed concept-type hints, without owning retrieval order or mapping decisions. |
| exactLabel | Exact label lookup | `RETRIEVE_CANDIDATE_CONCEPTS`; `CONCEPT_RETRIEVAL_POLICY` | Lookup method that searches graph concepts whose label or text exactly equals the selected term text. V1 weight is `1.0`. |
| alias | Alias lookup | `RETRIEVE_CANDIDATE_CONCEPTS`; `CONCEPT_RETRIEVAL_POLICY` | Lookup method that searches graph alias values for the selected term text after exact label lookup misses. V1 weight is `0.7`. |
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
| RETRIEVE_CANDIDATE_CONCEPTS | Retrieve matches | State Table | State that searches the KG for ordered weighted concept candidates or explicit no-match evidence for extracted terms. |
| CREATE_MATCHING_CANDIDATES | Create matchings | State Table | State that converts retrieval results into policy-governed term-to-concept matchings or review items. |
| REQUEST_HUMAN_REVIEW | Human review | State Table | State that collects required review decisions for ambiguous mappings and concept proposals. |
| PERSIST_GRAPH_CHANGES | Persist graph | State Table | State that writes requirement, provenance, terms, mappings, and approved concepts to the KG. |
| DONE | Complete | State Table | Terminal state for successful KG persistence. |
| ABORTED | Failed; stopped | State Table | Terminal state for unsafe or impossible continuation. |

## Decisions

| Term | Aliases | Used In | Description |
|---|---|---|---|
| AUTO_CREATE | Automatic creation | Human Decision Points | Policy option allowing new concepts to be created without human approval under configured conditions. |
| PROPOSE_ONLY | Proposal only | Human Decision Points | Policy option that creates concept proposals without immediately creating KG concepts. |
| REQUIRE_APPROVAL | Manual approval | Human Decision Points | Policy option requiring a human decision before concept creation or certain mappings. |
| BLOCK | Stop | Human Decision Points | Policy option that prevents persistence or concept creation for unresolved cases. |

## Policies

| Term | Aliases | Used In | Description |
|---|---|---|---|
| RAW_TEXT_POLICY | Raw text validation | State Table policy column | Requires non-blank RawText and recorded provenance before derived artifacts are created. |
| CLASSIFICATION_POLICY | Classification output policy | State Table policy column | Treats RequirementType and RequirementProperty as classifier outputs from supported enum sets and treats low confidence as a review signal. |
| SYNTAX_EXTRACTION_POLICY | Syntax extraction policy | State Table policy column | Requires subject, action text, and target object, while allowing condition and constraint only when expressed by the raw text. |
| CONCEPT_RETRIEVAL_POLICY | Retrieval ordering policy | State Table policy column | Domain policy that defines lookup method order, lookup weights, score aggregation, stop conditions, and no-match evidence for candidate retrieval. |
| OrderedWeightedConceptRetrievalPolicy | Ordered weighted retrieval | `CONCEPT_RETRIEVAL_POLICY`; `RETRIEVE_CANDIDATE_CONCEPTS` | V1 retrieval policy that runs lookup methods by descending weight, gives `exactLabel` weight `1.0`, gives `alias` weight `0.7`, stops on exact label hits, and keeps non-exact combinations below score `1.0`. |
| MATCHING_POLICY | Matching decision policy | State Table policy column | Defines threshold boundaries for exact, probable, ambiguous, and no-match matching outcomes and resolves them into approve, reject, propose, create, review, or block decisions. |
| HUMAN_REVIEW_POLICY | Review routing policy | State Table policy column | Defines reviewer ownership, allowed response shape, and due condition for policy-directed review items. |
| GRAPH_PERSISTENCE_POLICY | Graph write policy | State Table policy column | Requires idempotent graph writes, policy-approved concept creation, and explicit permission to persist pending review states. |
| COMPLETION_POLICY | Workflow completion policy | State Table policy column | Requires a committed KG transaction with persisted identifiers before the workflow can reach DONE. |
| STOP_POLICY | Workflow stop policy | State Table policy column | Stops the workflow for missing required input, unavailable dependency, invalid policy or reviewer response, or violated invariant. |
