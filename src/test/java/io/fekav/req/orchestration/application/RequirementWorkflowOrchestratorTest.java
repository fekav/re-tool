package io.fekav.req.orchestration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.application.ClassifyRequirementCommand;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.ingestion.application.IngestRequirementResult;
import io.fekav.req.ingestion.application.IngestionOutcomeStore;
import io.fekav.req.orchestration.infrastructure.InMemoryEventStore;
import io.fekav.req.review.application.NodeMatchReviewProjection;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.resolution.application.ResolveNodeCommand;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.SourceMetadata;
import io.fekav.req.syntaxextraction.application.ExtractSyntaxCommand;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class RequirementWorkflowOrchestratorTest {

    private static final RawText RAW_TEXT =
        new RawText("The login form must validate credentials before authentication.");

    private final InMemoryEventStore eventStore = new InMemoryEventStore();
    private final RecordingCommandBus commandBus = new RecordingCommandBus();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final IngestionOutcomeStore outcomeStore = new IngestionOutcomeStore();
    private final NodeMatchReviewProjection reviewProjection =
        new NodeMatchReviewProjection();
    private final RequirementWorkflowOrchestrator orchestrator =
        new RequirementWorkflowOrchestrator(
            eventStore,
            commandBus,
            eventPublisher,
            outcomeStore,
            reviewProjection
        );

    @Test
    void ignoresEvents_whenOrchestrationIsDisabled() {
        // Arrange
        RequirementWorkflowOrchestrator disabledOrchestrator =
            new RequirementWorkflowOrchestrator(
                eventStore,
                commandBus,
                eventPublisher,
                outcomeStore,
                reviewProjection,
                false
            );
        RequirementIngestedEvent event = ingestedEvent();

        // Act
        disabledOrchestrator.onRequirementIngested(event);

        // Assert
        assertThat(commandBus.commands()).isEmpty();
        assertThat(eventStore.load(event.correlationId())).isEmpty();
    }

    @Test
    void dispatchesClassificationAndSyntaxExtraction_whenRequirementIsIngested() {
        // Arrange
        RequirementIngestedEvent event = ingestedEvent();

        // Act
        orchestrator.onRequirementIngested(event);

        // Assert
        assertThat(eventStore.load(event.correlationId())).containsExactly(event);
        assertThat(commandBus.commands()).hasSize(2);
        assertThat(commandBus.commands().get(0))
            .isInstanceOfSatisfying(ClassifyRequirementCommand.class, command -> {
                assertThat(command.correlationId()).isEqualTo(event.correlationId());
                assertThat(command.rawText()).isEqualTo(event.provenance().getOriginalText().text());
            });
        assertThat(commandBus.commands().get(1))
            .isInstanceOfSatisfying(ExtractSyntaxCommand.class, command -> {
                assertThat(command.correlationId()).isEqualTo(event.correlationId());
                assertThat(command.rawText()).isEqualTo(event.provenance().getOriginalText().text());
            });
    }

    @Test
    void doesNotDispatchAgain_whenIngestedEventIsRedelivered() {
        // Arrange
        RequirementIngestedEvent event = ingestedEvent();

        // Act
        orchestrator.onRequirementIngested(event);
        orchestrator.onRequirementIngested(event);

        // Assert
        assertThat(commandBus.commands()).hasSize(2);
        assertThat(eventStore.load(event.correlationId())).containsExactly(event);
    }

    @Test
    void dispatchesResolutionForEveryExtractedRequirementElement() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        Action action = actionWithConditionAndConstraint();
        RequirementElementsExtractedEvent event = RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            action
        );

        // Act
        orchestrator.onRequirementElementsExtracted(event);

        // Assert
        assertThat(commandBus.commands()).hasSize(5);
        assertThat(commandBus.commands())
            .allSatisfy(command ->
                assertThat(command).isInstanceOf(ResolveNodeCommand.class)
            )
            .extracting(command ->
                ((ResolveNodeCommand) command).requirementElement()
            )
            .containsExactly(
                new RequirementElement(RequirementElementType.SUBJECT, "login form"),
                new RequirementElement(RequirementElementType.ACTION, "must validate"),
                new RequirementElement(RequirementElementType.OBJECT, "credentials"),
                new RequirementElement(RequirementElementType.CONDITION, "before authentication"),
                new RequirementElement(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
            );
        assertThat(commandBus.commands())
            .extracting(command -> ((ResolveNodeCommand) command).correlationId())
            .containsOnly(correlationId);
    }

    @Test
    void doesNotDispatchResolutionAgain_whenEquivalentExtractionEventIsRedeliveredWithNewEventId() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        Action action = actionWithConditionAndConstraint();

        // Act
        orchestrator.onRequirementElementsExtracted(RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            action
        ));
        orchestrator.onRequirementElementsExtracted(RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            action
        ));

        // Assert
        assertThat(commandBus.commands()).hasSize(5);
    }

    @Test
    void publishesCompletionOnce_whenClassifiedEventCompletesWorkflow() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<NodeMatchDecision> decisions = requiredDecisions();
        eventStore.appendIfAbsent(ingestedEvent);
        eventStore.appendIfAbsent(
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action)
        );
        decisions.forEach(decision ->
            eventStore.appendIfAbsent(
                NodeResolutionDecidedEvent.create(correlationId, decision)
            )
        );
        RequirementClassifiedEvent event =
            RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification());

        // Act
        orchestrator.onRequirementClassified(event);
        orchestrator.onRequirementClassified(event);

        // Assert
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementAnalysisCompletedEvent.class, completion -> {
                assertThat(completion.correlationId()).isEqualTo(correlationId);
                assertThat(completion.provenance()).isEqualTo(ingestedEvent.provenance());
                assertThat(completion.classification()).isEqualTo(event.classification());
                assertThat(completion.action()).isEqualTo(action);
                assertThat(completion.nodeMatchDecisions())
                    .containsExactlyElementsOf(decisions);
            });
    }

    @Test
    void publishesCompletionOnce_whenResolutionDecisionCompletesWorkflow() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<NodeMatchDecision> decisions = requiredDecisions();
        eventStore.appendIfAbsent(ingestedEvent);
        eventStore.appendIfAbsent(
            RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification())
        );
        eventStore.appendIfAbsent(
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action)
        );
        eventStore.appendIfAbsent(
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(0))
        );
        eventStore.appendIfAbsent(
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(1))
        );
        NodeResolutionDecidedEvent event =
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(2));

        // Act
        orchestrator.onNodeResolutionDecided(event);
        orchestrator.onNodeResolutionDecided(event);

        // Assert
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementAnalysisCompletedEvent.class, completion ->
                assertThat(completion.nodeMatchDecisions()).containsExactlyElementsOf(decisions)
            );
    }

    @Test
    void recordsReviewRequiredOutcome_whenNodeResolutionRequiresReview() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        NodeResolutionReviewRequiredEvent event =
            NodeResolutionReviewRequiredEvent.create(correlationId, reviewRequest());
        String reviewId = NodeMatchReviewId.from(
            correlationId,
            event.reviewRequest().requirementElement()
        ).value();

        // Act
        orchestrator.onNodeResolutionReviewRequired(event);

        // Assert
        assertThat(eventStore.load(correlationId)).containsExactly(event);
        assertThat(eventPublisher.applicationEvents()).isEmpty();
        assertThat(outcomeStore.consume(correlationId))
            .hasValueSatisfying(result -> {
                assertThat(result.correlationId()).isEqualTo(correlationId);
                assertThat(result.status())
                    .isEqualTo(IngestRequirementResult.Status.REVIEW_REQUIRED);
                assertThat(result.message()).isEqualTo("Requirement requires review.");
            });
        assertThat(reviewProjection.pendingReviews())
            .singleElement()
            .satisfies(review -> assertThat(review.reviewId()).isEqualTo(reviewId));
    }

    @Test
    void closesPendingReview_whenMatchingResolutionDecisionIsStored() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        NodeResolutionReviewRequiredEvent reviewRequiredEvent =
            NodeResolutionReviewRequiredEvent.create(correlationId, reviewRequest());
        orchestrator.onNodeResolutionReviewRequired(reviewRequiredEvent);
        String reviewId = NodeMatchReviewId.from(
            correlationId,
            reviewRequiredEvent.reviewRequest().requirementElement()
        ).value();
        NodeResolutionDecidedEvent decisionEvent = NodeResolutionDecidedEvent.create(
            correlationId,
            new NodeMatchDecision(
                reviewRequiredEvent.reviewRequest().requirementElement(),
                NodeMatchDecisionStatus.REVIEW_MAP_EXISTING,
                reviewRequiredEvent.reviewRequest().candidates(),
                "Domain reviewer selected this candidate."
            )
        );

        // Act
        orchestrator.onNodeResolutionDecided(decisionEvent);

        // Assert
        assertThat(reviewProjection.pendingReviews()).isEmpty();
        assertThat(reviewProjection.pendingReview(reviewId)).isEmpty();
    }

    private RequirementIngestedEvent ingestedEvent() {
        return RequirementIngestedEvent.create(Provenance.create(
            ElementId.create(),
            new OriginalText(RAW_TEXT.text()),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        ));
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The sentence expresses a verifiable obligation.")
        );
    }

    private Action actionWithConditionAndConstraint() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(new Condition("before authentication")),
            Set.of(new Constraint("within 200 milliseconds"))
        );
    }

    private Action actionWithoutOptionalElements() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(),
            Set.of()
        );
    }

    private List<NodeMatchDecision> requiredDecisions() {
        return List.of(
            autoCreateDecision(new RequirementElement(RequirementElementType.SUBJECT, "login form")),
            autoCreateDecision(new RequirementElement(RequirementElementType.ACTION, "must validate")),
            autoCreateDecision(new RequirementElement(RequirementElementType.OBJECT, "credentials"))
        );
    }

    private NodeMatchDecision autoCreateDecision(RequirementElement element) {
        return new NodeMatchDecision(
            element,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private NodeMatchReviewRequest reviewRequest() {
        return new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "login form"),
            List.of(new RetrievedCandidateNode(
                new CandidateNode("login-form", "Login Form", NodeType.CONCEPT),
                List.of(new RetrievalEvidence(
                    "nodeName",
                    "Matched login form to an existing graph node",
                    0.82
                ))
            )),
            "Candidate needs review before mapping"
        );
    }

    private static final class RecordingCommandBus implements CommandBus {

        private final List<Command<?>> commands = new ArrayList<>();

        @Override
        public <R, C extends Command<R>> R dispatch(C command) {
            commands.add(command);
            return null;
        }

        List<Command<?>> commands() {
            return commands;
        }
    }

    private static final class RecordingEventPublisher implements EventPublisher {

        private final List<ApplicationEvent> applicationEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
        }

        @Override
        public void publishAll(List<DomainEvent> events) {
        }

        @Override
        public void publish(ApplicationEvent event) {
            applicationEvents.add(event);
        }

        @Override
        public void publishApplicationEvents(List<ApplicationEvent> events) {
            applicationEvents.addAll(events);
        }

        List<ApplicationEvent> applicationEvents() {
            return applicationEvents;
        }
    }
}
