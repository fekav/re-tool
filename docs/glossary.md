# Glossary

| Term | Type | Description |
|---|---|---|
| ABORTED | STATE | Terminal state reached when a blocking condition prevents safe workflow execution. |
| Aborted workflow status | ARTIFACT | Terminal status artifact that records the workflow ended because of a blocking condition. |
| Abort reason | ARTIFACT | Explanation of the condition that forced the workflow to stop. |
| Accepted mapping | ARTIFACT | Mapping candidate accepted by the mapping policy or by human review. |
| APPLY_HUMAN_DECISIONS | STATE | State that applies reviewer decisions to ambiguous or unmapped terms. |
| Ambiguity report | ARTIFACT | Workflow artifact listing terms that need human selection or confirmation. |
| Ambiguous mapping decision | CONDITION | Human decision condition triggered when one term has multiple candidate graph concepts. |
| Candidate term | ARTIFACT | Extracted term mention before normalization and deduplication. |
| Candidate term list | ARTIFACT | Workflow artifact containing candidate term text, source reference, and occurrence data. |
| Concept ID | ARTIFACT | Stable identifier for a graph node concept. |
| Completed workflow status | ARTIFACT | Terminal status artifact that records the workflow ended with validated mapping artifacts. |
| Confidence score | ARTIFACT | Numeric or categorical score used by the mapping policy to compare mapping candidates. |
| DONE | STATE | Terminal state reached when final validated mapping artifacts have been produced. |
| Domain reviewer | ARTIFACT | Human role responsible for resolving ambiguous or unmapped term decisions. |
| Empty extraction decision | CONDITION | Human decision condition triggered when term extraction returns zero candidate terms. |
| EXTRACT_TERMS | STATE | State that extracts candidate terms from validated source text. |
| Extraction settings | ARTIFACT | Runtime configuration that defines extraction scope, exclusions, and allowed term shapes. |
| Final term-concept mapping | ARTIFACT | Durable mapping artifact containing one final status for each normalized term. |
| GENERATE_MAPPING_CANDIDATES | STATE | State that compares normalized terms with graph node concepts and records candidate mappings. |
| Graph concept inventory | ARTIFACT | Runtime list or query result of graph node concepts available for term mapping. |
| Graph node concept | ARTIFACT | Concept represented as a graph node with at least a stable ID and label. |
| Human decision record | ARTIFACT | Workflow artifact containing a reviewer or owner decision and required IDs or configuration values. |
| Human decision request | ARTIFACT | Workflow artifact that asks a human owner for a missing input, policy, or review decision. |
| Human owner | ARTIFACT | Human role responsible for supplying required runtime inputs or choosing to abort. |
| INTAKE | STATE | Start state that captures the workflow request and identifies supplied runtime materials. |
| Intake record | ARTIFACT | Workflow artifact naming supplied inputs and missing inputs. |
| Input validation report | ARTIFACT | Workflow artifact confirming whether source text and graph concept inventory are usable. |
| Invalid final mapping correction | CONDITION | Human decision condition triggered when final validation finds invalid concept IDs or unresolved ambiguity. |
| Mapping candidate | ARTIFACT | Candidate relationship between a normalized term and a graph node concept. |
| Mapping candidate list | ARTIFACT | Workflow artifact containing candidate concept IDs, confidence when used, and rationale for each term. |
| Mapping policy | ARTIFACT | Runtime configuration that defines scoring, acceptance, ambiguity, unmapped handling, and new concept policy. |
| Mapping rationale | ARTIFACT | Explanation recorded for why a term was mapped, left unmapped, or sent for review. |
| Mapping validation report | ARTIFACT | Workflow artifact that verifies final mapping schema, concept references, confidence, rationale, and statuses. |
| Missing input list | ARTIFACT | Intake output naming required runtime inputs that have not been supplied. |
| Missing runtime input decision | CONDITION | Human decision condition triggered when required runtime inputs or configuration are missing. |
| New concept proposal list | ARTIFACT | Workflow artifact listing proposed graph node concepts when policy allows proposals. |
| NORMALIZE_TERMS | STATE | State that canonicalizes and deduplicates candidate terms into stable normalized terms. |
| Normalized term | ARTIFACT | Candidate term after canonicalization, deduplication, and stable term ID assignment. |
| Normalized term list | ARTIFACT | Workflow artifact containing stable term IDs, normalized labels, and original mentions. |
| Original mention | ARTIFACT | The source-text form of a term before normalization. |
| Output schema | ARTIFACT | Required shape of the final mapping artifact. |
| Provisional mapping report | ARTIFACT | Mapping artifact produced before human decisions are applied. |
| REQUEST_HUMAN_DECISION | STATE | State that obtains missing choices or review decisions from a human owner or reviewer. |
| RESOLVE_AMBIGUITIES | STATE | State that separates automatic mappings from ambiguous and unmapped terms using the mapping policy. |
| Reviewed mapping report | ARTIFACT | Mapping artifact after human decisions have been applied. |
| Source reference | ARTIFACT | Pointer from an extracted term back to its original location in the source text. |
| Source span | ARTIFACT | Exact character range, sentence ID, or document location for a candidate term. |
| Source text | ARTIFACT | Runtime text input from which terms are extracted. |
| Stable term ID | ARTIFACT | Identifier assigned to a normalized term within one workflow run. |
| Term-concept mapping | ARTIFACT | Relationship between an extracted normalized term and a graph node concept or final unmapped status. |
| Term frequency | ARTIFACT | Count of how often a candidate term appears in the source text. |
| Tie rule | CONDITION | Mapping policy condition that defines how equal or near-equal mapping candidates are handled. |
| Unmapped rule | CONDITION | Mapping policy condition that defines when a term must be marked unmapped. |
| Unmapped term decision | CONDITION | Human decision condition triggered when no acceptable graph concept exists for a term. |
| Unmapped term list | ARTIFACT | Workflow artifact listing normalized terms with no accepted graph node concept and the reason. |
| VALIDATE_INPUTS | STATE | State that confirms runtime materials are readable and have required fields. |
| VALIDATE_MAPPING | STATE | State that verifies final mapping consistency before terminal completion. |
