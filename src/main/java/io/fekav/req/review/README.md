# Review Slice

The Review slice manages human decisions for node matches that Resolution could
not decide automatically. It keeps pending review requests queryable and turns a
reviewer's choice into a final node resolution decision.

## Domain Responsibility

Review starts when Resolution publishes `NodeResolutionReviewRequiredEvent`.
The event contains a `NodeMatchReviewRequest` with:

- the requirement element that needs a decision,
- candidate graph nodes,
- the rationale for why review is required.

The slice exposes pending reviews and accepts two human decisions:

- `MAP_EXISTING`: map the requirement element to one of the proposed
  candidates.
- `CREATE_NEW`: create a new graph node from the requirement element.

Relevant shared classes:

- `src/main/java/io/fekav/req/shared/model/NodeMatchReviewRequest.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchDecision.java`
- `src/main/java/io/fekav/req/shared/model/NodeMatchDecisionStatus.java`

## Review Identity

`NodeMatchReviewId` is derived from the workflow `CorrelationId`, the
requirement element type, and a short SHA-256 hash of the element text. This
makes repeated review-required events for the same workflow element collapse
into one pending review.

Relevant class:

- `src/main/java/io/fekav/req/review/domain/NodeMatchReviewId.java`

## Projection

`NodeMatchReviewProjection` stores review state in memory:

- `NodeResolutionReviewRequiredEvent` opens a pending review if it does not
  already exist.
- `NodeResolutionDecidedEvent` closes pending reviews for the decided
  requirement element.
- pending review lists are deterministic and keep the earliest open review per
  requirement element.

Relevant classes:

- `src/main/java/io/fekav/req/review/application/NodeMatchReviewProjection.java`
- `src/main/java/io/fekav/req/review/domain/PendingNodeMatchReview.java`
- `src/main/java/io/fekav/req/review/application/ListPendingNodeMatchReviewsQuery.java`
- `src/main/java/io/fekav/req/review/application/ListPendingNodeMatchReviewsQueryHandler.java`

## Review Decisions

`SubmitNodeMatchReviewDecisionCommandHandler` validates the decision against
the pending review:

- `MAP_EXISTING` requires a candidate key and the key must belong to the
  pending review.
- `CREATE_NEW` must not include a candidate key.
- unsupported decision values are rejected.
- already closed or unknown reviews are rejected.

Successful review decisions publish `NodeResolutionDecidedEvent` with either
`REVIEW_MAP_EXISTING` or `REVIEW_CREATE_NEW`.

Relevant classes:

- `src/main/java/io/fekav/req/review/application/SubmitNodeMatchReviewDecisionCommand.java`
- `src/main/java/io/fekav/req/review/application/SubmitNodeMatchReviewDecisionCommandHandler.java`
- `src/main/java/io/fekav/req/shared/event/NodeResolutionDecidedEvent.java`

## Scope Boundary

Review does not retrieve candidates and does not decide automatically. It also
does not persist graph changes. It only manages unresolved node decisions until
a reviewer turns them into final decisions.

## Tests

The behavior is covered by:

- `src/test/java/io/fekav/req/review/application/*Test.java`
- `src/test/java/io/fekav/platform/api/ListPendingNodeMatchReviewsQueryRestControllerTestIT.java`
- `src/test/java/io/fekav/platform/api/SubmitNodeMatchReviewDecisionCommandRestControllerTestIT.java`

The tests verify review projection behavior, deterministic pending review
lists, decision validation, closing reviews, publication failure behavior, and
API responses.

## Technical Interface

Query endpoint:

```http
POST /app/q
```

Request body:

```json
{
  "query": "ListPendingNodeMatchReviewsQuery",
  "payload": {}
}
```

Query response:

```json
{
  "reviews": [
    {
      "reviewId": "...::SUBJECT::...",
      "correlationId": { "value": "..." },
      "requirementElement": {
        "type": "SUBJECT",
        "text": "checkout service"
      },
      "candidates": [
        {
          "candidate": {
            "candidateKey": "checkout-service",
            "label": "Checkout Service",
            "nodeType": "CONCEPT"
          },
          "evidence": [
            {
              "policyName": "nodeName",
              "evidenceText": "matched node name",
              "score": 0.82
            }
          ]
        }
      ],
      "rationale": "Candidate needs review before mapping",
      "requestedAt": "2026-07-07T10:15:30Z"
    }
  ]
}
```

Command endpoint:

```http
POST /app/c
```

Map-existing request body:

```json
{
  "command": "SubmitNodeMatchReviewDecisionCommand",
  "payload": {
    "reviewId": "...::SUBJECT::...",
    "decision": "MAP_EXISTING",
    "candidateKey": "checkout-service",
    "rationale": "Domain reviewer selected this candidate."
  }
}
```

Create-new request body:

```json
{
  "command": "SubmitNodeMatchReviewDecisionCommand",
  "payload": {
    "reviewId": "...::SUBJECT::...",
    "decision": "CREATE_NEW",
    "candidateKey": null,
    "rationale": "Domain reviewer requested a new concept."
  }
}
```

Consumed events:

- `NodeResolutionReviewRequiredEvent`
- `NodeResolutionDecidedEvent`

Published event and command response for `MAP_EXISTING`:

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
      "text": "checkout service"
    },
    "status": "REVIEW_MAP_EXISTING",
    "candidates": [
      {
        "candidate": {
          "candidateKey": "checkout-service",
          "label": "Checkout Service",
          "nodeType": "CONCEPT"
        },
        "evidence": [
          {
            "policyName": "nodeName",
            "evidenceText": "matched node name",
            "score": 0.82
          }
        ]
      }
    ],
    "rationale": "Domain reviewer selected this candidate."
  }
}
```

Published event and command response for `CREATE_NEW`:

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
      "text": "checkout service"
    },
    "status": "REVIEW_CREATE_NEW",
    "candidates": [],
    "rationale": "Domain reviewer requested a new concept."
  }
}
```
