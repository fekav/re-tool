| Term | Type | Aliases | Used In | Description |
|---|---|---|---|---|
| Requirement KG Persistence | WORKFLOW | Requirement-to-KG ingestion | `docs/workflows/requirement-kg-persistence.md` | Workflow for persisting textual software requirements into a knowledge graph with provenance, extracted terms, mappings, and policy-governed concept creation. |
| Requirement Analyst | ROLE | Analyst | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for validating requirement interpretation and extraction correctness. |
| Test Engineer | ROLE | Tester | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing scenario, condition, and constraint mappings when policy assigns those decisions. |
| Software Developer | ROLE | Developer | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for reviewing mappings to implementation-facing concepts such as system components. |
| Domain Modeler | ROLE | Ontology curator; domain expert | `REQUEST_HUMAN_REVIEW`; Human Decision Points | Human role responsible for concept quality, concept creation approval, and ontology consistency. |
| RequirementRecord | ARTIFACT | Requirement node; raw requirement | `INTAKE_REQUIREMENT` | Durable representation of a raw requirement text with source provenance and ingestion metadata. |
| RequirementClassification | ARTIFACT | Requirement type assignment | `CLASSIFY_REQUIREMENT` | Assignment of the requirement to a supported KG type such as requirement, goal, or scenario. |
| TermMentionSet | ARTIFACT | Candidate terms; extracted terms | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted subject, object, action, condition, and constraint mentions linked to source text spans. |
| CandidateConceptMatchSet | ARTIFACT | Candidate concepts; retrieval results | `RETRIEVE_CANDIDATE_CONCEPTS` | Ranked KG concepts that may match extracted term mentions, including match scores and evidence. |
| TermConceptMappingSet | ARTIFACT | Mapping candidates | `CREATE_MAPPING_CANDIDATES` | Candidate mappings between extracted term mentions and KG concepts, with status and rationale. |
| ConceptCreationPolicy | ARTIFACT | Creation policy; mapping policy | `EVALUATE_CONCEPT_POLICY` | Configured rules that decide whether no-match or ambiguous terms are auto-created, proposed, reviewed, blocked, or rejected. |
| MappingDecisionSet | ARTIFACT | Mapping decisions | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW` | Final or pending decisions for term-to-concept mappings and concept proposals. |
| ConceptProposalSet | ARTIFACT | New concept proposals | `EVALUATE_CONCEPT_POLICY`; `REQUEST_HUMAN_REVIEW` | Proposed new KG concepts generated when extracted terms do not sufficiently match existing concepts. |
| PersistenceVerificationReport | ARTIFACT | Verification report | `VERIFY_QUERYABILITY` | Report proving the persisted requirement can be queried by required identifiers, provenance, terms, mappings, and review states. |
| Raw Requirement Text | ARTIFACT | Source text | `INTAKE_REQUIREMENT` | Original textual software requirement preserved for provenance and auditability. |
| Provenance | ARTIFACT | Source metadata; traceability | `INTAKE_REQUIREMENT`; `PERSIST_GRAPH_CHANGES` | Metadata linking KG facts back to the raw requirement source, submitter, and ingestion event. |
| Requirement | DOMAIN_TERM | Software requirement | `CLASSIFY_REQUIREMENT`; Required Artifacts | KG concept representing a required system behavior, quality, constraint, or capability. |
| Goal | DOMAIN_TERM | Objective | `CLASSIFY_REQUIREMENT`; Open Questions | KG concept representing a desired outcome or stakeholder objective. |
| Scenario | DOMAIN_TERM | Use case; example flow | `CLASSIFY_REQUIREMENT`; Open Questions | KG concept representing a concrete situation or behavioral path relevant to a requirement. |
| SystemComponent | DOMAIN_TERM | Backend component; service; module | `RETRIEVE_CANDIDATE_CONCEPTS`; Human Decision Points | KG concept representing an implementation-facing system part. |
| UIComponent | DOMAIN_TERM | User interface component; screen element | `RETRIEVE_CANDIDATE_CONCEPTS`; Human Decision Points | KG concept representing a visible or interactive user interface part. |
| Subject | DOMAIN_TERM | Actor; grammatical subject | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted syntax role identifying who or what performs or owns an action. |
| Object | DOMAIN_TERM | Target; grammatical object | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted syntax role identifying what an action affects. |
| Action | DOMAIN_TERM | Behavior; verb | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted syntax role identifying required behavior or operation. |
| Condition | DOMAIN_TERM | Precondition; trigger; circumstance | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted syntax role identifying when or under what situation the requirement applies. |
| Constraint | DOMAIN_TERM | Rule; limit; restriction | `EXTRACT_REQUIREMENT_SYNTAX` | Extracted syntax role identifying a required limitation, quality, or boundary. |
| INTAKE_REQUIREMENT | STATE | Intake | State Table | Start state that captures raw requirement text and provenance. |
| CLASSIFY_REQUIREMENT | STATE | Classify | State Table | State that assigns an initial KG type and context to the requirement. |
| EXTRACT_REQUIREMENT_SYNTAX | STATE | Extract syntax | State Table | State that extracts candidate terms from requirement syntax. |
| RETRIEVE_CANDIDATE_CONCEPTS | STATE | Retrieve matches | State Table | State that searches the KG for concepts matching extracted terms. |
| CREATE_MAPPING_CANDIDATES | STATE | Create mappings | State Table | State that converts retrieval results into explicit mapping candidates. |
| REQUEST_POLICY_DECISION | STATE | Policy selection | State Table | State that obtains the governing concept creation policy when it is not already configured. |
| EVALUATE_CONCEPT_POLICY | STATE | Evaluate policy | State Table | State that applies the concept creation policy to mappings and no-match terms. |
| REQUEST_HUMAN_REVIEW | STATE | Human review | State Table | State that collects required review decisions for ambiguous mappings and concept proposals. |
| PERSIST_GRAPH_CHANGES | STATE | Persist graph | State Table | State that writes requirement, provenance, terms, mappings, and approved concepts to the KG. |
| VERIFY_QUERYABILITY | STATE | Verify queryability | State Table | State that confirms the requirement can be queried from the KG. |
| DONE | STATE | Complete | State Table | Terminal state for successful persistence and verification. |
| ABORTED | STATE | Failed; stopped | State Table | Terminal state for unsafe or impossible continuation. |
| AUTO_CREATE | DECISION | Automatic creation | Human Decision Points | Policy option allowing new concepts to be created without human approval under configured conditions. |
| PROPOSE_ONLY | DECISION | Proposal only | Human Decision Points | Policy option that creates concept proposals without immediately creating KG concepts. |
| REQUIRE_APPROVAL | DECISION | Manual approval | Human Decision Points | Policy option requiring a human decision before concept creation or certain mappings. |
| BLOCK | DECISION | Stop | Human Decision Points | Policy option that prevents persistence or concept creation for unresolved cases. |
