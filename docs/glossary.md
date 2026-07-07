# Glossary

This glossary is limited to terms that are present in `src/main/java` as Java
types, enum values, package/slice names, or stable method-level concepts. It
does not describe obsolete workflow-state names or planned terminology that is
not represented in the current code.

## Requirement Core

| Term | Java Reference | Meaning |
|---|---|---|
| `Requirement` | `io.fekav.req.shared.model.Requirement` | Aggregate for raw requirement text and its analysis state. It can receive provenance, extraction results, and classification results. |
| `RequirementId` | `io.fekav.req.shared.model.RequirementId` | UUID-backed identifier for a requirement aggregate. |
| `RawText` | `io.fekav.req.shared.model.RawText` | Validated non-blank text used as the normalized raw requirement input. |
| `OriginalText` | `io.fekav.req.shared.model.OriginalText` | Validated original text preserved in provenance. |
| `Provenance` | `io.fekav.req.shared.model.Provenance` | Traceability value containing original text, source metadata, and ingestion timestamp. |
| `SourceMetadata` | `io.fekav.req.shared.model.SourceMetadata` | Source information for a requirement. The current factory represents API requests. |
| `SourceName` | `io.fekav.req.shared.model.SourceName` | Validated source-name value. |
| `RequirementStatus` | `io.fekav.req.shared.model.RequirementStatus` | Requirement aggregate status: `PENDING`, `INGESTED`, `EXTRACTED`, or `CLASSIFIED`. |

## Ingestion

| Term | Java Reference | Meaning |
|---|---|---|
| `ingestion` | `io.fekav.req.ingestion` | Slice that accepts original requirement text and publishes the first workflow event. |
| `IngestRequirementCommand` | `io.fekav.req.ingestion.application.IngestRequirementCommand` | Command for ingesting original requirement text. |
| `IngestRequirementCommandHandler` | `io.fekav.req.ingestion.application.IngestRequirementCommandHandler` | Command handler that creates provenance, publishes `RequirementIngestedEvent`, and returns an ingestion outcome. |
| `IngestRequirementResult` | `io.fekav.req.ingestion.application.IngestRequirementResult` | Result returned to callers after ingestion processing. |
| `IngestRequirementResult.Status` | `io.fekav.req.ingestion.application.IngestRequirementResult.Status` | Ingestion result status: `RECORDED`, `ALREADY_EXISTS`, `REVIEW_REQUIRED`, or `INTERNAL_ERROR`. |
| `IngestionOutcomeStore` | `io.fekav.req.ingestion.application.IngestionOutcomeStore` | In-memory outcome coordination used by the event-driven ingestion workflow. |

## Classification

| Term | Java Reference | Meaning |
|---|---|---|
| `classification` | `io.fekav.req.classification` | Slice that assigns a requirement type, property, confidence score, and rationale. |
| `Classification` | `io.fekav.req.classification.domain.Classification` | Domain result of classifying a requirement. |
| `ClassificationService` | `io.fekav.req.classification.application.ClassificationService` | Application port for requirement classification. |
| `ClassifyRequirementCommand` | `io.fekav.req.classification.application.ClassifyRequirementCommand` | Command for classifying raw requirement text. |
| `ClassifyRequirementCommandHandler` | `io.fekav.req.classification.application.ClassifyRequirementCommandHandler` | Command handler that calls `ClassificationService`, applies the result to a `Requirement`, and publishes `RequirementClassifiedEvent`. |
| `LlmRequirementClassificationService` | `io.fekav.req.classification.infrastructure.LlmRequirementClassificationService` | LLM-backed classification adapter. |
| `RequirementClassificationOutput` | `io.fekav.req.classification.infrastructure.RequirementClassificationOutput` | Infrastructure DTO for structured model output before mapping to the domain model. |
| `RequirementClassificationFieldsOutput` | `io.fekav.req.classification.infrastructure.RequirementClassificationFieldsOutput` | Nested DTO for classification fields returned by the model. |
| `RequirementType` | `io.fekav.req.classification.domain.RequirementType` | Closed classification type enum: `GOAL`, `NEED`, or `REQUIREMENT`. |
| `RequirementProperty` | `io.fekav.req.classification.domain.RequirementProperty` | Closed classification property enum: `FUNCTIONAL` or `QUALITY`. |
| `ConfidenceScore` | `io.fekav.req.classification.domain.ConfidenceScore` | Numeric classifier confidence from `0.0` to `1.0`. |
| `Rationale` | `io.fekav.req.classification.domain.Rationale` | Non-blank explanation for a classification result. |

## Requirement Element Extraction

| Term | Java Reference | Meaning |
|---|---|---|
| `extraction` | `io.fekav.req.extraction` | Slice that extracts requirement elements from raw text. This is the current package/slice name. |
| `SyntaxExtraction` | `io.fekav.req.extraction.application.SyntaxExtraction` | Application port whose current Java name still contains `Syntax`; it extracts an `Action` from `RawText`. |
| `ExtractSyntaxCommand` | `io.fekav.req.extraction.application.ExtractSyntaxCommand` | Command whose current Java name still contains `Syntax`; it requests requirement element extraction for raw text. |
| `ExtractSyntaxCommandHandler` | `io.fekav.req.extraction.application.ExtractSyntaxCommandHandler` | Command handler that calls `SyntaxExtraction` and publishes `RequirementElementsExtractedEvent`. |
| `LlmSyntaxExtraction` | `io.fekav.req.extraction.infrastructure.LlmSyntaxExtraction` | LLM-backed extraction adapter. |
| `SyntaxExtractionOutput` | `io.fekav.req.extraction.infrastructure.SyntaxExtractionOutput` | Infrastructure DTO for structured model output before mapping to `Action`. |
| `RequirementElementsOutput` | `io.fekav.req.extraction.infrastructure.RequirementElementsOutput` | DTO wrapper for extracted requirement element fields. |
| `Action` | `io.fekav.req.extraction.domain.Action` | Extracted semantic action. It owns action text, subject, target object, conditions, and constraints. |
| `Subject` | `io.fekav.req.extraction.domain.Subject` | Extracted actor or owner of an action. |
| `TargetObject` | `io.fekav.req.extraction.domain.TargetObject` | Extracted object affected by an action. |
| `Condition` | `io.fekav.req.extraction.domain.Condition` | Extracted circumstance under which an action applies. |
| `Constraint` | `io.fekav.req.extraction.domain.Constraint` | Extracted limitation, rule, boundary, or quality-related qualifier. |
| `RequirementElement` | `io.fekav.req.shared.model.RequirementElement` | Shared representation of an extracted element used by node resolution. |
| `RequirementElementType` | `io.fekav.req.shared.model.RequirementElementType` | Extracted element type enum: `SUBJECT`, `ACTION`, `OBJECT`, `CONDITION`, or `CONSTRAINT`. |
| `ElementId` | `io.fekav.req.shared.model.ElementId` | UUID-backed identifier used by requirement-related values such as provenance and extracted elements. |

## Node Resolution

| Term | Java Reference | Meaning |
|---|---|---|
| `resolution` | `io.fekav.req.resolution` | Slice that retrieves candidate graph nodes and decides whether an extracted element can be mapped automatically or needs review. |
| `ResolveNodeCommand` | `io.fekav.req.resolution.application.ResolveNodeCommand` | Command for resolving one `RequirementElement` against graph candidates. |
| `ResolveNodeCommandHandler` | `io.fekav.req.resolution.application.ResolveNodeCommandHandler` | Command handler that retrieves candidates, evaluates the match, and publishes a node-resolution event. |
| `NodeRetrievalService` | `io.fekav.req.resolution.domain.NodeRetrievalService` | Domain service that applies the configured `NodeRetrievalPolicy`. |
| `NodeRetrievalPolicy` | `io.fekav.req.resolution.domain.NodeRetrievalPolicy` | Policy interface for retrieving candidates for one `RequirementElement`. |
| `NodeNameRetrievalPolicy` | `io.fekav.req.resolution.domain.NodeNameRetrievalPolicy` | Retrieval policy that performs exact node-name lookup and emits `nodeName` evidence with score `1.0`. |
| `TokenOverlapNodeRetrievalPolicy` | `io.fekav.req.resolution.domain.TokenOverlapNodeRetrievalPolicy` | Retrieval policy that scores compatible candidates by token overlap. It skips `ACTION` elements. |
| `EvidenceBasedNodeRetrievalPolicy` | `io.fekav.req.resolution.domain.EvidenceBasedNodeRetrievalPolicy` | Composite retrieval policy that combines candidates from multiple retrieval policies by candidate key. |
| `CandidateLookup` | `io.fekav.req.resolution.domain.CandidateLookup` | Port for finding candidates compatible with one `RequirementElement`. |
| `CompatibleCandidateLookup` | `io.fekav.req.resolution.domain.CompatibleCandidateLookup` | Port for broader compatible-candidate lookup used by token-overlap retrieval. |
| `Neo4jNodeNameLookup` | `io.fekav.req.resolution.infrastructure.Neo4jNodeNameLookup` | Neo4j adapter for exact name lookup. It maps element types to graph node labels. |
| `NodeMatchingService` | `io.fekav.req.resolution.domain.NodeMatchingService` | Domain service that applies the configured `NodeMatchingPolicy`. |
| `NodeMatchingPolicy` | `io.fekav.req.resolution.domain.NodeMatchingPolicy` | Policy interface for turning one candidate match into a final decision or review request. |
| `ThresholdNodeMatchingPolicy` | `io.fekav.req.resolution.domain.ThresholdNodeMatchingPolicy` | Matching policy with default auto-map threshold `1.0`. It auto-creates when no candidates exist, auto-maps one candidate at threshold, and requests review for ambiguous or below-threshold matches. |
| `NodeMatchingResult` | `io.fekav.req.resolution.domain.NodeMatchingResult` | Sealed result of node matching: either a decided match or a review-required result. |
| `CandidateNode` | `io.fekav.req.shared.model.CandidateNode` | Graph-node candidate identity containing candidate key, label, and node type. |
| `CandidateNodeMatch` | `io.fekav.req.shared.model.CandidateNodeMatch` | Pairing of one `RequirementElement` with zero or more retrieved candidate nodes. |
| `RetrievedCandidateNode` | `io.fekav.req.shared.model.RetrievedCandidateNode` | Candidate plus retrieval evidence. |
| `RetrievalEvidence` | `io.fekav.req.shared.model.RetrievalEvidence` | Evidence for a retrieved candidate, including policy name, evidence text, and score. |
| `NodeType` | `io.fekav.req.shared.model.NodeType` | Graph candidate node type enum: `CONCEPT`, `PREDICATE`, or `QUALIFIER`. |

## Review

| Term | Java Reference | Meaning |
|---|---|---|
| `review` | `io.fekav.req.review` | Slice for pending node-match reviews and human decisions. |
| `NodeMatchReviewRequest` | `io.fekav.req.shared.model.NodeMatchReviewRequest` | Request for a human decision when node matching is ambiguous or insufficiently supported. |
| `PendingNodeMatchReview` | `io.fekav.req.review.domain.PendingNodeMatchReview` | Stored pending review item. |
| `NodeMatchReviewId` | `io.fekav.req.review.domain.NodeMatchReviewId` | Identifier for a pending node-match review. |
| `NodeMatchReviewProjection` | `io.fekav.req.review.application.NodeMatchReviewProjection` | In-memory projection of pending and closed node-match reviews. |
| `ListPendingNodeMatchReviewsQuery` | `io.fekav.req.review.application.ListPendingNodeMatchReviewsQuery` | Query for listing currently pending node-match reviews. |
| `ListPendingNodeMatchReviewsQueryHandler` | `io.fekav.req.review.application.ListPendingNodeMatchReviewsQueryHandler` | Query handler returning pending review projections. |
| `SubmitNodeMatchReviewDecisionCommand` | `io.fekav.req.review.application.SubmitNodeMatchReviewDecisionCommand` | Command for submitting a human node-match decision. |
| `SubmitNodeMatchReviewDecisionCommandHandler` | `io.fekav.req.review.application.SubmitNodeMatchReviewDecisionCommandHandler` | Command handler that converts a review response into a `NodeMatchDecision` and publishes `NodeResolutionDecidedEvent`. |
| `NodeMatchDecision` | `io.fekav.req.shared.model.NodeMatchDecision` | Final mapping decision for one requirement element. |
| `NodeMatchDecisionStatus` | `io.fekav.req.shared.model.NodeMatchDecisionStatus` | Decision status enum: `AUTO_MAP_EXISTING`, `AUTO_CREATE_NEW`, `REVIEW_MAP_EXISTING`, or `REVIEW_CREATE_NEW`. |

## Orchestration And Events

| Term | Java Reference | Meaning |
|---|---|---|
| `orchestration` | `io.fekav.req.orchestration` | Slice that coordinates the event-driven requirement workflow. |
| `RequirementWorkflowOrchestrator` | `io.fekav.req.orchestration.application.RequirementWorkflowOrchestrator` | Event observer that reacts to requirement and node-resolution events, dispatches follow-up commands, records review requirements, and publishes completion events. |
| `WorkflowState` | `io.fekav.req.orchestration.domain.WorkflowState` | Replayed state of one requirement workflow based on stored application events. |
| `RequirementElementCollector` | `io.fekav.req.orchestration.domain.RequirementElementCollector` | Derives expected `RequirementElement` values from an extracted `Action`. |
| `EventStore` | `io.fekav.req.orchestration.application.EventStore` | Port for storing and loading application events by correlation id. |
| `InMemoryEventStore` | `io.fekav.req.orchestration.infrastructure.InMemoryEventStore` | In-memory event store implementation. |
| `RequirementIngestedEvent` | `io.fekav.req.shared.event.RequirementIngestedEvent` | Application event emitted after requirement ingestion. |
| `RequirementClassifiedEvent` | `io.fekav.req.shared.event.RequirementClassifiedEvent` | Application event emitted after classification. |
| `RequirementElementsExtractedEvent` | `io.fekav.req.shared.event.RequirementElementsExtractedEvent` | Application event emitted after requirement element extraction. |
| `NodeResolutionEvent` | `io.fekav.req.shared.event.NodeResolutionEvent` | Base type for node-resolution events. |
| `NodeResolutionDecidedEvent` | `io.fekav.req.shared.event.NodeResolutionDecidedEvent` | Event emitted when a node-resolution decision is final. |
| `NodeResolutionReviewRequiredEvent` | `io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent` | Event emitted when a node-resolution review is required. |
| `RequirementAnalysisCompletedEvent` | `io.fekav.req.shared.event.RequirementAnalysisCompletedEvent` | Event emitted when provenance, classification, extraction, and all node decisions are complete. |
| `RequirementKnownEvent` | `io.fekav.req.shared.event.RequirementKnownEvent` | Follow-up event for an already-known assertion or requirement graph result. |

## Graph Change

| Term | Java Reference | Meaning |
|---|---|---|
| `graphchange` | `io.fekav.req.graphchange` | Slice that converts a completed requirement analysis into graph persistence. |
| `CompletedAnalysisGraphChangeFactory` | `io.fekav.req.graphchange.application.CompletedAnalysisGraphChangeFactory` | Factory that converts `RequirementAnalysisCompletedEvent` into `PersistRequirementGraphChange`. |
| `PersistRequirementGraphChange` | `io.fekav.req.graphchange.application.PersistRequirementGraphChange` | Application command-like value describing the graph write for a completed analysis. |
| `RequirementGraphChangeService` | `io.fekav.req.graphchange.application.RequirementGraphChangeService` | Event observer that persists graph changes and records ingestion outcomes. |
| `RequirementGraphChangePort` | `io.fekav.req.graphchange.application.RequirementGraphChangePort` | Port for graph persistence. |
| `RequirementGraphChangeResult` | `io.fekav.req.graphchange.application.RequirementGraphChangeResult` | Result of graph persistence, mapped back to ingestion outcomes and follow-up events. |
| `Neo4jRequirementGraphChangeAdapter` | `io.fekav.req.graphchange.infrastructure.Neo4jRequirementGraphChangeAdapter` | Neo4j implementation of `RequirementGraphChangePort`. |
| `AssertionIdentity` | `io.fekav.req.graphchange.domain.AssertionIdentity` | Deterministic identity for a subject-predicate-object assertion. |
| `GraphQualifier` | `io.fekav.req.graphchange.domain.GraphQualifier` | Graph qualifier derived from a condition or constraint node decision. |
| `GraphNodeReference` | `io.fekav.req.shared.model.GraphNodeReference` | Reference to an existing or new graph node by type, key, and label. |
| `InvalidGraphChangeException` | `io.fekav.req.graphchange.domain.InvalidGraphChangeException` | Exception raised when a completed analysis cannot be converted into a valid graph change. |
| `Neo4jSchemaInitializer` | `io.fekav.req.shared.kg.Neo4jSchemaInitializer` | Initializes Neo4j constraints and vocabulary data used by the requirement graph. |

## Platform

| Term | Java Reference | Meaning |
|---|---|---|
| `Command` | `io.fekav.platform.cqrs.Command` | Marker interface for commands dispatched through the command bus. |
| `CommandBus` | `io.fekav.platform.cqrs.CommandBus` | Dispatches commands to registered command handlers. |
| `CommandHandler` | `io.fekav.platform.cqrs.CommandHandler` | Handles a specific command type. |
| `Query` | `io.fekav.platform.cqrs.Query` | Marker interface for queries executed through the query bus. |
| `QueryBus` | `io.fekav.platform.cqrs.QueryBus` | Executes queries through registered query handlers. |
| `QueryHandler` | `io.fekav.platform.cqrs.QueryHandler` | Handles a specific query type. |
| `RestController` | `io.fekav.platform.api.RestController` | Generic REST entrypoint for command and query execution. |
| `LlmClientPort` | `io.fekav.platform.llm.LlmClientPort` | Provider-neutral port for LLM calls. |
| `OllamaClientAdapter` | `io.fekav.platform.adapter.OllamaClientAdapter` | Ollama-backed implementation of `LlmClientPort`. |
| `Prompt` | `io.fekav.platform.llm.Prompt` | Prompt payload sent to an LLM adapter. |
| `PromptFactory` | `io.fekav.platform.llm.PromptFactory` | Creates structured prompt values. |
| `StructuredOutputContract` | `io.fekav.platform.structuredoutput.StructuredOutputContract` | App-owned validation contract for structured model output DTOs. |
| `StructuredOutputValidator` | `io.fekav.platform.structuredoutput.StructuredOutputValidator` | Validator for structured model output DTOs. |
| `StructuredOutputValidationException` | `io.fekav.platform.structuredoutput.StructuredOutputValidationException` | Exception for DTOs that violate a structured-output contract. |
| `InvalidStructuredOutputException` | `io.fekav.platform.structuredoutput.InvalidStructuredOutputException` | Exception for model output that cannot be parsed, validated, or mapped. |
| `ApplicationEvent` | `io.fekav.platform.messaging.ApplicationEvent` | Base contract for application events. |
| `DomainEvent` | `io.fekav.platform.messaging.DomainEvent` | Base contract for domain events. |
| `EventPublisher` | `io.fekav.platform.messaging.EventPublisher` | Publishes events to the application. |
| `CorrelationId` | `io.fekav.platform.messaging.CorrelationId` | Identifier used to correlate events in one workflow. |
| `EventId` | `io.fekav.platform.messaging.EventId` | Unique identifier for an event. |
