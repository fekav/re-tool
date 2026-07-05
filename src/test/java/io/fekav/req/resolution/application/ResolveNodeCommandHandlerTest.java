package io.fekav.req.resolution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.resolution.domain.NodeMatchingPolicy;
import io.fekav.req.resolution.domain.NodeMatchingService;
import io.fekav.req.resolution.domain.NodeRetrievalPolicy;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class ResolveNodeCommandHandlerTest {

    @Test
    void returnsNodeResolutionDecidedEventAndPublishesSameEvent_whenResolutionSucceeds() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "billing service");
        RetrievedCandidateNode candidate = candidate();
        List<CandidateNodeMatch> handledMatches = new ArrayList<>();
        NodeRetrievalPolicy retrievalPolicy =
            requirementElement -> new CandidateNodeMatch(requirementElement, List.of(candidate));
        NodeMatchingPolicy matchingPolicy = match -> {
            handledMatches.add(match);
            return new NodeMatchDecision(
                match.requirementElement(),
                NodeMatchDecisionStatus.PROPOSE_EXISTING,
                List.of(candidate),
                "Top candidate is proposed"
            );
        };
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        ResolveNodeCommandHandler handler = handlerWith(
            eventPublisher,
            retrievalPolicy,
            matchingPolicy
        );
        ResolveNodeCommand command = new ResolveNodeCommand(correlationId, element);

        // When
        NodeResolutionDecidedEvent result = handler.handle(command);

        // Then
        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.decision().requirementElement()).isEqualTo(element);
        assertThat(result.decision().status()).isEqualTo(NodeMatchDecisionStatus.PROPOSE_EXISTING);
        assertThat(handledMatches)
            .singleElement()
            .satisfies(match -> {
                assertThat(match.requirementElement()).isEqualTo(element);
                assertThat(match.candidates()).containsExactly(candidate);
            });
        assertThat(eventPublisher.applicationEvents()).containsExactly(result);
    }

    @Test
    void assignsCorrelationId_whenCommandIsCreatedWithoutOne() {
        // Given
        ResolveNodeCommandHandler handler = handlerWith(
            new RecordingEventPublisher(),
            requirementElement -> new CandidateNodeMatch(requirementElement, List.of()),
            match -> autoCreateDecision(match.requirementElement())
        );
        RequirementElement element =
            new RequirementElement(RequirementElementType.OBJECT, "invoice");

        // When
        NodeResolutionDecidedEvent result =
            handler.handle(new ResolveNodeCommand(element));

        // Then
        assertThat(result.correlationId()).isNotNull();
        assertThat(result.decision().requirementElement()).isEqualTo(element);
        assertThat(result.decision().status()).isEqualTo(NodeMatchDecisionStatus.AUTO_CREATE_NEW);
    }

    @Test
    void rejectsCommand_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ResolveNodeCommand(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirementElement must not be null");
    }

    @Test
    void exposesHandledCommandType() {
        // Given
        ResolveNodeCommandHandler handler = handlerWith(
            new RecordingEventPublisher(),
            requirementElement -> new CandidateNodeMatch(requirementElement, List.of()),
            match -> autoCreateDecision(match.requirementElement())
        );

        // When
        Class<ResolveNodeCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(ResolveNodeCommand.class);
    }

    private ResolveNodeCommandHandler handlerWith(
        RecordingEventPublisher eventPublisher,
        NodeRetrievalPolicy retrievalPolicy,
        NodeMatchingPolicy matchingPolicy
    ) {
        return new ResolveNodeCommandHandler(
            eventPublisher,
            new NodeRetrievalService(retrievalPolicy),
            new NodeMatchingService(matchingPolicy)
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

    private RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("concept-1", "Billing Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.8))
        );
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
