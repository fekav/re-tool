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
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
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
        assertThat(state.expectedResolutionDecisionCount()).isEqualTo(5);
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void keepsRelevantResolutionDecisions_whenRequirementElementWasExpected() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        List<NodeMatchDecision> decisions = requiredDecisions();

        // Act
        WorkflowState state = WorkflowState.replay(List.of(
            RequirementElementsExtractedEvent.create(
                correlationId,
                RAW_TEXT,
                actionWithoutOptionalElements()
            ),
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(0)),
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(1)),
            NodeResolutionDecidedEvent.create(correlationId, decisions.get(2))
        ));

        // Assert
        assertThat(state.nodeMatchDecisions()).containsExactlyElementsOf(decisions);
        assertThat(state.expectedResolutionDecisionCount()).isEqualTo(3);
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void ignoresResolutionDecisions_whenRequirementElementWasNotExpected() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        NodeMatchDecision unexpectedDecision = autoCreateDecision(
            new RequirementElement(RequirementElementType.CONDITION, "after logout")
        );

        // Act
        WorkflowState state = WorkflowState.replay(List.of(
            RequirementElementsExtractedEvent.create(
                correlationId,
                RAW_TEXT,
                actionWithoutOptionalElements()
            ),
            NodeResolutionDecidedEvent.create(correlationId, unexpectedDecision)
        ));

        // Assert
        assertThat(state.nodeMatchDecisions()).isEmpty();
        assertThat(state.expectedResolutionDecisionCount()).isEqualTo(3);
        assertThat(state.isComplete()).isFalse();
    }

    @Test
    void isComplete_whenIngestedClassifiedExtractedAndResolved() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<NodeMatchDecision> decisions = requiredDecisions();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action));
        decisions.forEach(decision ->
            events.add(NodeResolutionDecidedEvent.create(correlationId, decision))
        );

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isTrue();
        assertThat(state.nodeMatchDecisions()).containsExactlyElementsOf(decisions);
    }

    @Test
    void isNotComplete_whenOneResolutionDecisionIsStillMissing() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        Action action = actionWithoutOptionalElements();
        List<NodeMatchDecision> decisions = requiredDecisions();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(correlationId, RAW_TEXT, action));
        events.add(NodeResolutionDecidedEvent.create(correlationId, decisions.getFirst()));

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isFalse();
        assertThat(state.expectedResolutionDecisionCount()).isEqualTo(3);
        assertThat(state.nodeMatchDecisions()).hasSize(1);
    }

    @Test
    void deduplicatesEquivalentResolutionDecisions_whenEventsAreRedeliveredWithNewEventId() {
        // Arrange
        RequirementIngestedEvent ingestedEvent = ingestedEvent();
        CorrelationId correlationId = ingestedEvent.correlationId();
        List<NodeMatchDecision> decisions = requiredDecisions();
        List<ApplicationEvent> events = new ArrayList<>();
        events.add(ingestedEvent);
        events.add(RequirementClassifiedEvent.create(correlationId, RAW_TEXT, classification()));
        events.add(RequirementElementsExtractedEvent.create(
            correlationId,
            RAW_TEXT,
            actionWithoutOptionalElements()
        ));
        decisions.forEach(decision ->
            events.add(NodeResolutionDecidedEvent.create(correlationId, decision))
        );
        decisions.forEach(decision ->
            events.add(NodeResolutionDecidedEvent.create(correlationId, decision))
        );

        // Act
        WorkflowState state = WorkflowState.replay(events);

        // Assert
        assertThat(state.isComplete()).isTrue();
        assertThat(state.nodeMatchDecisions()).containsExactlyElementsOf(decisions);
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
}
