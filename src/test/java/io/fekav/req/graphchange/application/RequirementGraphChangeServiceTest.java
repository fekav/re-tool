package io.fekav.req.graphchange.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.ingestion.application.IngestRequirementResult;
import io.fekav.req.ingestion.application.IngestionOutcomeStore;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementKnownEvent;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.SourceMetadata;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class RequirementGraphChangeServiceTest {

    private final CompletedAnalysisGraphChangeFactory factory =
        new CompletedAnalysisGraphChangeFactory();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final RecordingGraphChangePort graphChangePort =
        new RecordingGraphChangePort();
    private final IngestionOutcomeStore outcomeStore = new IngestionOutcomeStore();

    @Test
    void ignoresCompletionEvents_whenGraphChangeIsDisabled() {
        // Arrange
        RequirementGraphChangeService service = new RequirementGraphChangeService(
            factory,
            graphChangePort,
            eventPublisher,
            outcomeStore,
            false
        );
        RequirementAnalysisCompletedEvent event = completedEvent();

        // Act
        service.onRequirementAnalysisCompleted(event);

        // Assert
        assertThat(graphChangePort.graphChanges()).isEmpty();
        assertThat(eventPublisher.applicationEvents()).isEmpty();
        assertThat(outcomeStore.consume(event.correlationId())).isEmpty();
    }

    @Test
    void publishesRequirementKnownEvent_whenAssertionAlreadyExists() {
        // Arrange
        RequirementGraphChangeService service = enabledService();
        graphChangePort.result = RequirementGraphChangeResult.known(assertionIdentity());
        RequirementAnalysisCompletedEvent event = completedEvent();

        // Act
        service.onRequirementAnalysisCompleted(event);

        // Assert
        assertThat(graphChangePort.graphChanges()).hasSize(1);
        assertThat(eventPublisher.applicationEvents())
            .singleElement()
            .isInstanceOfSatisfying(RequirementKnownEvent.class, knownEvent -> {
                assertThat(knownEvent.correlationId()).isEqualTo(event.correlationId());
                assertThat(knownEvent.subject()).isEqualTo(assertionIdentity().subject());
                assertThat(knownEvent.predicate()).isEqualTo(assertionIdentity().predicate());
                assertThat(knownEvent.object()).isEqualTo(assertionIdentity().object());
            });
        assertOutcome(event.correlationId(), IngestRequirementResult.Status.ALREADY_EXISTS);
    }

    @Test
    void publishesNoEvent_whenAssertionWasNewlyPersisted() {
        // Arrange
        RequirementGraphChangeService service = enabledService();
        graphChangePort.result = RequirementGraphChangeResult.created();
        RequirementAnalysisCompletedEvent event = completedEvent();

        // Act
        service.onRequirementAnalysisCompleted(event);

        // Assert
        assertThat(graphChangePort.graphChanges()).hasSize(1);
        assertThat(eventPublisher.applicationEvents()).isEmpty();
        assertOutcome(event.correlationId(), IngestRequirementResult.Status.RECORDED);
    }

    @Test
    void publishesNoEvent_whenRequirementWasAlreadyPersisted() {
        // Arrange
        RequirementGraphChangeService service = enabledService();
        graphChangePort.result = RequirementGraphChangeResult.unchanged();
        RequirementAnalysisCompletedEvent event = completedEvent();

        // Act
        service.onRequirementAnalysisCompleted(event);

        // Assert
        assertThat(graphChangePort.graphChanges()).hasSize(1);
        assertThat(eventPublisher.applicationEvents()).isEmpty();
        assertOutcome(event.correlationId(), IngestRequirementResult.Status.ALREADY_EXISTS);
    }

    private RequirementGraphChangeService enabledService() {
        return new RequirementGraphChangeService(
            factory,
            graphChangePort,
            eventPublisher,
            outcomeStore,
            true
        );
    }

    private void assertOutcome(
        CorrelationId correlationId,
        IngestRequirementResult.Status status
    ) {
        assertThat(outcomeStore.consume(correlationId))
            .hasValueSatisfying(result -> {
                assertThat(result.correlationId()).isEqualTo(correlationId);
                assertThat(result.status()).isEqualTo(status);
                assertThat(result.message()).isNotBlank();
            });
    }

    private RequirementAnalysisCompletedEvent completedEvent() {
        return RequirementAnalysisCompletedEvent.create(
            CorrelationId.create(),
            provenance(),
            classification(),
            action(),
            List.of(
                autoCreateDecision(RequirementElementType.SUBJECT, "login form"),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials")
            )
        );
    }

    private AssertionIdentity assertionIdentity() {
        return new AssertionIdentity(
            new GraphNodeReference(NodeType.CONCEPT, "login form", "login form"),
            new GraphNodeReference(NodeType.PREDICATE, "must validate", "must validate"),
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "credentials")
        );
    }

    private Provenance provenance() {
        return Provenance.create(
            ElementId.create(),
            new OriginalText("The login form must validate credentials."),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        );
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

    private NodeMatchDecision autoCreateDecision(
        RequirementElementType type,
        String text
    ) {
        return new NodeMatchDecision(
            new RequirementElement(type, text),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private static final class RecordingGraphChangePort
        implements RequirementGraphChangePort {

        private final List<PersistRequirementGraphChange> graphChanges =
            new ArrayList<>();
        private RequirementGraphChangeResult result =
            RequirementGraphChangeResult.created();

        @Override
        public RequirementGraphChangeResult persist(
            PersistRequirementGraphChange graphChange
        ) {
            graphChanges.add(graphChange);
            return result;
        }

        List<PersistRequirementGraphChange> graphChanges() {
            return graphChanges;
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
