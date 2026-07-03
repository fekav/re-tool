package io.fekav.req.orchestration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.SourceMetadata;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class WorkflowStateTest {

    private static final RawText RAW_TEXT =
        new RawText("The login form must validate credentials before authentication.");

    @Test
    void derivesExpectedRequirementElements_whenExtractionEventWasRecorded() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        Action action = actionWithConditionAndConstraint();

        // Act
        WorkflowState state = WorkflowState.replay(List.of(
            RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action)
        ));

        // Assert
        assertThat(state.expectedRequirementElements()).containsExactly(
            new RequirementElement(RequirementElementType.SUBJECT, "login form"),
            new RequirementElement(RequirementElementType.ACTION, "must validate"),
            new RequirementElement(RequirementElementType.OBJECT, "credentials"),
            new RequirementElement(RequirementElementType.CONDITION, "before authentication"),
            new RequirementElement(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
        );
        assertThat(state.expectedRetrievalCount()).isEqualTo(5);
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void derivesExpectedMatchDecisionsFromRecordedRetrievalResults() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        List<CandidateConceptMatch> matches = requiredMatches();

        // Act
        WorkflowState state = WorkflowState.replay(List.of(
            ConceptCandidatesRetrievedEvent.create(correlationId, matches.get(0)),
            ConceptCandidatesRetrievedEvent.create(correlationId, matches.get(1)),
            ConceptCandidatesRetrievedEvent.create(correlationId, matches.get(2))
        ));

        // Assert
        assertThat(state.retrievedCandidateMatches()).containsExactlyElementsOf(matches);
        assertThat(state.expectedMatchDecisionCount()).isEqualTo(3);
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void ignoresRetrievedCandidateMatches_whenRequirementElementWasNotExpected() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        CandidateConceptMatch unexpectedMatch = noCandidates(
            new RequirementElement(RequirementElementType.CONDITION, "after logout")
        );

        // Act
        WorkflowState state = WorkflowState.replay(List.of(
            RequirementElementsExtractedEvent.create(
                correlationId,
                RAW_TEXT,
                actionWithoutOptionalElements()
            ),
            ConceptCandidatesRetrievedEvent.create(correlationId, unexpectedMatch)
        ));

        // Assert
        assertThat(state.retrievedCandidateMatches()).isEmpty();
        assertThat(state.expectedMatchDecisionCount()).isZero();
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void isComplete_whenIngestedClassifiedExtractedRetrievedAndMatched() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<CandidateConceptMatch> matches = requiredMatches();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action));
        matches.forEach(match -> events.add(ConceptCandidatesRetrievedEvent.create(correlationId, match)));
        matches.forEach(match -> events.add(ConceptMatchEvaluatedEvent.create(
            correlationId,
            autoCreateDecision(match.requirementElement())
        )));

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isTrue();
        assertThat(state.conceptMatchDecisions())
            .containsExactlyElementsOf(matches.stream()
                .map(match -> autoCreateDecision(match.requirementElement()))
                .toList());
    }

    @Test
    void isNotComplete_whenOneMatchDecisionIsStillMissing() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<CandidateConceptMatch> matches = requiredMatches();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action));
        matches.forEach(match -> events.add(ConceptCandidatesRetrievedEvent.create(correlationId, match)));
        events.add(ConceptMatchEvaluatedEvent.create(
            correlationId,
            autoCreateDecision(matches.getFirst().requirementElement())
        ));

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isFalse();
        assertThat(state.expectedMatchDecisionCount()).isEqualTo(3);
        assertThat(state.conceptMatchDecisions()).hasSize(1);
    }

    @Test
    void ignoresMatchDecisions_whenRequirementElementWasNotRetrieved() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        List<CandidateConceptMatch> matches = requiredMatches();
        RequirementElement unexpectedElement =
            new RequirementElement(RequirementElementType.CONDITION, "after logout");
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            actionWithoutOptionalElements()
        ));
        matches.forEach(match -> events.add(
            ConceptCandidatesRetrievedEvent.create(correlationId, match)
        ));
        events.add(ConceptMatchEvaluatedEvent.create(
            correlationId,
            autoCreateDecision(unexpectedElement)
        ));

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isFalse();
        assertThat(state.expectedMatchDecisionCount()).isEqualTo(3);
        assertThat(state.conceptMatchDecisions()).isEmpty();
    }

    @Test
    void deduplicatesEquivalentMatchDecisions_whenEventsAreRedeliveredWithNewEventId() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        List<CandidateConceptMatch> matches = requiredMatches();
        List<ConceptMatchDecision> decisions = matches.stream()
            .map(match -> autoCreateDecision(match.requirementElement()))
            .toList();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            actionWithoutOptionalElements()
        ));
        matches.forEach(match -> events.add(
            ConceptCandidatesRetrievedEvent.create(correlationId, match)
        ));
        decisions.forEach(decision ->
            events.add(ConceptMatchEvaluatedEvent.create(correlationId, decision))
        );
        decisions.forEach(decision ->
            events.add(ConceptMatchEvaluatedEvent.create(correlationId, decision))
        );

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isTrue();
        assertThat(state.conceptMatchDecisions()).containsExactlyElementsOf(decisions);
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
}
