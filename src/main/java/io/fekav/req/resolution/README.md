# Resolution Slice

The Resolution slice connects extracted requirement elements to existing or new
Knowledge Graph nodes. It turns a selected requirement element into either a
node match decision or a human review request.

## Domain Responsibility

Resolution receives one `RequirementElement` at a time. The element type
determines the compatible graph node type:

- `SUBJECT` and `OBJECT` resolve to `CONCEPT`.
- `ACTION` resolves to `PREDICATE`.
- `CONDITION` and `CONSTRAINT` resolve to `QUALIFIER`.

The slice has two phases:

- Candidate retrieval: find compatible graph candidates and attach evidence.
- Candidate matching: decide whether to map automatically, create a new node,
  or request review.

Relevant shared concepts:

- `src/main/java/io/fekav/req/shared/model/RequirementElement.java`
- `src/main/java/io/fekav/req/shared/model/RequirementElementType.java`
- `src/main/java/io/fekav/req/shared/model/CandidateNode.java`
- `src/main/java/io/fekav/req/shared/model/RetrievedCandidateNode.java`
- `src/main/java/io/fekav/req/shared/model/RetrievalEvidence.java`

## Candidate Retrieval

The configured retrieval policy combines exact node-name lookup and token
overlap lookup.

`NodeNameRetrievalPolicy` performs exact lookup and assigns full evidence score
`1.0` with policy name `nodeName`.

`TokenOverlapNodeRetrievalPolicy` searches compatible candidates by token
overlap. It:

- ignores `ACTION` elements because predicate token overlap is currently not
  used,
- tokenizes case-insensitively on non-letter and non-number separators,
- requires a minimum score of `0.5`,
- caps overlap scores at `0.99` so token overlap cannot equal exact lookup,
- sorts by score, label, and candidate key.

`EvidenceBasedNodeRetrievalPolicy` runs configured policies in order and
deduplicates candidates by candidate key, keeping the first evidence source.

Relevant classes:

- `src/main/java/io/fekav/req/resolution/domain/NodeNameRetrievalPolicy.java`
- `src/main/java/io/fekav/req/resolution/domain/TokenOverlapNodeRetrievalPolicy.java`
- `src/main/java/io/fekav/req/resolution/domain/EvidenceBasedNodeRetrievalPolicy.java`
- `src/main/java/io/fekav/req/resolution/infrastructure/Neo4jNodeNameLookup.java`

## Matching Decisions

`ThresholdNodeMatchingPolicy` applies the business decision policy:

- no candidates: `AUTO_CREATE_NEW`,
- one unique top candidate at or above threshold: `AUTO_MAP_EXISTING`,
- multiple top candidates at threshold: review required,
- top score below threshold: review required.

The default auto-map threshold is `1.0`. This means only exact, unique top
matches are automatically mapped by default.

Relevant classes:

- `src/main/java/io/fekav/req/resolution/domain/ThresholdNodeMatchingPolicy.java`
- `src/main/java/io/fekav/req/resolution/domain/NodeMatchingResult.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchDecision.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchDecisionStatus.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchReviewRequest.java`

## Processing

`ResolveNodeCommandHandler` receives a `RequirementElement`, retrieves
candidates, evaluates the match, and publishes one of two events:

- `NodeResolutionDecidedEvent` for automatic decisions,
- `NodeResolutionReviewRequiredEvent` when human review is needed.

Relevant classes:

- `src/main/java/io/fekav/req/resolution/application/ResolveNodeCommand.java`
- `src/main/java/io/fekav/req/resolution/application/ResolveNodeCommandHandler.java`
- `src/main/java/io/fekav/req/shared/event/NodeResolutionDecidedEvent.java`
- `src/main/java/io/fekav/req/shared/event/NodeResolutionReviewRequiredEvent.java`

## Scope Boundary

Resolution does not collect elements from an extracted action and does not close
reviews. Orchestration decides which elements must be resolved. Review handles
human decisions. Graph Change turns final decisions into persisted graph
changes.

## Tests

The behavior is covered by:

- `src/test/java/io/fekav/req/resolution/domain/*Test.java`
- `src/test/java/io/fekav/req/resolution/application/ResolveNodeCommandHandlerTest.java`
- `src/test/java/io/fekav/req/resolution/infrastructure/Neo4jNodeNameLookupTest.java`
- `src/test/java/io/fekav/platform/api/ResolveNodeCommandRestControllerTestIT.java`

The tests verify exact and compatible Neo4j lookup, token scoring, threshold
decisions, deduplication, event publishing, and public API serialization.

## Technical Interface

Command endpoint:

```http
POST /app/c
```

Request body:

```json
{
  "command": "ResolveNodeCommand",
  "payload": {
    "requirementElement": {
      "type": "SUBJECT",
      "text": "billing service"
    }
  }
}
```

Published event and command response, if an existing node is mapped
automatically:

- `NodeResolutionDecidedEvent`

Response shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "decision": {
    "requirementElement": {
      "type": "SUBJECT",
      "text": "billing service"
    },
    "status": "AUTO_MAP_EXISTING",
    "candidates": [
      {
        "candidate": {
          "candidateKey": "billing service",
          "label": "Billing Service",
          "nodeType": "CONCEPT"
        },
        "evidence": [
          {
            "policyName": "nodeName",
            "evidenceText": "Matched node name 'billing service' to graph candidate 'Billing Service'",
            "score": 1.0
          }
        ]
      }
    ],
    "rationale": "Unique top candidate reached auto-map threshold 1.0 with score 1.0."
  }
}
```

Published event and command response, if no candidates exist and a new node is
created automatically:

- `NodeResolutionDecidedEvent`

Response shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "decision": {
    "requirementElement": {
      "type": "SUBJECT",
      "text": "billing service"
    },
    "status": "AUTO_CREATE_NEW",
    "candidates": [],
    "rationale": "No existing candidates found; auto-creating node from selected term."
  }
}
```

Published event and command response, if human review is needed:

- `NodeResolutionReviewRequiredEvent`

Response shape:

```json
{
  "eventId": { "value": "..." },
  "occurredAt": "2026-07-07T10:15:30Z",
  "correlationId": { "value": "..." },
  "reviewRequest": {
    "requirementElement": {
      "type": "SUBJECT",
      "text": "notification component"
    },
    "candidates": [
      {
        "candidate": {
          "candidateKey": "notification service",
          "label": "notification service",
          "nodeType": "CONCEPT"
        },
        "evidence": [
          {
            "policyName": "tokenOverlap",
            "evidenceText": "Matched selected term 'notification component' to graph candidate 'notification service' by token overlap",
            "score": 0.5
          }
        ]
      }
    ],
    "rationale": "Top candidate score 0.5 is below auto-map threshold 1.0; human review is required before mapping."
  }
}
```

Review is returned for two policy outcomes:

- top candidate score is below the auto-map threshold,
- multiple top candidates share a score at the auto-map threshold.
