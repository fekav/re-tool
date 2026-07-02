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
    private final RequirementWorkflowOrchestrator orchestrator =
        new RequirementWorkflowOrchestrator(eventStore, commandBus, eventPublisher);

    @Test
    void ignoresEvents_whenOrchestrationIsDisabled() {
        // Arrange
        RequirementWorkflowOrchestrator disabledOrchestrator =
            new RequirementWorkflowOrchestrator(
                eventStore,
                commandBus,
                eventPublisher,
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
    void dispatchesRetrievalForEveryExtractedRequirementElement() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        RequirementElementsExtractedEvent event = RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            actionWithConditionAndConstraint()
        );

        // Act
        orchestrator.onRequirementElementsExtracted(event);

        // Assert
        assertThat(commandBus.commands()).hasSize(5);
        assertThat(commandBus.commands())
            .allSatisfy(command ->
                assertThat(command).isInstanceOf(RetrieveCandidateConceptsCommand.class)
            )
            .extracting(command ->
                ((RetrieveCandidateConceptsCommand) command).requirementElement()
            )
            .containsExactly(
                new RequirementElement(RequirementElementType.SUBJECT, "login form"),
                new RequirementElement(RequirementElementType.ACTION, "must validate"),
                new RequirementElement(RequirementElementType.OBJECT, "credentials"),
                new RequirementElement(RequirementElementType.CONDITION, "before authentication"),
                new RequirementElement(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
            );
        assertThat(commandBus.commands())
            .extracting(command -> ((RetrieveCandidateConceptsCommand) command).correlationId())
            .containsOnly(correlationId);
    }

    @Test
    void dispatchesMatchingForRetrievedCandidateMatch() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        CandidateConceptMatch match = noCandidates(
            new RequirementElement(RequirementElementType.SUBJECT, "login form")
        );
        ConceptCandidatesRetrievedEvent event =
            ConceptCandidatesRetrievedEvent.create(correlationId, match);

        // Act
        orchestrator.onConceptCandidatesRetrieved(event);

        // Assert
        assertThat(commandBus.commands())
            .singleElement()
            .isInstanceOfSatisfying(EvaluateConceptMatchCommand.class, command -> {
                assertThat(command.correlationId()).isEqualTo(correlationId);
                assertThat(command.match()).isEqualTo(match);
            });
    }

    @Test
    void publishesCompletionOnce_whenClassifiedEventCompletesWorkflow() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<CandidateConceptMatch> matches = requiredMatches();
        eventStore.appendIfAbsent(correlationId, ingestedEvent);
        eventStore.appendIfAbsent(
            correlationId,
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action)
        );
        matches.forEach(match ->
            eventStore.appendIfAbsent(
                correlationId,
                ConceptCandidatesRetrievedEvent.create(correlationId, match)
            )
        );
        matches.forEach(match ->
            eventStore.appendIfAbsent(
                correlationId,
                ConceptMatchEvaluatedEvent.create(
                    correlationId,
                    match,
                    autoCreateDecision(match.requirementElement())
                )
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
                assertThat(completion.conceptMatchResults()).hasSize(3);
            });
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

    private List<CandidateConceptMatch> requiredMatches() {
        return List.of(
            noCandidates(new RequirementElement(RequirementElementType.SUBJECT, "login form")),
            noCandidates(new RequirementElement(RequirementElementType.ACTION, "must validate")),
            noCandidates(new RequirementElement(RequirementElementType.OBJECT, "credentials"))
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
