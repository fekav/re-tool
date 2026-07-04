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
import io.fekav.req.resolution.domain.ConceptMatchingPolicy;
import io.fekav.req.resolution.domain.ConceptMatchingService;
import io.fekav.req.resolution.domain.ConceptRetrievalPolicy;
import io.fekav.req.resolution.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptResolutionDecidedEvent;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class ResolveConceptCommandHandlerTest {

    @Test
    void returnsConceptResolutionDecidedEventAndPublishesSameEvent_whenResolutionSucceeds() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "billing service");
        RetrievedCandidateConcept candidate = candidate();
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptRetrievalPolicy retrievalPolicy =
            requirementElement -> new CandidateConceptMatch(requirementElement, List.of(candidate));
        ConceptMatchingPolicy matchingPolicy = match -> {
            handledMatches.add(match);
            return new ConceptMatchDecision(
                match.requirementElement(),
                ConceptMatchDecisionStatus.PROPOSE_EXISTING,
                List.of(candidate),
                "Top candidate is proposed"
            );
        };
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        ResolveConceptCommandHandler handler = handlerWith(
            eventPublisher,
            retrievalPolicy,
            matchingPolicy
        );
        ResolveConceptCommand command = new ResolveConceptCommand(correlationId, element);

        // When
        ConceptResolutionDecidedEvent result = handler.handle(command);

        // Then
        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.decision().requirementElement()).isEqualTo(element);
        assertThat(result.decision().status()).isEqualTo(ConceptMatchDecisionStatus.PROPOSE_EXISTING);
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
        ResolveConceptCommandHandler handler = handlerWith(
            new RecordingEventPublisher(),
            requirementElement -> new CandidateConceptMatch(requirementElement, List.of()),
            match -> autoCreateDecision(match.requirementElement())
        );
        RequirementElement element =
            new RequirementElement(RequirementElementType.OBJECT, "invoice");

        // When
        ConceptResolutionDecidedEvent result =
            handler.handle(new ResolveConceptCommand(element));

        // Then
        assertThat(result.correlationId()).isNotNull();
        assertThat(result.decision().requirementElement()).isEqualTo(element);
        assertThat(result.decision().status()).isEqualTo(ConceptMatchDecisionStatus.AUTO_CREATE_NEW);
    }

    @Test
    void rejectsCommand_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ResolveConceptCommand(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirementElement must not be null");
    }

    @Test
    void exposesHandledCommandType() {
        // Given
        ResolveConceptCommandHandler handler = handlerWith(
            new RecordingEventPublisher(),
            requirementElement -> new CandidateConceptMatch(requirementElement, List.of()),
            match -> autoCreateDecision(match.requirementElement())
        );

        // When
        Class<ResolveConceptCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(ResolveConceptCommand.class);
    }

    private ResolveConceptCommandHandler handlerWith(
        RecordingEventPublisher eventPublisher,
        ConceptRetrievalPolicy retrievalPolicy,
        ConceptMatchingPolicy matchingPolicy
    ) {
        return new ResolveConceptCommandHandler(
            eventPublisher,
            new ConceptRetrievalService(retrievalPolicy),
            new ConceptMatchingService(matchingPolicy)
        );
    }

    private ConceptMatchDecision autoCreateDecision(RequirementElement element) {
        return new ConceptMatchDecision(
            element,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private RetrievedCandidateConcept candidate() {
        return new RetrievedCandidateConcept(
            new CandidateConcept("concept-1", "Billing Service", "SystemComponent"),
            List.of(new RetrievalEvidence("conceptName", "matched concept name", 0.8))
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
