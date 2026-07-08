# RE-Tool Requirements Knowledge Graph

[Glossary](docs/glossary.md)

RE-Tool turns natural-language requirements into business-readable knowledge
building blocks. The project does not only store requirements as text. It
classifies their intent, decomposes their semantic structure, matches extracted
terms against known graph concepts, and persists the result as a traceable
Requirement Graph.

The central question is:

> What business statement is contained in a requirement, how binding is it, and
> which already known concepts or statements in product knowledge does it refer
> to?

## Goal

Requirements are often written as free language. Goals, stakeholder needs,
binding system requirements, quality expectations, constraints, and context are
frequently phrased in similar sentences. RE-Tool makes this language
structurable without losing the original text.

The project pursues four goals:

1. Classify requirements by their role in requirements engineering.
2. Decompose the core statement of a requirement into subject, action, object,
   and qualifiers.
3. Recognize recurring concepts and statements in the knowledge graph.
4. Turn uncertain mappings into explicit human review decisions.

The result is a graph that contains not only individual requirements, but also
the derived business concepts, predicates, qualifiers, and assertions.

## Model

An incoming requirement is understood on two levels.

The first level is the requirement classification:

- `GOAL`: a desired outcome or business objective.
- `NEED`: a stakeholder need or capability gap.
- `REQUIREMENT`: a binding product or system obligation.

In addition, the requirement property is classified:

- `FUNCTIONAL`: behavior, capability, workflow, or interaction.
- `QUALITY`: a quality attribute or measurable constraint, such as
  performance, security, availability, or compliance.

The second level is the semantic statement of the requirement:

- `SUBJECT`: who or what acts.
- `ACTION`: which action, relationship, or capability is described.
- `OBJECT`: what the action refers to.
- `CONDITION`: under which condition the statement applies.
- `CONSTRAINT`: which limitation or quality expectation applies.

These elements form a business assertion:

```text
Subject -> Action -> Object
```

Conditions and constraints qualify this assertion, but they are not part of its
identity. This allows multiple requirements to share the same core statement
while adding different conditions or constraints.

## Processing Flow

The central workflow starts by ingesting an original requirement text. The text
is enriched with provenance data so that the origin of every later statement
remains traceable.

Two analyses then run:

1. The requirement is classified as `GOAL`, `NEED`, or `REQUIREMENT` and as
   `FUNCTIONAL` or `QUALITY`.
2. The requirement elements are extracted into Subject, Action, Object,
   Conditions, and Constraints.

For each extracted element, RE-Tool searches for matching existing graph nodes.
If no candidate is found, a new node can be created. If one clear candidate has
sufficient evidence, the element can be mapped automatically. If several
candidates are plausible or the evidence is insufficient, RE-Tool creates a
review request.

Only after all element mappings have been decided is the completed analysis
persisted as a graph change.

```text
Requirement Text
  -> Ingestion + Provenance
  -> Classification
  -> Requirement Element Extraction
  -> Node Resolution
  -> Human Review, if required
  -> Requirement Graph Change
```

## Knowledge Graph

The Neo4j graph deliberately separates origin, business statement, and reusable
concepts.

Central node types are:

- `Requirement`: normalized original text including classification.
- `Provenance`: source, original text, and ingestion timestamp.
- `Concept`: reusable business subjects and objects.
- `Predicate`: reusable actions or relationships.
- `Qualifier`: conditions and constraints.
- `Assertion`: canonical Subject-Predicate-Object statement.

A persisted requirement links these elements as follows:

```text
(Requirement)-[:HAS_PROVENANCE]->(Provenance)
(Requirement)-[:ASSERTS]->(Assertion)
(Assertion)-[:HAS_SUBJECT]->(Concept)
(Assertion)-[:HAS_PREDICATE]->(Predicate)
(Assertion)-[:HAS_OBJECT]->(Concept)
(Assertion)-[:HAS_QUALIFIER]->(Qualifier)
```

This turns free text into an inspectable business knowledge model. The graph can
show which requirements use the same concepts, which statements are already
known, and where new terms or new business facts emerge.

## Handling Uncertainty

RE-Tool treats uncertainty as part of the business process.

Low classification confidence is not a technical failure. It is a signal for
review or triage. Node mapping is handled with the same discipline: clear
matches can be mapped automatically, ambiguous matches create an open
Node-Match Review.

A review decision can:

- map an extracted element to an existing candidate.
- create a new graph node for the element.

The graph is updated only after these decisions are complete. This keeps it
traceable which statements were derived automatically and where human
clarification was required.

## Architecture In Brief

The backend is organized as a small hexagonal application with vertical slices.
Business slices live under `req`; technical cross-cutting capabilities live
under `platform`.

Important business slices:

- `ingestion`: ingestion of original text and provenance.
- `classification`: classification as Goal, Need, or Requirement and as
  Functional or Quality.
- `extraction`: requirement element extraction.
- `resolution`: lookup and scoring of matching graph nodes.
- `review`: open Node-Match Reviews and human decisions.
- `orchestration`: event-driven workflow coordination.
- `graphchange`: conversion of completed analyses into Neo4j changes.

LLM outputs are not accepted directly as domain truth. They pass through JSON
schemas, DTOs, and structured validation before they are translated into domain
objects.

## Interfaces

The backend exposes a generic Command/Query API:

- `POST /app/c` executes commands.
- `POST /app/q` executes queries.

Business-relevant commands and queries include:

- `IngestRequirementCommand`
- `ClassifyRequirementCommand`
- `ExtractSyntaxCommand`
- `ResolveNodeCommand`
- `SubmitNodeMatchReviewDecisionCommand`
- `ListPendingNodeMatchReviewsQuery`

There is also a Chainlit UI for interactive work with requirements and open
reviews. It can ingest requirements, list open reviews, and send review
decisions back to the backend.

## Technical Start

### Stack Overview

- Java 25
- Quarkus
- Gradle
- Neo4j
- Ollama
- Chainlit

The recommended local setup is the VS Code devcontainer. It starts the services
from `.devcontainer/docker-compose.yml` and opens the repository in the `app`
container.

- `app`: development container for Java, Gradle, and Quarkus.
- `neo4j`: local graph database.
- `ollama`: local LLM runtime.
- `ollama-init`: one-shot model download and verification for Ollama.
- `chat`: Chainlit UI.

### Addresses

- Quarkus backend: `http://localhost:8080`
- Chainlit UI: `http://localhost:8000`
- Neo4j Browser: `http://localhost:7474`
- Neo4j Bolt: `bolt://localhost:7687`
- Ollama API: `http://localhost:11434`

Neo4j development credentials:

- username: `neo4j`
- password: `devpassword`

Inside the Docker network, services use container hostnames:

- Backend to Neo4j: `bolt://neo4j:7687`
- Backend to Ollama: `http://ollama:11434`
- Chainlit to backend: `http://app:8080`

### Model Configuration

The devcontainer downloads the configured Ollama model automatically during
startup. The model is configured once in `.devcontainer/devcontainer.env`:

```env
OLLAMA_MODEL=granite4.1:8b
```

That value is used by:

- `ollama-init`, which pulls and verifies the model.
- The Quarkus app through `llm.model=${OLLAMA_MODEL:granite4.1:8b}` in
  `src/main/resources/application.properties`.
- The Chainlit UI through its `OLLAMA_MODEL` environment variable.

To try a smaller local model, change the env file, for example:

```env
OLLAMA_MODEL=granite4.1:3b
```

Then rebuild or reopen the devcontainer so the services are recreated and the
new env file is loaded.

### Get Started

Prerequesites: <paste here>

From VS Code, run `Dev Containers: Reopen in Container`.

The first startup may take a while because `ollama-init` downloads the model into
the `ollama-data` Docker volume. Subsequent starts reuse that volume.

Once the devcontainer is open, start the Quarkus backend manually inside the
container:

```bash
./gradlew quarkusDev
```

This will also initialize the graph database. The `app` container intentionally runs `sleep infinity`; this keeps the
development container alive while leaving backend startup under developer
control.

Alternative to VS Code Dev container: from a shell, start the same Compose stack directly:

```bash
docker compose -f .devcontainer/docker-compose.yml up -d --build
docker compose -f .devcontainer/docker-compose.yml exec app bash
```

### Smoke Tests

Check that Ollama has the configured model:

```bash
curl -fsS http://ollama:11434/api/tags | jq '.models[].name'
```

After Quarkus is running, ingest a requirement:

```bash
curl http://localhost:8080/app/c \
  -H 'Content-Type: application/json' \
  -d '{
    "command": "IngestRequirementCommand",
    "payload": {
      "originalText": "The system shall notify the customer when a payment fails."
    }
  }'
```

List open node-match reviews:

```bash
curl http://localhost:8080/app/q \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "ListPendingNodeMatchReviewsQuery",
    "payload": {}
  }'
```

### Chat UI Smoke Test

Open `http://localhost:8000`.

Direct command path:

```text
User:
/ingest The system shall notify the customer when a payment fails.

Assistant:
Requirement ingested...

User:
/reviews

Assistant:
Pending node-match reviews...
```

LLM-assisted path:

```text
User:
Please ingest this requirement: The system shall notify the customer when a payment fails.

Assistant:
Uses the ingest_requirement tool and reports the result.

User:
Show me the pending node match reviews.

Assistant:
Uses the list_pending_node_match_reviews tool and summarizes the open reviews.
```

The direct commands avoid using the LLM for command interpretation. The
LLM-assisted path uses Ollama and the configured `OLLAMA_MODEL`.

### Useful Commands

Run all tests:

```bash
./gradlew test
```

Run the backend in dev mode:

```bash
./gradlew quarkusDev
```

Build the application:

```bash
./gradlew build
```

Inspect Neo4j in the browser:

1. Open `http://localhost:7474`.
2. Connect with username `neo4j` and password `devpassword`.
3. The app seeds ontology and sample requirement data on startup from
   `src/main/java/io/fekav/req/shared/kg/Neo4jSchemaInitializer.java`.
4. Run a query that shows all requirements and their relationships:

```cypher
MATCH (source:Requirement)-[relationship]->(target:Requirement)
RETURN source, relationship, target;
```

Stop the devcontainer services:

```bash
docker compose -f .devcontainer/docker-compose.yml down
```

Stop and remove local service data volumes:

```bash
docker compose -f .devcontainer/docker-compose.yml down -v
```

Useful entry points:

- Backend configuration: `src/main/resources/application.properties`
- Graph model: `docs/graph-model.md`
- Architecture overview: `docs/ARCHITECTURE.md`
- Chainlit UI: `chainlit/app.py`
