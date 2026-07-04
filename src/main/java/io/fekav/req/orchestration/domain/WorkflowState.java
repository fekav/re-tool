package io.fekav.req.orchestration.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.event.ConceptResolutionDecidedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.syntaxextraction.domain.Action;

public final class WorkflowState {

    private final Optional<Provenance> provenance;
    private final Optional<Classification> classification;
    private final Optional<Action> action;
    private final List<RequirementElement> expectedRequirementElements;
    private final List<ConceptMatchDecision> conceptMatchDecisions;

    private WorkflowState(
        Optional<Provenance> provenance,
        Optional<Classification> classification,
        Optional<Action> action,
        List<RequirementElement> expectedRequirementElements,
        List<ConceptMatchDecision> conceptMatchDecisions
    ) {
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.classification = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.expectedRequirementElements = List.copyOf(expectedRequirementElements);
        this.conceptMatchDecisions = List.copyOf(conceptMatchDecisions);
    }

    public static WorkflowState replay(List<ApplicationEvent> events) {
        Objects.requireNonNull(events, "events must not be null");

        RequirementElementCollector collector = new RequirementElementCollector();
        Provenance provenance = null;
        Classification classification = null;
        Action action = null;
        List<RequirementElement> expectedRequirementElements = List.of();
        List<ConceptMatchDecision> allConceptMatchDecisions = new ArrayList<>();

        for (ApplicationEvent event : events) {
            Objects.requireNonNull(event, "events must not contain null");

            if (event instanceof RequirementIngestedEvent ingestedEvent) {
                provenance = ingestedEvent.provenance();
            }
            if (event instanceof RequirementClassifiedEvent classifiedEvent) {
                classification = classifiedEvent.classification();
            }
            if (event instanceof RequirementElementsExtractedEvent extractedEvent) {
                action = extractedEvent.action();
                expectedRequirementElements = collector.collectFrom(extractedEvent.action());
            }
            if (event instanceof ConceptResolutionDecidedEvent decidedEvent) {
                allConceptMatchDecisions.add(decidedEvent.decision());
            }
        }

        List<ConceptMatchDecision> relevantConceptMatchDecisions =
            filterRelevantConceptMatchDecisions(
                expectedRequirementElements,
                allConceptMatchDecisions
            );

        return new WorkflowState(
            Optional.ofNullable(provenance),
            Optional.ofNullable(classification),
            Optional.ofNullable(action),
            expectedRequirementElements,
            relevantConceptMatchDecisions
        );
    }

    private static List<ConceptMatchDecision> filterRelevantConceptMatchDecisions(
        List<RequirementElement> expectedRequirementElements,
        List<ConceptMatchDecision> conceptMatchDecisions
    ) {
        if (expectedRequirementElements.isEmpty()) {
            return distinctValues(conceptMatchDecisions);
        }

        Set<RequirementElement> relevantRequirementElements =
            new LinkedHashSet<>(expectedRequirementElements);
        return distinctValues(conceptMatchDecisions.stream()
            .filter(decision ->
                relevantRequirementElements.contains(decision.requirementElement())
            )
            .toList());
    }

    private static <T> List<T> distinctValues(List<T> values) {
        return values.stream()
            .distinct()
            .toList();
    }

    public Optional<Provenance> provenance() {
        return provenance;
    }

    public Optional<Classification> classification() {
        return classification;
    }

    public Optional<Action> action() {
        return action;
    }

    public List<RequirementElement> expectedRequirementElements() {
        return expectedRequirementElements;
    }

    public int expectedResolutionDecisionCount() {
        return expectedRequirementElements.size();
    }

    public List<ConceptMatchDecision> conceptMatchDecisions() {
        return conceptMatchDecisions;
    }

    public boolean isComplete() {
        return provenance.isPresent() &&
            classification.isPresent() &&
            action.isPresent() &&
            conceptMatchDecisions.size() == expectedResolutionDecisionCount();
    }
}
