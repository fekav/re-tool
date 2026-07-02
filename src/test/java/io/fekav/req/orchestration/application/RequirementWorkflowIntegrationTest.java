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
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.application.ClassifyRequirementCommand;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.conceptmatching.application.EvaluateConceptMatchCommand;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptretrieval.application.RetrieveCandidateConceptsCommand;
import io.fekav.req.orchestration.infrastructure.InMemoryEventStore;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RawText;
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
    private final RequirementWorkflowOrchestrator orchestrator =
        new RequirementWorkflowOrchestrator(
            new InMemoryEventStore(),
            commandBus,
            eventPublisher
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

        List<ConceptCandidatesRetrievedEvent> retrievedEvents = retrievalCommands()
            .stream()
            .map(command -> ConceptCandidatesRetrievedEvent.create(
                correlationId,
                noCandidates(command.requirementElement())
            ))
            .toList();
        retrievedEvents.forEach(orchestrator::onConceptCandidatesRetrieved);

        List<ConceptMatchEvaluatedEvent> evaluatedEvents = matchingCommands()
            .stream()
            .map(command -> ConceptMatchEvaluatedEvent.create(
                correlationId,
                command.match(),
                autoCreateDecision(command.match().requirementElement())
            ))
            .toList();
        evaluatedEvents.forEach(orchestrator::onConceptMatchEvaluated);

        orchestrator.onRequirementIngested(ingestedEvent);
        orchestrator.onRequirementElementsExtracted(extractedEvent);
        orchestrator.onConceptCandidatesRetrieved(retrievedEvents.getFirst());
        orchestrator.onConceptMatchEvaluated(evaluatedEvents.getFirst());

        // Assert
        assertThat(commandBus.commands())
            .filteredOn(command -> command instanceof ClassifyRequirementCommand)
            .hasSize(1);
        assertThat(commandBus.commands())
            .filteredOn(command -> command instanceof ExtractSyntaxCommand)
            .hasSize(1);
        assertThat(retrievalCommands()).hasSize(3);
        assertThat(matchingCommands()).hasSize(3);
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementAnalysisCompletedEvent.class, completion -> {
                assertThat(completion.correlationId()).isEqualTo(correlationId);
                assertThat(completion.provenance()).isEqualTo(ingestedEvent.provenance());
                assertThat(completion.conceptMatchResults()).hasSize(3);
            });
    }

    private List<RetrieveCandidateConceptsCommand> retrievalCommands() {
        return commandBus.commands()
            .stream()
            .filter(RetrieveCandidateConceptsCommand.class::isInstance)
            .map(RetrieveCandidateConceptsCommand.class::cast)
            .toList();
    }

    private List<EvaluateConceptMatchCommand> matchingCommands() {
        return commandBus.commands()
            .stream()
            .filter(EvaluateConceptMatchCommand.class::isInstance)
            .map(EvaluateConceptMatchCommand.class::cast)
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

    private CandidateConceptMatch noCandidates(RequirementElement element) {
        return new CandidateConceptMatch(element, List.of());
    }

    private ConceptMatchDecision autoCreateDecision(RequirementElement element) {
        return new ConceptMatchDecision(
            element,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
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
