# Plan: Chainlit Tool Calling für
  Human Review

  ## Summary

  Wir bauen den ersten Review-MVP mit Chainlit als Tool-Calling-UI und Quarkus als deterministische
  Tool-API. Chainlit nutzt Ollama /api/chat Tool Calling, führt Tools aber als HTTP-Aufrufe gegen
  Quarkus aus. Quarkus bekommt dafür einen generischen Query-Endpunkt /app/q, eine in-memory Review-
  Queue, eine Pending-Review-Query und einen Review-Decision-Command.

  ## Key Changes

  - Generic Query API
      - Ergänze /app/q analog zu /app/c.
      - Request-Shape:

        { "query": "ListPendingNodeMatchReviewsQuery", "payload": {} }

      - Response: 200 OK mit Query-Ergebnis.
      - QueryHandlerRegistry bekommt queryType(String queryName) analog zu
        CommandHandlerRegistry.commandType.

  - Review Slice
      - Neuer Slice req.review mit in-memory NodeMatchReviewStore.
      - Store öffnet pending Reviews aus NodeResolutionReviewRequiredEvent.
      - Store schließt Reviews nach passendem NodeResolutionDecidedEvent.
      - reviewId ist tool-facing und stabil innerhalb des Prozesses: aus correlationId +
        requirementElement.type + hash(requirementElement.text) abgeleitet.

  - Review Query
      - ListPendingNodeMatchReviewsQuery liefert:

        {
          "reviews": [
            {
              "reviewId": "...",
              "correlationId": { "value": "..." },
              "requirementElement": { "type": "SUBJECT", "text": "checkout service" },
              "candidates": [],
              "rationale": "...",
              "requestedAt": "..."
            }
          ]
        }

  - Review Decision Command
      - SubmitNodeMatchReviewDecisionCommand über /app/c.
      - Payload:

        {
          "reviewId": "...",
          "decision": "MAP_EXISTING",
          "candidateKey": "checkout-service",
          "rationale": "Domain reviewer selected this candidate."
        }

      - Für CREATE_NEW ist candidateKey leer/fehlend.
      - Handler validiert: Review existiert, ist offen, Kandidat gehört zum Review, Payload passt
        zur Entscheidung.

      - Handler publisht NodeResolutionDecidedEvent; bestehender Orchestrator kann danach
        RequirementAnalysisCompletedEvent auslösen.

  - Decision Status
      - Ergänze NodeMatchDecisionStatus um manuelle Review-Ausgänge:
          - REVIEW_MAP_EXISTING
          - REVIEW_CREATE_NEW

      - Bestehende AUTO_* bleiben unverändert.
      - Graph-Persistenz behandelt AUTO_MAP_EXISTING und REVIEW_MAP_EXISTING als Mapping auf
        bestehenden Node; AUTO_CREATE_NEW und REVIEW_CREATE_NEW als Neuanlage.

  - Chainlit + Ollama Tool Calling
      - Neuer Chainlit Docker-Service mit Env Vars:
          - QUARKUS_BASE_URL=http://app:8080
          - OLLAMA_BASE_URL=http://ollama:11434
          - OLLAMA_MODEL=granite4.1:8b

      - Chainlit registriert zwei Ollama Tools:
          - list_pending_node_match_reviews
          - submit_node_match_review_decision

      - Tool Loop: maximal ein Tool-Call pro User-Turn plus finale Antwort.
      - Ollama Tool Calling nutzt /api/chat mit tools, wie in den offiziellen Ollama Docs
        beschrieben: Tool Calling (https://docs.ollama.com/capabilities/tool-calling), API Reference
        (https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-chat-completion).

  ## Implementation Tasks

  1. Add Query Endpoint Foundation
      - Add queryType(String) to QueryHandlerRegistry.
      - Add /app/q to the existing REST controller or a sibling controller under /app.
      - Acceptance: known query dispatches through QueryBus; unknown query and invalid payload
        return 400.

  2. Add Review Read Model
      - Add in-memory NodeMatchReviewStore.
      - Add pending review view/result records.
      - Wire RequirementWorkflowOrchestrator to open reviews on NodeResolutionReviewRequiredEvent.
      - Acceptance: duplicate review-required events do not create duplicate pending reviews.

  3. Add Pending Review Query
      - Add ListPendingNodeMatchReviewsQuery and handler.
      - Acceptance: /app/q returns all open pending reviews in deterministic order.

  4. Add Review Decision Command
      - Add SubmitNodeMatchReviewDecisionCommand and handler.
      - Validate MAP_EXISTING and CREATE_NEW rules against the pending review.
      - Publish NodeResolutionDecidedEvent and close the pending review only after successful
        publish.

      - Acceptance: invalid review ID, invalid candidate key, duplicate decision, and malformed
        payload are rejected.

  5. Update Decision Status Consumers
      - Extend NodeMatchDecisionStatus.
      - Update decision validation and graph-change mapping for review statuses.
      - Acceptance: completed analysis with review decisions persists the same graph shape as
        equivalent auto decisions.

  6. Add Chainlit Service
      - Add Chainlit app, requirements, Dockerfile, and compose service.
      - Implement Ollama chat loop and Quarkus HTTP tool wrappers.
      - Acceptance: user can ask for open reviews and submit a valid decision through Chainlit.

  ## Test Plan

  - Unit tests for NodeMatchReviewStore: open, list, close, duplicate events.
  - Unit tests for SubmitNodeMatchReviewDecisionCommandHandler: map existing, create new, invalid
    candidate, missing review, already closed.

  - Workflow tests: review-required event creates pending review; submitted decision completes
    workflow when it is the last missing decision.

  - API tests:
      - /app/q dispatches query and returns 200.
      - /app/c dispatches review decision command and returns 201.
      - invalid query/command payloads return 400.

  - Graph-change tests: REVIEW_MAP_EXISTING and REVIEW_CREATE_NEW resolve to the expected graph node
    references.

  - Manual smoke test: start Quarkus, Ollama, Chainlit; ingest a requirement that creates review-
    required matching; list and submit review through Chainlit.

  ## Assumptions

  - First version is in-memory only; restart loses pending reviews.
  - No auth, reviewer identity, audit trail beyond rationale, or multi-reviewer locking in this MVP.
  - No Quarkus-side LLM tool loop in this iteration; Tool Calling lives in Chainlit.
  - No direct Chainlit writes to Neo4j or EventStore.
  - Existing /app/c remains the command surface; /app/q becomes the matching query surface.