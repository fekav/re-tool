# Shared Requirement Model

`req/shared` contains cross-slice business messages and value objects. It is not
a vertical slice. It defines the vocabulary that lets Ingestion,
Classification, Extraction, Resolution, Review, Orchestration, and Graph Change
exchange domain facts without depending on each other's internals.

## Application Events

The shared events define the workflow language:

- `RequirementIngestedEvent`: original requirement text and provenance were
  recorded.
- `RequirementClassifiedEvent`: raw text received a business classification.
- `RequirementElementsExtractedEvent`: raw text was decomposed into an action
  with subject, predicate, object, conditions, and constraints.
- `NodeResolutionDecidedEvent`: one requirement element received a final node
  match decision.
- `NodeResolutionReviewRequiredEvent`: one requirement element needs human node
  match review.
- `RequirementAnalysisCompletedEvent`: the requirement has provenance,
  classification, extracted action, and all node match decisions.
- `RequirementKnownEvent`: the completed assertion was already present in the
  graph.

Relevant package:

- `src/main/java/io/fekav/req/shared/event`

The tests assert that these are `ApplicationEvent`s, not domain events, and
that they carry only the intended business payload.

## Requirement Text and Provenance

The text model separates different stages:

- `OriginalText`: the original source text stored with provenance.
- `RawText`: normalized text passed to analysis slices.
- `Provenance`: source id, original text, source metadata, and ingestion time.
- `SourceMetadata` and `SourceName`: where the requirement came from.

Relevant classes:

- `src/main/java/io/fekav/req/shared/model/OriginalText.java`
- `src/main/java/io/fekav/req/shared/model/RawText.java`
- `src/main/java/io/fekav/req/shared/model/Provenance.java`
- `src/main/java/io/fekav/req/shared/model/SourceMetadata.java`
- `src/main/java/io/fekav/req/shared/model/SourceName.java`

## Requirement Elements

`RequirementElementType` defines the shared extraction and resolution
vocabulary:

- `SUBJECT`
- `ACTION`
- `OBJECT`
- `CONDITION`
- `CONSTRAINT`

`RequirementElement` stores one selected text fragment with one of these types.
It is the input unit for node resolution.

Relevant classes:

- `src/main/java/io/fekav/req/shared/model/RequirementElement.java`
- `src/main/java/io/fekav/req/shared/model/RequirementElementType.java`

## Candidate Nodes and Evidence

Resolution candidates are represented independently from Neo4j internals:

- `NodeType`: `CONCEPT`, `PREDICATE`, or `QUALIFIER`.
- `CandidateNode`: candidate key, label, and node type.
- `RetrievedCandidateNode`: candidate plus retrieval evidence.
- `RetrievalEvidence`: policy name, explanation, and score.
- `CandidateNodeMatch`: selected requirement element plus candidates.

Relevant classes:

- `src/main/java/io/fekav/req/shared/model/NodeType.java`
- `src/main/java/io/fekav/req/shared/model/CandidateNode.java`
- `src/main/java/io/fekav/req/shared/model/RetrievedCandidateNode.java`
- `src/main/java/io/fekav/req/shared/model/RetrievalEvidence.java`
- `src/main/java/io/fekav/req/shared/model/CandidateNodeMatch.java`

## Node Decisions and Reviews

`NodeMatchDecisionStatus` defines the four final decision states:

- `AUTO_MAP_EXISTING`: automatic mapping to exactly one existing candidate.
- `AUTO_CREATE_NEW`: automatic creation of a new node; no candidates allowed.
- `REVIEW_MAP_EXISTING`: reviewer maps to exactly one existing candidate.
- `REVIEW_CREATE_NEW`: reviewer requests a new node; no candidates allowed.

`NodeMatchDecision` enforces the payload shape for these states. Review
requests must contain at least one candidate and a non-blank rationale.

Relevant classes:

- `src/main/java/io/fekav/req/shared/model/NodeMatchDecision.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchDecisionStatus.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchReviewRequest.java`

## Graph References

`GraphNodeReference` is the graph-facing reference used after resolution:

- node type,
- stable key,
- business label.

It is consumed by Graph Change to build assertion identities and qualifiers.

Relevant class:

- `src/main/java/io/fekav/req/shared/model/GraphNodeReference.java`

## Tests

The shared model is covered by:

- `src/test/java/io/fekav/req/shared/event/*Test.java`
- `src/test/java/io/fekav/req/shared/model/*Test.java`
- `src/test/java/io/fekav/req/shared/kg/Neo4jSchemaInitializerTest.java`

The tests verify trimming, null and blank validation, immutable collection
copies, event payload shape, decision payload rules, review request rules, and
the supported v1 requirement element vocabulary.

## Technical Interface

This package has no direct REST command or query. It provides the shared JSON
payload types used by the REST-facing slices and workflow events.

Shared event payloads:

- `RequirementIngestedEvent`
- `RequirementClassifiedEvent`
- `RequirementElementsExtractedEvent`
- `NodeResolutionDecidedEvent`
- `NodeResolutionReviewRequiredEvent`
- `RequirementAnalysisCompletedEvent`
- `RequirementKnownEvent`

Shared model payloads:

- `RawText`
- `OriginalText`
- `Provenance`
- `RequirementElement`
- `CandidateNode`
- `RetrievedCandidateNode`
- `RetrievalEvidence`
- `CandidateNodeMatch`
- `NodeMatchDecision`
- `NodeMatchReviewRequest`
- `GraphNodeReference`

Shared enums used in JSON:

- `RequirementElementType`: `SUBJECT`, `ACTION`, `OBJECT`, `CONDITION`,
  `CONSTRAINT`
- `NodeType`: `CONCEPT`, `PREDICATE`, `QUALIFIER`
- `NodeMatchDecisionStatus`: `AUTO_MAP_EXISTING`, `AUTO_CREATE_NEW`,
  `REVIEW_MAP_EXISTING`, `REVIEW_CREATE_NEW`
