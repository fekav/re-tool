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
import io.fekav.req.ingestion.application.IngestionOutcomeStore;
import io.fekav.req.orchestration.infrastructure.InMemoryEventStore;
import io.fekav.req.review.application.NodeMatchReviewProjection;
import io.fekav.req.review.application.SubmitNodeMatchReviewDecisionCommand;
import io.fekav.req.review.application.SubmitNodeMatchReviewDecisionCommandHandler;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.resolution.application.ResolveNodeCommand;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.ElementId;
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
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class RequirementWorkflowIntegrationTest {

    private static final RawText RAW_TEXT =
        new RawText("The login form must validate credentials.");

    private final RecordingCommandBus commandBus = new RecordingCommandBus();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final IngestionOutcomeStore outcomeStore = new IngestionOutcomeStore();
    private final InMemoryEventStore eventStore = new InMemoryEventStore();
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
    void completesAnalysisWorkflow_whenAllSliceEventsAreConsumedInProcess() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();

        // Act
        orchestrator.onRequirementIngested(ingestedEvent);
        orchestrator.onRequirementClassified(
            RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification())
        );
        RequirementElementsExtractedEvent extractedEvent =
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action());
        orchestrator.onRequirementElementsExtracted(extractedEvent);

        List<NodeResolutionDecidedEvent> resolutionEvents = resolutionCommands()
            .stream()
            .map(command -> NodeResolutionDecidedEvent.create(
                correlationId,
                autoCreateDecision(command.requirementElement())
            ))
            .toList();
        resolutionEvents.forEach(orchestrator::onNodeResolutionDecided);

        orchestrator.onRequirementIngested(ingestedEvent);
        orchestrator.onRequirementElementsExtracted(extractedEvent);
        orchestrator.onNodeResolutionDecided(resolutionEvents.getFirst());

        // Assert
        assertThat(commandBus.commands())
            .filteredOn(command -> command instanceof ClassifyRequirementCommand)
            .hasSize(1);
        assertThat(commandBus.commands())
            .filteredOn(command -> command instanceof ExtractSyntaxCommand)
            .hasSize(1);
        assertThat(resolutionCommands()).hasSize(3);
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementAnalysisCompletedEvent.class, completion -> {
                assertThat(completion.correlationId()).isEqualTo(correlationId);
                assertThat(completion.provenance()).isEqualTo(ingestedEvent.provenance());
                assertThat(completion.nodeMatchDecisions()).hasSize(3);
            });
    }

    @Test
    void completesAnalysisWorkflow_whenLastResolutionDecisionComesFromSubmittedReview() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        orchestrator.onRequirementIngested(ingestedEvent);
        orchestrator.onRequirementClassified(
            RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification())
        );
        orchestrator.onRequirementElementsExtracted(
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action())
        );

        RequirementElement reviewElement =
            new RequirementElement(RequirementElementType.SUBJECT, "login form");
        NodeResolutionReviewRequiredEvent reviewRequiredEvent =
            NodeResolutionReviewRequiredEvent.create(
                correlationId,
                reviewRequest(reviewElement)
            );
        resolutionCommands()
            .stream()
            .filter(command -> command.requirementElement().type() != RequirementElementType.SUBJECT)
            .map(command -> NodeResolutionDecidedEvent.create(
                correlationId,
                autoCreateDecision(command.requirementElement())
            ))
            .forEach(orchestrator::onNodeResolutionDecided);
        orchestrator.onNodeResolutionReviewRequired(reviewRequiredEvent);

        SubmitNodeMatchReviewDecisionCommandHandler reviewHandler =
            new SubmitNodeMatchReviewDecisionCommandHandler(
                reviewProjection,
                new RoutingEventPublisher(orchestrator)
            );
        String reviewId = NodeMatchReviewId.from(correlationId, reviewElement).value();

        // Act
        reviewHandler.handle(new SubmitNodeMatchReviewDecisionCommand(
            reviewId,
            "MAP_EXISTING",
            "concept-login-form",
            "Domain reviewer selected this candidate."
        ));

        // Assert
        assertThat(reviewProjection.pendingReviews()).isEmpty();
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementAnalysisCompletedEvent.class, completion -> {
                assertThat(completion.correlationId()).isEqualTo(correlationId);
                assertThat(completion.nodeMatchDecisions())
                    .extracting(NodeMatchDecision::status)
                    .containsExactlyInAnyOrder(
                        NodeMatchDecisionStatus.REVIEW_MAP_EXISTING,
                        NodeMatchDecisionStatus.AUTO_CREATE_NEW,
                        NodeMatchDecisionStatus.AUTO_CREATE_NEW
                    );
            });
    }

    private List<ResolveNodeCommand> resolutionCommands() {
        return commandBus.commands()
            .stream()
            .filter(ResolveNodeCommand.class::isInstance)
            .map(ResolveNodeCommand.class::cast)
            .toList();
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

    private Action action() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(),
            Set.of()
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

    private NodeMatchReviewRequest reviewRequest(RequirementElement element) {
        return new NodeMatchReviewRequest(
            element,
            List.of(candidate()),
            "Candidate needs review before mapping"
        );
    }

    private RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("concept-login-form", "Login Form", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
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

    private static final class RoutingEventPublisher implements EventPublisher {

        private final RequirementWorkflowOrchestrator orchestrator;

        private RoutingEventPublisher(RequirementWorkflowOrchestrator orchestrator) {
            this.orchestrator = orchestrator;
        }

        @Override
        public void publish(DomainEvent event) {
        }

        @Override
        public void publishAll(List<DomainEvent> events) {
        }

        @Override
        public void publish(ApplicationEvent event) {
            if (event instanceof NodeResolutionDecidedEvent decidedEvent) {
                orchestrator.onNodeResolutionDecided(decidedEvent);
            }
        }

        @Override
        public void publishApplicationEvents(List<ApplicationEvent> events) {
            events.forEach(this::publish);
        }
    }
}
