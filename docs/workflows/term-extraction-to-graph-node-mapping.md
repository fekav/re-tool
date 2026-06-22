# Term Extraction To Graph Node Mapping Workflow Contract

## Purpose
Enable an agent to extract terms from a supplied source text and map each extracted term to graph node concepts using explicit inputs, mapping rules, validation, and human review for ambiguous cases.

## Scope

### In Scope
- Validate that source text and graph node concepts are available in usable formats.
- Extract candidate terms from the source text.
- Normalize and deduplicate extracted terms.
- Generate mapping candidates between normalized terms and graph node concepts.
- Produce a final term-concept mapping with rationale, confidence, and unresolved items.

### Non-goals
- Mutating a production graph database.
- Creating authoritative new graph node concepts unless a human explicitly chooses that policy.
- Training or tuning a term extraction model.
- Resolving domain disagreements that require subject-matter judgment beyond the supplied mapping policy.

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Abort If |
|---|---|---|---|---|---|
| INTAKE START | Capture the workflow request and identify the runtime materials. | Workflow request; source text or source text reference; graph concept inventory or graph concept inventory reference | Record supplied inputs; identify missing inputs; create an intake record; do not extract or map terms | Intake record; missing input list | Both source text and graph concept inventory are absent. |
| VALIDATE_INPUTS | Confirm that the runtime materials are readable and contain the minimum fields needed by later states. | Intake record; source text; graph concept inventory | Parse input formats; verify source text is non-empty; verify graph concepts have stable concept IDs and labels; verify whether extraction settings and mapping policy are supplied | Input validation report; human decision request if required inputs are missing | Source text is unreadable or empty; graph concept inventory is unreadable or lacks stable concept IDs. |
| REQUEST_HUMAN_DECISION | Obtain missing choices or review decisions needed before execution can continue. | Human decision request; open question or ambiguity report requiring a decision | Ask the human owner for the required response; capture the response exactly; validate the response format; route the workflow based on the response | Human decision record; updated extraction settings, mapping policy, or mapping decisions | No human owner is available for a blocking decision; the response omits required term IDs, concept IDs, or selected options. |
| EXTRACT_TERMS | Produce candidate terms from the validated source text. | Input validation report; source text; extraction settings | Segment source text; identify candidate terms; preserve source spans or references; record term frequency; exclude terms only when the extraction settings allow it | Candidate term list | Extraction settings are missing after the decision path; extraction cannot run on the supplied source text. |
| NORMALIZE_TERMS | Convert candidate terms into stable normalized terms for mapping. | Candidate term list; extraction settings | Canonicalize casing and whitespace; merge duplicate candidates; preserve original mentions; assign stable term IDs | Normalized term list | Candidate term list is malformed or lacks original term text. |
| GENERATE_MAPPING_CANDIDATES | Compare normalized terms with graph node concepts and produce candidate mappings. | Normalized term list; graph concept inventory; mapping policy | Match terms to concept labels and aliases; calculate confidence scores if the mapping policy defines scoring; record mapping rationale; keep all candidates needed for review | Mapping candidate list | Mapping policy is missing after the decision path; graph concept inventory cannot be queried or searched. |
| RESOLVE_AMBIGUITIES | Separate automatic mappings from ambiguous and unmapped terms. | Mapping candidate list; mapping policy | Apply explicit thresholds and tie rules; mark accepted mappings; create ambiguity report; create unmapped term list; request human review when policy requires it | Provisional mapping report; ambiguity report; unmapped term list; human decision request if needed | Mapping policy does not define automatic acceptance, ambiguity, or unmapped conditions. |
| APPLY_HUMAN_DECISIONS | Apply human review decisions to ambiguous or unmapped terms. | Human decision record; ambiguity report or unmapped term list; graph concept inventory | Apply selected concept IDs; mark terms as intentionally unmapped; record approved new concept proposals if allowed by policy; preserve reviewer rationale | Reviewed mapping report; new concept proposal list if allowed | A human decision references a missing term ID or missing concept ID; a requested new concept proposal is not allowed by policy. |
| VALIDATE_MAPPING | Verify that the final mapping is internally consistent and usable by downstream agents. | Provisional mapping report or reviewed mapping report; graph concept inventory; mapping policy | Verify every mapped concept ID exists; verify required confidence and rationale fields; verify unresolved terms are explicitly marked; verify output schema | Mapping validation report; final term-concept mapping | Any final mapping references a nonexistent concept ID; unresolved ambiguous terms remain without an explicit unmapped or review status. |
| DONE TERMINAL | End with validated mapping artifacts. | Mapping validation report; final term-concept mapping | Report artifact locations and validation status; perform no further workflow actions | Completed workflow status | A downstream action is requested under this workflow after terminal status. |
| ABORTED TERMINAL | End after a blocking condition prevents safe execution. | Abort reason; last valid workflow artifact, if any | Report abort reason; preserve partial artifacts; identify the blocked state | Aborted workflow status | A downstream action is requested under this workflow after terminal status. |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| INTAKE | Source text and graph concept inventory are present or referenced. | VALIDATE_INPUTS | No | References may be file paths, database query descriptors, or inline content if the agent can read them. |
| INTAKE | Source text or graph concept inventory is missing, but at least one required input is present. | REQUEST_HUMAN_DECISION | Yes, human owner | The request must ask only for the missing runtime artifact. |
| INTAKE | Both source text and graph concept inventory are absent. | ABORTED | No | The workflow cannot identify its core inputs. |
| VALIDATE_INPUTS | Required runtime materials are readable, source text is non-empty, graph concepts have stable IDs, and extraction settings plus mapping policy are present. | EXTRACT_TERMS | No | Proceed without human input only when validation passes. |
| VALIDATE_INPUTS | Extraction settings or mapping policy is missing. | REQUEST_HUMAN_DECISION | Yes, human owner | The request must name the missing setting or policy field. |
| VALIDATE_INPUTS | Source text is empty or unreadable, or graph concept inventory lacks stable IDs. | ABORTED | No | Partial artifacts may include the input validation report. |
| REQUEST_HUMAN_DECISION | Human response supplies missing source text, graph concept inventory, extraction settings, or mapping policy. | VALIDATE_INPUTS | Yes, human owner | Re-validate all runtime materials after receiving new inputs. |
| REQUEST_HUMAN_DECISION | Human response supplies ambiguity or unmapped-term decisions with required term IDs and concept IDs or statuses. | APPLY_HUMAN_DECISIONS | Yes, domain reviewer | Applies only after `RESOLVE_AMBIGUITIES` creates a review request. |
| REQUEST_HUMAN_DECISION | Human owner chooses to abort or fails to provide a required response. | ABORTED | Yes, human owner | Record the missing decision in the abort reason. |
| EXTRACT_TERMS | Candidate term list contains at least one term with source reference. | NORMALIZE_TERMS | No | Every candidate term must retain a link to original text. |
| EXTRACT_TERMS | Candidate term list is empty. | REQUEST_HUMAN_DECISION | Yes, domain reviewer | Human must choose whether to accept an empty mapping, revise extraction settings, or abort. |
| NORMALIZE_TERMS | Normalized term list contains stable term IDs and original mentions. | GENERATE_MAPPING_CANDIDATES | No | Original mentions are needed for traceability. |
| NORMALIZE_TERMS | Candidate term list is malformed or lacks original term text. | ABORTED | No | The agent cannot safely recover missing term text. |
| GENERATE_MAPPING_CANDIDATES | Mapping candidate list exists for every normalized term. | RESOLVE_AMBIGUITIES | No | Terms may have zero candidates if the list explicitly records that status. |
| GENERATE_MAPPING_CANDIDATES | Graph concept inventory cannot be searched or mapping policy is missing. | ABORTED | No | Missing policy should have been resolved before this state. |
| RESOLVE_AMBIGUITIES | All terms are either automatically mapped or explicitly marked unmapped by policy. | VALIDATE_MAPPING | No | No human review is needed when policy handles every term. |
| RESOLVE_AMBIGUITIES | Ambiguous mappings or unmapped terms require review under the mapping policy. | REQUEST_HUMAN_DECISION | Yes, domain reviewer | The review request must include term IDs, candidate concept IDs, confidence, and rationale. |
| RESOLVE_AMBIGUITIES | Mapping policy lacks acceptance, ambiguity, or unmapped rules. | ABORTED | No | This prevents implicit mapping decisions. |
| APPLY_HUMAN_DECISIONS | All human decisions reference valid term IDs and allowed concept IDs or statuses. | VALIDATE_MAPPING | No | New concept proposals remain proposals unless mutation is separately authorized. |
| APPLY_HUMAN_DECISIONS | A decision references a missing term ID, missing concept ID, or disallowed new concept proposal. | REQUEST_HUMAN_DECISION | Yes, domain reviewer | Ask for corrected decisions instead of guessing. |
| VALIDATE_MAPPING | Final term-concept mapping passes schema, concept ID, confidence, rationale, and unresolved-status checks. | DONE | No | The final mapping is the durable terminal artifact. |
| VALIDATE_MAPPING | Final mapping contains nonexistent concept IDs or unresolved ambiguous terms. | REQUEST_HUMAN_DECISION | Yes, domain reviewer | Ask for corrections only for the invalid rows. |
| DONE | Terminal status reached and final artifacts have been reported. | DONE | No | Terminal state; no further workflow action. |
| ABORTED | Terminal status reached and abort reason has been reported. | ABORTED | No | Terminal state; no further workflow action. |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| Source text | Human owner or upstream system | INTAKE; VALIDATE_INPUTS; EXTRACT_TERMS | Runtime input: inline text, file path, document export, or text extraction output | Exists, is readable, and contains non-empty text. |
| Graph concept inventory | Human owner or upstream graph system | INTAKE; VALIDATE_INPUTS; GENERATE_MAPPING_CANDIDATES; VALIDATE_MAPPING | Runtime input: JSON, CSV, graph query result, or equivalent concept list | Each concept has a stable concept ID and label; aliases are optional unless required by mapping policy. |
| Extraction settings | Human owner or default supplied by project policy | VALIDATE_INPUTS; EXTRACT_TERMS; NORMALIZE_TERMS | Runtime configuration | Defines extraction scope, exclusion rules, and whether multi-word terms are allowed. |
| Mapping policy | Human owner or default supplied by project policy | VALIDATE_INPUTS; GENERATE_MAPPING_CANDIDATES; RESOLVE_AMBIGUITIES; VALIDATE_MAPPING | Runtime configuration | Defines scoring method if used, automatic acceptance rule, ambiguity rule, unmapped rule, and new concept policy. |
| Intake record | INTAKE | VALIDATE_INPUTS | Workflow run artifact | Names supplied inputs and missing inputs. |
| Input validation report | VALIDATE_INPUTS | EXTRACT_TERMS | Workflow run artifact | States that source text is readable and graph concept inventory has stable IDs. |
| Candidate term list | EXTRACT_TERMS | NORMALIZE_TERMS | Workflow run artifact | Contains candidate term text, source reference, and frequency or occurrence count. |
| Normalized term list | NORMALIZE_TERMS | GENERATE_MAPPING_CANDIDATES | Workflow run artifact | Contains stable term IDs, normalized labels, and original mentions. |
| Mapping candidate list | GENERATE_MAPPING_CANDIDATES | RESOLVE_AMBIGUITIES | Workflow run artifact | Contains term IDs, candidate concept IDs, confidence if used, and rationale. |
| Ambiguity report | RESOLVE_AMBIGUITIES | REQUEST_HUMAN_DECISION; APPLY_HUMAN_DECISIONS | Workflow run artifact | Lists only terms that require human selection or confirmation. |
| Unmapped term list | RESOLVE_AMBIGUITIES | REQUEST_HUMAN_DECISION; APPLY_HUMAN_DECISIONS; VALIDATE_MAPPING | Workflow run artifact | Lists terms with no accepted concept and records reason. |
| Human decision record | REQUEST_HUMAN_DECISION | VALIDATE_INPUTS; APPLY_HUMAN_DECISIONS | Workflow run artifact | Contains owner, decision timestamp, selected option, and required IDs or supplied artifacts. |
| Reviewed mapping report | APPLY_HUMAN_DECISIONS | VALIDATE_MAPPING | Workflow run artifact | Applies each human decision exactly once and preserves rationale. |
| New concept proposal list | APPLY_HUMAN_DECISIONS | VALIDATE_MAPPING; out-of-scope graph governance workflow | Workflow run artifact | Exists only when mapping policy allows proposals; each proposal links to source term IDs. |
| Mapping validation report | VALIDATE_MAPPING | DONE | Workflow run artifact | Confirms schema, concept ID references, required confidence, rationale, and unresolved statuses. |
| Final term-concept mapping | VALIDATE_MAPPING | DONE | Workflow run artifact | Contains one final status for each normalized term: mapped, unmapped, or proposed-new-concept. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| Missing runtime input decision | Source text, graph concept inventory, extraction settings, or mapping policy is missing. | Human owner | Provide missing artifact; provide missing configuration; abort workflow | Artifact content or path, configuration values, or explicit abort choice. |
| Empty extraction decision | `EXTRACT_TERMS` produces zero candidate terms. | Domain reviewer | Accept empty mapping; revise extraction settings; abort workflow | Selected option and revised extraction settings if revision is chosen. |
| Ambiguous mapping decision | One normalized term has multiple candidate concepts and the mapping policy requires review. | Domain reviewer | Select one concept ID; mark term unmapped; request new concept proposal if allowed | Term ID, selected option, concept ID when mapped, and reviewer rationale. |
| Unmapped term decision | One normalized term has no acceptable graph node concept and the mapping policy requires review. | Domain reviewer | Mark term unmapped; request new concept proposal if allowed; revise mapping policy | Term ID, selected option, and proposed concept label if proposal is allowed. |
| Invalid final mapping correction | `VALIDATE_MAPPING` finds nonexistent concept IDs or unresolved ambiguous terms. | Domain reviewer | Correct concept ID; mark unmapped; return to ambiguity review; abort workflow | Invalid row ID, corrected value or status, and reviewer rationale. |

## Abort Conditions
- Source text is absent, unreadable, or empty after the missing-input decision path.
- Graph concept inventory is absent, unreadable, or lacks stable concept IDs after the missing-input decision path.
- Extraction settings or mapping policy remain missing after a human decision request.
- No human owner is available for a blocking missing-input, ambiguity, unmapped-term, or correction decision.
- A human decision references a term ID or concept ID that does not exist in the current workflow run.
- The final term-concept mapping references nonexistent graph node concepts or leaves ambiguous terms unresolved.
- The request requires mutating a production graph as part of this workflow rather than producing mapping artifacts.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| What source text formats must agents support? | Input parsing and source references depend on the format. | Plain text, Markdown, PDF text extraction output, HTML text extraction output | VALIDATE_INPUTS; EXTRACT_TERMS |
| What graph concept inventory format must agents support? | Mapping requires stable concept IDs, labels, and optional aliases in a searchable representation. | JSON list, CSV export, graph database query result, ontology file | VALIDATE_INPUTS; GENERATE_MAPPING_CANDIDATES |
| What mapping policy defines automatic acceptance and ambiguity? | Agents cannot safely decide mappings without explicit rules. | Exact label match only, label plus alias match, fuzzy lexical match with threshold, semantic similarity with threshold | GENERATE_MAPPING_CANDIDATES; RESOLVE_AMBIGUITIES; VALIDATE_MAPPING |
| Are new graph node concepts allowed as proposals? | Unmapped terms need different handling depending on graph governance policy. | Proposals allowed as artifacts only, proposals disallowed, proposals routed to a separate graph governance workflow | APPLY_HUMAN_DECISIONS; VALIDATE_MAPPING |
| Who owns human review decisions? | Blocking decisions require an accountable owner and response format. | Domain reviewer, ontology owner, product owner, data steward | REQUEST_HUMAN_DECISION; APPLY_HUMAN_DECISIONS |
| What final output schema and location should agents use? | Downstream agents need a predictable artifact for consumption. | JSON mapping file, CSV mapping file, Markdown report, project-specific run directory | VALIDATE_MAPPING; DONE |

## Assumption Log

| Assumption | Source | Risk | How To Validate |
|---|---|---|---|
| Source text and graph concept inventory are runtime inputs supplied by a human or upstream system. | User request mentions "given text" and "graph node concepts." | The workflow may ask for artifacts that another system should derive automatically. | Confirm the runtime input source for text and graph concepts. |
| The workflow produces mapping artifacts and does not mutate the graph. | Non-goal inferred from the request focusing on extraction and mapping. | A consuming agent may expect graph writes. | Confirm whether graph updates are handled by a separate workflow. |
| Ambiguous or unmapped cases require human review unless mapping policy explicitly resolves them. | Safety inference from mapping extracted terms to graph concepts. | Too many human review requests may slow automated execution. | Define automatic acceptance, ambiguity, and unmapped thresholds. |
| Candidate term extraction preserves source references for traceability. | Workflow design inference. | Reviewers may not be able to verify why a term was extracted. | Confirm whether source spans, sentence IDs, or document locations are required. |
| New concept creation, if needed, is proposal-only in this workflow. | Graph mutation is listed as a non-goal. | Users may expect new nodes to be inserted directly. | Confirm graph governance and mutation authorization. |
