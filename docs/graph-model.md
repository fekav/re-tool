# Neo4j Graph Model

## Purpose

The graph model separates requirement-domain language from generic graph
language. `Requirement` remains the source of a statement. Extracted domain
syntax is persisted as mentions and canonical semantic nodes, not as extraction
DTO or domain entity nodes. Classification is stored on the `Requirement` as
closed vocabulary codes, while the vocabulary nodes document and validate the
allowed values and requirement-to-requirement relation names.

## Labels

| Label | Meaning | Identity |
|---|---|---|
| `Requirement` | Normalized raw requirement text, classification type/property, and source of assertions | `id` |
| `Provenance` | Source, original text (excerpt) and ingestion metadata for a requirement | `id` |
| `Mention` | Concrete text span or phrase found in a requirement | `id` |
| `Concept` | Reusable domain object used as assertion subject or object | `canonicalName.strip()` |
| `Predicate` | Reusable relation or action term | `canonicalName.strip()` |
| `Qualifier` | Condition or constraint enriching an assertion | `(qualifierKind, canonicalText.strip())` |
| `Assertion` | Canonical subject-predicate-object fact | deterministic `assertionKey` from subject concept, predicate, and object concept |
| `RequirementType` | Allowed classification concept types | `code` |
| `RequirementProperty` | Allowed classification properties | `code` |
| `RequirementRelationType` | Allowed directed relation names between requirements | `code` |
| `AllowedRequirementRelation` | Curated valid source-type, relation-type, and target-type triples | `(sourceTypeCode, relationTypeCode, targetTypeCode)` |

`qualifierKind` is `CONDITION` or `CONSTRAINT`.
`Requirement.type` uses a `RequirementType.code`.
`Requirement.property` uses a `RequirementProperty.code`.

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
(Requirement)-[:SATISFIES]->(Requirement)
(Requirement)-[:REFINES]->(Requirement)
(Requirement)-[:DEPENDS_ON]->(Requirement)
(Requirement)-[:CONFLICTS_WITH]->(Requirement)
```

Qualifiers are not part of `Assertion` identity. Two requirements with the same
subject concept, predicate, and object concept point at the same `Assertion`
even when they add different qualifiers.

Requirement relation edges are directed. Their allowed type combinations are
seeded as `AllowedRequirementRelation` nodes.

| Source `Requirement.type` | Relationship | Target `Requirement.type` | Meaning |
|---|---|---|---|
| `NEED` | `SATISFIES` | `GOAL` | The need contributes to satisfying the goal. |
| `REQUIREMENT` | `REFINES` | `NEED` | The requirement makes a need concrete and verifiable. |
| `REQUIREMENT` | `DEPENDS_ON` | `REQUIREMENT` | The source requirement relies on the target requirement. |
| `REQUIREMENT` | `CONFLICTS_WITH` | `REQUIREMENT` | The requirements are in tension or cannot both hold as stated. |

## Classification Semantics

`CLASSIFY_REQUIREMENT` classifies raw text on two axes: concept type and
property. The concept type records intent and commitment level and is represented
by `RequirementType` in source code. The property records whether the classified
concept is primarily behavioral or quality-related and is represented by
`RequirementProperty`.

Low confidence is a consumer review and triage signal, not a classifier failure.
The workflow still returns the best-fit `GOAL`, `NEED`, or `REQUIREMENT`
classification because it does not define an `UNKNOWN` concept type.

| Concept Type | Intent / Commitment Level | Classifier Signal | Example |
|---|---|---|---|
| `GOAL` | Desired outcome or business objective. Explains why change matters. | Outcome language, benefit, target state, or strategic result. Usually not directly testable as one system behavior. | "Reduce failed customer onboarding by 30%." |
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

`confidenceScore` and `rationale` are classification evidence. They do not
participate in graph identity and are not requirement relation vocabulary.

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
| `Classification.conceptType` | `Requirement.type` scalar using `RequirementType` vocabulary |
| `Classification.property` | `Requirement.property` scalar using `RequirementProperty` vocabulary |
| Requirement relation | Directed relationship between `Requirement` nodes, validated against `AllowedRequirementRelation` |

## Lookup Mapping

Resolution uses exact stripped text in v1. It does not lowercase or casefold.

| Selected term type | Lookup label | Candidate node type |
|---|---|---|
| `SUBJECT` | `Concept` | `CONCEPT` |
| `OBJECT` | `Concept` | `CONCEPT` |
| `ACTION` | `Predicate` | `PREDICATE` |
| `CONDITION` | `Qualifier` with `qualifierKind = CONDITION` | `QUALIFIER` |
| `CONSTRAINT` | `Qualifier` with `qualifierKind = CONSTRAINT` | `QUALIFIER` |
