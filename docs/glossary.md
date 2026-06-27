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
| CandidateConceptMatchSet | Candidate concepts; retrieval results | `RETRIEVE_CANDIDATE_CONCEPTS` | Ranked KG concepts that may match extracted term mentions, including match scores and evidence. |
| TermConceptMappingSet | Mapping candidates | `CREATE_MAPPING_CANDIDATES` | Candidate mappings between extracted term mentions and KG concepts, with status and rationale. |
| ConceptCreationPolicy | Creation policy; mapping policy | `EVALUATE_CONCEPT_POLICY` | Configured rules that decide whether no-match or ambiguous terms are auto-created, proposed, reviewed, blocked, or rejected. |
| MappingDecisionSet | Mapping decisions | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW` | Final or pending decisions for term-to-concept mappings and concept proposals. |
| ConceptProposalSet | New concept proposals | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW` | Proposed new KG concepts generated when extracted terms do not sufficiently match existing concepts. |
| PersistenceVerificationReport | Verification report | `VERIFY_QUERYABILITY` | Report proving the persisted requirement can be queried by required identifiers, provenance, terms, mappings, and review states. |
| Raw Requirement Text | Source text; RawText | `INTAKE_REQUIREMENT` | Original textual software requirement preserved for provenance and auditability. |
| Provenance | Source metadata; traceability | `INTAKE_REQUIREMENT`; `PERSIST_GRAPH_CHANGES` | Metadata linking KG facts back to the raw requirement source, submitter, and ingestion event. |
| SyntaxExtractionOutput | Requirement syntax DTO | `EXTRACT_REQUIREMENT_SYNTAX`; `docs/json-contracts.md` | Boundary DTO representing structured model output before it is validated and mapped to the Action domain model. |
| StructuredOutputContract | DTO validation contract | `docs/json-contracts.md` | App-owned description of required DTO fields used by the handwritten structured-output validator. |
| StructuredOutputValidator | Handwritten DTO validator | `docs/json-contracts.md` | Small reusable validator that checks DTOs against StructuredOutputContract instances before mapping to domain types. |
| StructuredOutputValidationException | Structured output validation failure | `docs/json-contracts.md` | Failure raised when model output DTOs do not satisfy their StructuredOutputContract. |
| InvalidStructuredOutputException | Invalid model output | `docs/json-contracts.md` | Failure raised when model output cannot be parsed, validated, or mapped into an application-owned DTO. |

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
| RETRIEVE_CANDIDATE_CONCEPTS | Retrieve matches | State Table | State that searches the KG for concepts matching extracted terms. |
| CREATE_MAPPING_CANDIDATES | Create mappings | State Table | State that converts retrieval results into explicit mapping candidates. |
| REQUEST_POLICY_DECISION | Policy selection | State Table | State that obtains the governing concept creation policy when it is not already configured. |
| EVALUATE_CONCEPT_POLICY | Evaluate policy | State Table | State that applies the concept creation policy to mappings and no-match terms. |
| REQUEST_HUMAN_REVIEW | Human review | State Table | State that collects required review decisions for ambiguous mappings and concept proposals. |
| PERSIST_GRAPH_CHANGES | Persist graph | State Table | State that writes requirement, provenance, terms, mappings, and approved concepts to the KG. |
| VERIFY_QUERYABILITY | Verify queryability | State Table | State that confirms the requirement can be queried from the KG. |
| DONE | Complete | State Table | Terminal state for successful persistence and verification. |
| ABORTED | Failed; stopped | State Table | Terminal state for unsafe or impossible continuation. |

## Decisions

| Term | Aliases | Used In | Description |
|---|---|---|---|
| AUTO_CREATE | Automatic creation | Human Decision Points | Policy option allowing new concepts to be created without human approval under configured conditions. |
| PROPOSE_ONLY | Proposal only | Human Decision Points | Policy option that creates concept proposals without immediately creating KG concepts. |
| REQUIRE_APPROVAL | Manual approval | Human Decision Points | Policy option requiring a human decision before concept creation or certain mappings. |
| BLOCK | Stop | Human Decision Points | Policy option that prevents persistence or concept creation for unresolved cases. |
