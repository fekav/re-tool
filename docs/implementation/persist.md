# Graph Persistence Planning Note

The `graphpersistence` slice is postponed until after the Neo4j graph-model
redesign. The persistence plan must target the ontology-near graph model in
[docs/graph-model.md](/workspace/docs/graph-model.md), not the previous
extraction-shaped node model.

## Current Contract

- `RequirementAnalysisCompletedEvent` carries the domain `Action` result and
  `NodeMatchDecision` values.
- Graph writes translate the domain result into `Mention`, `Concept`,
  `Predicate`, `Qualifier`, and `Assertion` nodes.
- Semantic identity is the base subject-predicate-object triple:
  subject concept canonical name, predicate canonical name, and object concept
  canonical name.
- Qualifiers enrich the base assertion and do not participate in assertion
  identity.
- Candidate resolution uses `CandidateNode`, `RetrievedCandidateNode`,
  `CandidateNodeMatch`, and `NodeMatchDecision`.

## Persistence Slice Implications

- Technical idempotency can still use `Provenance.id`.
- Semantic idempotency should use the deterministic `Assertion.assertionKey`.
- `AUTO_MAP_EXISTING` and `AUTO_CREATE_NEW` remain final decisions.
- `PROPOSE_EXISTING` and `REVIEW_REQUIRED` still require reviewer actions before
  persistence.
- The Neo4j adapter should commit the target graph relationships documented in
  `docs/graph-model.md`.
