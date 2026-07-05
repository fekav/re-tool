# Neo4j Graph Model

## Purpose

The graph model separates requirement-domain language from generic graph
language. `Requirement` remains the source of a statement. Extracted domain
syntax is persisted as mentions and canonical semantic nodes, not as extraction
DTO or domain entity nodes.

## Labels

| Label | Meaning | Identity |
|---|---|---|
| `Requirement` | Fachliche Anforderung and source of assertions | `id` |
| `Provenance` | Source and ingestion metadata for a requirement | `id` |
| `Mention` | Concrete text span or phrase found in a requirement | `id` |
| `Concept` | Reusable domain object used as assertion subject or object | `canonicalName.strip()` |
| `Predicate` | Reusable relation or action term | `canonicalName.strip()` |
| `Qualifier` | Condition or constraint enriching an assertion | `(qualifierKind, canonicalText.strip())` |
| `Assertion` | Canonical subject-predicate-object fact | deterministic `assertionKey` from subject concept, predicate, and object concept |

`qualifierKind` is `CONDITION` or `CONSTRAINT`.

## Relationships

```text
(Requirement)-[:HAS_PROVENANCE]->(Provenance)
(Requirement)-[:HAS_MENTION]->(Mention)
(Mention)-[:DENOTES]->(Concept|Predicate|Qualifier)
(Requirement)-[:ASSERTS]->(Assertion)
(Assertion)-[:HAS_SUBJECT]->(Concept)
(Assertion)-[:HAS_PREDICATE]->(Predicate)
(Assertion)-[:HAS_OBJECT]->(Concept)
(Assertion)-[:HAS_QUALIFIER]->(Qualifier)
```

Qualifiers are not part of `Assertion` identity. Two requirements with the same
subject concept, predicate, and object concept point at the same `Assertion`
even when they add different qualifiers.

## Translation

| Domain/extraction source term | Graph language |
|---|---|
| `Requirement` aggregate | `Requirement` source node |
| `Provenance` value | `Provenance` node |
| `RequirementElement` | `Mention` node |
| `Action` semantics | `Assertion` node |
| `actionText` | `Predicate` node |
| `Subject` / `TargetObject` | `Concept` node |
| `Condition` / `Constraint` | `Qualifier` node |

## Lookup Mapping

Resolution uses exact stripped text in v1. It does not lowercase or casefold.

| Selected term type | Lookup label | Candidate node type |
|---|---|---|
| `SUBJECT` | `Concept` | `CONCEPT` |
| `OBJECT` | `Concept` | `CONCEPT` |
| `ACTION` | `Predicate` | `PREDICATE` |
| `CONDITION` | `Qualifier` with `qualifierKind = CONDITION` | `QUALIFIER` |
| `CONSTRAINT` | `Qualifier` with `qualifierKind = CONSTRAINT` | `QUALIFIER` |
