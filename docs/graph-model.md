# Graph Model

## Intent

The requirement knowledge graph uses a lightweight ontology, not a full OWL/RDF
reasoning layer. The ontology defines the vocabulary the app is allowed to use;
the instance graph stores real requirements and their derived artifacts. Neo4j
constraints protect structural integrity, while the Java domain/application
layer validates semantic rules that Neo4j constraints cannot express.

There is no generic `:Concept` label in the graph model. Retrieval and
persistence must target explicit project vocabulary such as `RequirementType`,
`RequirementProperty`, `SyntaxRole`, and later configured domain concept labels
such as `SystemComponent` or `UIComponent`.

## Ontology Vocabulary

The first ontology is intentionally small:

| Vocabulary | Neo4j shape | Initial values | Purpose |
|---|---|---|---|
| Requirement type | `(:RequirementType {code, label})` | `GOAL`, `NEED`, `REQUIREMENT` | Classifies the intent and commitment level of a `Requirement`. |
| Requirement property | `(:RequirementProperty {code, label})` | `FUNCTIONAL`, `QUALITY` | Defines allowed values for the cross-cutting requirement property. |
| Syntax role | `(:SyntaxRole {code, label})` | `SUBJECT`, `ACTION`, `OBJECT`, `CONDITION`, `CONSTRAINT` | Defines allowed roles for extracted `SyntaxElement` values. |
| Requirement relation type | `(:RequirementRelationType {code, label})` | `REFINES`, `SATISFIES`, `CONFLICTS_WITH`, `DEPENDS_ON` | Defines allowed relation names between requirements. |
| Allowed requirement relation | `(:AllowedRequirementRelation {sourceTypeCode, relationTypeCode, targetTypeCode})` | curated triples | Defines which requirement relation combinations are meaningful. |

`GOAL`, `NEED`, and `REQUIREMENT` are project vocabulary values, not separate
aggregate roots. `REQUIREMENT` is the current project code for a binding
system/product requirement; if the vocabulary later changes to
`SYSTEM_REQUIREMENT`, code, docs, and seeded ontology data should be renamed
together. A real goal or need is still persisted as a `Requirement` instance
with a `type` classification.

## Instance Model

The aggregate root is `(:Requirement)`. It owns the raw text boundary and may
gain classification, syntax extraction, mappings, and graph relations as the
workflow progresses.

```text
(:Requirement {
  id,
  rawText,
  type: "GOAL" | "NEED" | "REQUIREMENT",
  property: "FUNCTIONAL" | "QUALITY"
})

(:Requirement)-[:HAS_PROVENANCE]->(:Provenance)
(:Requirement)-[:HAS_SYNTAX_ELEMENT]->(:SyntaxElement {id, role, text})
(:SyntaxElement)-[:MAPS_TO]->(configured domain concept node)
(:Requirement)-[:REFINES|SATISFIES|CONFLICTS_WITH|DEPENDS_ON]->(:Requirement)
```

`Requirement.property` is a Neo4j scalar property because `FUNCTIONAL` and
`QUALITY` are currently simple classification values. The corresponding
`RequirementProperty` ontology nodes still exist to seed, document, and validate
the allowed values.

## Neo4j Constraints

The dev environment uses `neo4j:5-community`, so the baseline schema should use
constraint features available there: uniqueness constraints and supporting
indexes.

```cypher
CREATE CONSTRAINT requirement_id IF NOT EXISTS
FOR (r:Requirement) REQUIRE r.id IS UNIQUE;

CREATE CONSTRAINT provenance_id IF NOT EXISTS
FOR (p:Provenance) REQUIRE p.id IS UNIQUE;

CREATE CONSTRAINT syntax_element_id IF NOT EXISTS
FOR (e:SyntaxElement) REQUIRE e.id IS UNIQUE;

CREATE CONSTRAINT requirement_type_code IF NOT EXISTS
FOR (t:RequirementType) REQUIRE t.code IS UNIQUE;

CREATE CONSTRAINT requirement_type_label IF NOT EXISTS
FOR (t:RequirementType) REQUIRE t.label IS UNIQUE;

CREATE CONSTRAINT requirement_property_code IF NOT EXISTS
FOR (p:RequirementProperty) REQUIRE p.code IS UNIQUE;

CREATE CONSTRAINT requirement_property_label IF NOT EXISTS
FOR (p:RequirementProperty) REQUIRE p.label IS UNIQUE;

CREATE CONSTRAINT syntax_role_code IF NOT EXISTS
FOR (r:SyntaxRole) REQUIRE r.code IS UNIQUE;

CREATE CONSTRAINT syntax_role_label IF NOT EXISTS
FOR (r:SyntaxRole) REQUIRE r.label IS UNIQUE;

CREATE CONSTRAINT requirement_relation_type_code IF NOT EXISTS
FOR (t:RequirementRelationType) REQUIRE t.code IS UNIQUE;

CREATE CONSTRAINT requirement_relation_type_label IF NOT EXISTS
FOR (t:RequirementRelationType) REQUIRE t.label IS UNIQUE;

CREATE CONSTRAINT allowed_requirement_relation_key IF NOT EXISTS
FOR (a:AllowedRequirementRelation)
REQUIRE (a.sourceTypeCode, a.relationTypeCode, a.targetTypeCode) IS UNIQUE;

CREATE INDEX requirement_type IF NOT EXISTS
FOR (r:Requirement) ON (r.type);

CREATE INDEX requirement_property IF NOT EXISTS
FOR (r:Requirement) ON (r.property);

CREATE INDEX requirement_raw_text IF NOT EXISTS
FOR (r:Requirement) ON (r.rawText);

CREATE INDEX syntax_element_text IF NOT EXISTS
FOR (e:SyntaxElement) ON (e.text);
```

If the deployment uses Neo4j Enterprise, the schema can be strengthened with
property existence, property type, and node key constraints. Those constraints
should replace or extend the baseline where they do not conflict, for example:

```cypher
CREATE CONSTRAINT requirement_id_key IF NOT EXISTS
FOR (r:Requirement) REQUIRE r.id IS NODE KEY;

CREATE CONSTRAINT requirement_raw_text_required IF NOT EXISTS
FOR (r:Requirement) REQUIRE r.rawText IS NOT NULL;

CREATE CONSTRAINT requirement_type_property_type IF NOT EXISTS
FOR (r:Requirement) REQUIRE r.type IS :: STRING;

CREATE CONSTRAINT requirement_property_property_type IF NOT EXISTS
FOR (r:Requirement) REQUIRE r.property IS :: STRING;
```

Neo4j constraints do not replace the ontology. They prevent duplicate identity
values, missing required properties where supported, and wrong property types
where supported. They do not prove that a requirement relation is semantically
meaningful.

## App-Side Semantic Validation

Before `PERSIST_GRAPH_CHANGES`, the Java domain/application layer must validate
the write set against the ontology vocabulary.

This split gives the graph meaningful structure without depending on a reasoner:
Neo4j rejects structurally invalid writes, and the app rejects meaningless domain
combinations before they reach the database.
