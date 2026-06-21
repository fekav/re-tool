# Term Extraction KG Mapping Workflow Contract

## Purpose
Enable agents to extract domain terms from source text and map each term to an existing knowledge graph concept node, while preserving evidence, confidence, human decisions, and abort boundaries.

## Intended Consuming Agents
- Information extraction agent
- Knowledge graph curation agent
- Data quality verification agent

## Scope

### In Scope
- Intake and validation of source text or corpus references.
- Candidate term extraction with source evidence.
- Term normalization and deduplication.
- Knowledge graph concept candidate lookup.
- Mapping proposed terms to concept node identifiers.
- Human review for ambiguous, missing, or risky mappings.
- Durable mapping output and verification.

### Non-goals
- Designing a new ontology or knowledge graph schema.
- Training a new extraction model.
- Bulk graph migration or destructive graph edits.
- Creating new concept nodes unless a human explicitly approves that policy.
- Resolving facts, claims, or relationships beyond term-to-concept-node mapping.

## Global Execution Rules
- Do not write to the knowledge graph unless explicit write approval and target environment are provided.
- Preserve source evidence for every extracted term, including document reference and text span or quoted snippet.
- Treat missing extraction policy, graph schema, or mapping threshold as blocking, not as permission to guess.
- Keep unmapped, ambiguous, and rejected terms in the output with status and rationale.
- Use read-only graph access until the `PERSIST_MAPPING` state has all required approvals.

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Transition | Abort If |
|---|---|---|---|---|---|---|
| INTAKE | Capture the run request, source corpus, and mapping objective. | User request; source text, file path, document set, or corpus identifier. | Record run identifier; list source documents; record requested domain or scope; identify missing source references. | Intake record. | If at least one readable source reference exists, go to `VALIDATE_INPUTS`; if source reference is missing, go to `REQUEST_HUMAN_DECISION`. | No source text or source reference is provided. |
| VALIDATE_INPUTS | Verify that execution inputs are complete enough to run without guessing. | Intake record; source corpus; extraction policy; knowledge graph endpoint or export; concept node schema; mapping threshold or review policy; output target. | Check source readability; check graph read access; inspect concept identifier, label, synonym, and type fields; list missing required inputs; create validation report. | Validation report with pass, warning, and missing-input findings. | If all blocking inputs pass validation, go to `EXTRACT_TERMS`; if recoverable inputs are missing, go to `REQUEST_HUMAN_DECISION`. | Source is unreadable; graph source is unavailable; concept node schema cannot be identified; credentials or approvals are required but absent. |
| EXTRACT_TERMS | Produce candidate terms from the validated source corpus. | Validation report; source corpus; extraction policy defining language, term granularity, and exclusion rules. | Extract candidate terms; record source evidence; retain frequencies and document coverage; exclude terms only by provided policy. | Candidate term inventory. | If candidate inventory is non-empty, go to `NORMALIZE_TERMS`; if no terms are found, go to `VERIFY_RESULT` with an empty mapping result. | Extraction cannot produce evidence-backed candidates or the source language is unsupported by the approved extraction method. |
| NORMALIZE_TERMS | Convert candidate terms into canonical term records without losing variants. | Candidate term inventory; normalization rules or approved default normalization policy. | Deduplicate terms; group variants; preserve original surface forms; assign stable term IDs; record normalization rationale. | Normalized term table. | If each candidate has a canonical term ID and preserved variants, go to `FIND_CONCEPT_CANDIDATES`. | Normalization rules are absent and no approved default policy exists; different terms collapse into one canonical form without review criteria. |
| FIND_CONCEPT_CANDIDATES | Retrieve possible knowledge graph concept nodes for each normalized term. | Normalized term table; knowledge graph source; concept node schema; allowed lookup methods. | Query labels, aliases, synonyms, identifiers, and allowed search indexes; collect candidate node IDs; record lookup method and evidence. | Term-to-concept candidate table. | If lookup completes for every normalized term, go to `SCORE_MAPPINGS`. | Graph queries fail; candidate node identity fields are missing; lookup method would require unapproved data access. |
| SCORE_MAPPINGS | Propose one mapping status for every normalized term. | Term-to-concept candidate table; mapping criteria; confidence thresholds; review policy. | Score candidates; select unique high-confidence mappings; mark low-confidence, multi-match, no-match, and rejected candidates; explain rationale. | Mapping proposal. | If all mappings satisfy automatic acceptance policy, go to `PERSIST_MAPPING`; if any mapping needs review, go to `REQUEST_HUMAN_DECISION`. | Mapping criteria or thresholds are missing; scoring produces conflicting accepted nodes for the same term. |
| REQUEST_HUMAN_DECISION | Obtain required human decisions for missing inputs, ambiguous mappings, new node policy, or graph writes. | Decision packet containing exact question, affected terms or states, options, evidence, and recommended next state. | Ask the named owner for one allowed option; record response; update decision log; route execution according to the response. | Human decision log; approved policy updates or mapping decisions. | If missing execution inputs are supplied, return to `VALIDATE_INPUTS`; if mapping decisions are supplied, go to `PERSIST_MAPPING`; if the human chooses stop, go to `ABORTED`. | Human owner is unknown; response does not select an allowed option; response changes scope beyond this contract. |
| PERSIST_MAPPING | Write approved mapping results to the approved durable target. | Mapping proposal; human decision log if required; output target; write approval if updating a graph. | Write mapping file; write graph updates only if approved; record unmapped and rejected terms; produce write log. | Mapping output; persistence log. | If output is written and includes every normalized term with status, go to `VERIFY_RESULT`. | Output target is missing; write permission is absent; graph target is production and approval is missing; persistence would overwrite existing mappings without conflict policy. |
| VERIFY_RESULT | Validate that the workflow output is complete, traceable, and usable. | Mapping output; persistence log; validation report; verification rules. | Check schema; check every normalized term has status; check every accepted mapping has concept node ID and evidence; check unresolved items have rationale; create verification report. | Verification report. | If verification passes, go to `DONE`; if recoverable defects require human choice, go to `REQUEST_HUMAN_DECISION`; if defects are unrecoverable, go to `ABORTED`. | Output cannot be read; accepted mappings lack node IDs; evidence is missing; unresolved defects cannot be repaired under approved policy. |
| DONE | End with verified mapping artifacts. | Verification report with pass status; mapping output. | Report artifact locations; report unresolved terms and decisions; stop execution. | Final workflow status. | Terminal state; no further transition. | N/A terminal state. |
| ABORTED | Stop execution without pretending the workflow succeeded. | Abort reason; last durable artifact if any. | Record abort reason; preserve partial artifacts; report required human clarification or external fix. | Abort report. | Terminal state; no further transition. | N/A terminal state. |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| INTAKE | At least one non-empty source text or readable source reference is recorded. | VALIDATE_INPUTS | No | Source must be concrete enough for an agent to read. |
| INTAKE | Source text or source reference is missing. | REQUEST_HUMAN_DECISION | Yes, request owner | Ask for source text, path, corpus ID, or document set. |
| VALIDATE_INPUTS | Source, extraction policy, graph source, concept schema, mapping policy, and output target all pass validation. | EXTRACT_TERMS | No | Validation report must name the checked inputs. |
| VALIDATE_INPUTS | One or more recoverable required inputs are missing or ambiguous. | REQUEST_HUMAN_DECISION | Yes, request owner or data steward | Decision packet must list each missing input and affected state. |
| EXTRACT_TERMS | Candidate term inventory contains at least one evidence-backed term. | NORMALIZE_TERMS | No | Evidence must identify document and span or snippet. |
| EXTRACT_TERMS | Extraction completes with zero candidate terms and no extraction errors. | VERIFY_RESULT | No | Empty mapping result is allowed only when source and policy were valid. |
| NORMALIZE_TERMS | Every candidate has a canonical term ID and all variants are preserved. | FIND_CONCEPT_CANDIDATES | No | Canonicalization must not discard source forms. |
| FIND_CONCEPT_CANDIDATES | Candidate lookup was attempted for every normalized term and results are recorded. | SCORE_MAPPINGS | No | Terms with no candidates continue with `no_match` status. |
| SCORE_MAPPINGS | Every term has a mapping status that satisfies automatic acceptance policy. | PERSIST_MAPPING | No | Accepted mappings must include concept node IDs. |
| SCORE_MAPPINGS | Any term has low-confidence, multi-match, no-match with possible new node, or policy conflict status. | REQUEST_HUMAN_DECISION | Yes, data steward | Human response must select allowed mapping action per term or batch. |
| REQUEST_HUMAN_DECISION | Human supplies missing execution inputs. | VALIDATE_INPUTS | Yes, request owner or data steward | Revalidate supplied inputs before execution continues. |
| REQUEST_HUMAN_DECISION | Human approves or rejects mapping decisions within allowed options. | PERSIST_MAPPING | Yes, data steward | Decision log must include owner, timestamp, selected option, and affected terms. |
| REQUEST_HUMAN_DECISION | Human selects stop or cannot provide a required answer. | ABORTED | Yes, request owner or data steward | Preserve partial artifacts and abort reason. |
| PERSIST_MAPPING | Mapping output is written and includes every normalized term with status. | VERIFY_RESULT | No | Graph writes also require persistence log with target environment. |
| VERIFY_RESULT | Verification report passes all required checks. | DONE | No | Final response should name mapping output and unresolved items. |
| VERIFY_RESULT | Verification finds recoverable defects needing a human choice. | REQUEST_HUMAN_DECISION | Yes, data steward | Example: accepted mapping conflicts with existing curated mapping. |
| VERIFY_RESULT | Verification finds unrecoverable defects under current policy. | ABORTED | No | Example: output is unreadable or lacks accepted node IDs. |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| Source corpus | Human request owner | INTAKE, EXTRACT_TERMS | Plain text, file path, document set, corpus ID, or repository path supplied at run time. | Exists, is readable, non-empty, and within approved scope. |
| Extraction and mapping policy | Human request owner or data steward | VALIDATE_INPUTS, EXTRACT_TERMS, SCORE_MAPPINGS | Run configuration, issue comment, or project policy document. | Defines language, term granularity, exclusions, lookup methods, thresholds, and review triggers. |
| Knowledge graph concept source | Human data steward or platform owner | VALIDATE_INPUTS, FIND_CONCEPT_CANDIDATES | Endpoint, export file, database view, or indexed graph source supplied at run time. | Read access works and concept node identity, label, alias, and type fields are identifiable. |
| Intake record | INTAKE | VALIDATE_INPUTS | Run-local Markdown, CSV, JSON, or ticket note. | Names run ID, source references, requested scope, and request owner. |
| Validation report | VALIDATE_INPUTS | EXTRACT_TERMS, VERIFY_RESULT | Run-local Markdown or structured report. | Lists each required input with pass, warning, or missing status. |
| Candidate term inventory | EXTRACT_TERMS | NORMALIZE_TERMS | Table with term text, document reference, span or snippet, frequency, and extractor source. | Every row has evidence and no excluded term lacks exclusion rationale. |
| Normalized term table | NORMALIZE_TERMS | FIND_CONCEPT_CANDIDATES, VERIFY_RESULT | Table with term ID, canonical label, variants, evidence references, and normalization rationale. | Every candidate maps to exactly one term ID and variants are preserved. |
| Term-to-concept candidate table | FIND_CONCEPT_CANDIDATES | SCORE_MAPPINGS | Table with term ID, candidate concept node IDs, labels, match method, and evidence. | Every normalized term has a lookup status, including `no_candidates` where applicable. |
| Mapping proposal | SCORE_MAPPINGS | REQUEST_HUMAN_DECISION, PERSIST_MAPPING | Table with term ID, selected node ID or unresolved status, confidence, rationale, and review flag. | Every term has exactly one status: `accepted`, `needs_review`, `no_match`, `rejected`, or `deferred`. |
| Human decision log | REQUEST_HUMAN_DECISION | VALIDATE_INPUTS, PERSIST_MAPPING, VERIFY_RESULT | Append-only decision table or ticket comments. | Each decision names owner, timestamp, allowed option selected, affected terms or inputs, and next state. |
| Mapping output | PERSIST_MAPPING | VERIFY_RESULT, DONE | Approved file, database table, graph update log, or graph write transaction. | Includes every normalized term, final status, node ID when accepted, evidence, and decision reference when applicable. |
| Verification report | VERIFY_RESULT | DONE, ABORTED | Run-local Markdown or structured report. | Passes schema checks and lists unresolved defects or confirms none. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| Source selection | `INTAKE` has no readable source text or corpus reference. | Request owner | Provide text; provide path; provide corpus ID; stop. | One option plus concrete source value or stop instruction. |
| Policy completion | `VALIDATE_INPUTS` finds missing extraction, normalization, lookup, threshold, or output policy. | Request owner or data steward | Provide policy; approve documented default; stop. | Named policy values or explicit approval of the default policy. |
| Ambiguous concept mapping | `SCORE_MAPPINGS` finds multiple plausible concept nodes or confidence below threshold. | Data steward | Select candidate node; mark no match; reject term; defer term; request new node policy. | Per-term or batch decision with selected option and rationale. |
| New concept node policy | A term has no matching concept node but appears in scope. | Data steward or ontology owner | Create new node; leave unmapped; defer to ontology workflow; reject term. | Selected option and whether this workflow may create or only request new nodes. |
| Persistent graph write approval | `PERSIST_MAPPING` would update a graph rather than write a file-only artifact. | Graph owner or release owner | Approve write to named environment; require file-only output; stop. | Target environment, approval record, and write boundaries. |
| Conflict resolution | `VERIFY_RESULT` detects conflict with existing curated mapping or graph constraint. | Data steward | Keep existing mapping; replace mapping; add alternate mapping; defer; stop. | Selected conflict action and affected term or node IDs. |

## Abort Conditions
- Source text or corpus reference is absent, unreadable, or outside approved scope.
- Knowledge graph access, schema, or concept node identity fields cannot be validated.
- Extraction policy, normalization rules, mapping thresholds, or review triggers are missing and no human-approved default exists.
- Mapping would require creating or changing concept nodes without explicit human approval.
- A production graph write is requested without named target environment and write approval.
- Human owner is unknown or does not choose one of the allowed decision options.
- Verification cannot prove that every normalized term has a final status and traceable evidence.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Where is the canonical source text or corpus supplied for each run? | Agents cannot begin extraction without concrete readable source input. | Inline text in request; repository path; uploaded document set; corpus identifier. | `INTAKE`, source corpus artifact. |
| What knowledge graph system and concept node schema should be used? | Agents need stable node IDs, label fields, synonym fields, and allowed read methods. | RDF/SPARQL graph; property graph; database table; exported concept catalog. | `VALIDATE_INPUTS`, `FIND_CONCEPT_CANDIDATES`, knowledge graph concept source artifact. |
| What counts as a term for this domain? | Extraction quality depends on granularity and exclusions. | Noun phrases; named entities; controlled vocabulary terms; glossary candidates; domain-specific multi-word expressions. | `VALIDATE_INPUTS`, `EXTRACT_TERMS`, extraction and mapping policy artifact. |
| What matching methods and confidence thresholds are approved? | Agents cannot decide automatic acceptance versus human review safely without criteria. | Exact label match only; labels plus synonyms; fuzzy matching; embedding search; human review for all mappings. | `SCORE_MAPPINGS`, mapping proposal artifact. |
| Should no-match terms ever create new concept nodes? | Node creation changes ontology governance and may exceed this workflow. | Never create nodes; create only in sandbox; request separate ontology workflow; create with data steward approval. | `REQUEST_HUMAN_DECISION`, `PERSIST_MAPPING`. |
| Where should final mappings be persisted, and are graph writes allowed? | Persistence affects permissions, auditability, and rollback. | File-only artifact; staging graph update; production graph update; ticket attachment. | `PERSIST_MAPPING`, mapping output artifact. |

## Assumption Log

| Assumption | Source | Risk | How To Validate |
|---|---|---|---|
| The primary goal is mapping extracted terms to existing knowledge graph concept nodes. | User request: "term extraction from text, term mapping to knowledge graph concept node." | If new node creation is expected by default, this workflow will pause too often for human decisions. | Confirm new concept node policy with data steward or request owner. |
| A consuming agent will receive or can access the source text and graph source at run time. | Inference from requested workflow. | Execution cannot start if sources are not provided through the run context. | Validate source corpus and graph source artifacts during `VALIDATE_INPUTS`. |
| A data steward or ontology owner is available for ambiguous mappings. | Inference from knowledge graph curation needs. | Ambiguous mappings will abort if no owner can decide. | Name the owner role in project policy or run intake. |
| Durable output can be represented as a table even if the final target is a graph write. | Inference from auditability requirements. | Downstream systems may reject the table format or require a specific schema. | Confirm mapping output format before `PERSIST_MAPPING`. |

## Validation Checklist
- [ ] Every state has a clear goal.
- [ ] Every state defines required inputs.
- [ ] Every state defines allowed actions.
- [ ] Every state defines outputs.
- [ ] Every transition has an explicit condition.
- [ ] Every ambiguous or risky step has an abort condition.
- [ ] Human decision points are named explicitly.
- [ ] Open questions are separated from assumptions.
- [ ] Assumptions are visible and never silently applied.
