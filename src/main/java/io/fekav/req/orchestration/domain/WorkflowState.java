package io.fekav.req.orchestration.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.extraction.domain.Action;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;

public final class WorkflowState {

    private final Optional<Provenance> provenance;
    private final Optional<Classification> classification;
    private final Optional<Action> action;
    private final List<RequirementElement> expectedRequirementElements;
    private final List<NodeMatchDecision> nodeMatchDecisions;

    private WorkflowState(
        Optional<Provenance> provenance,
        Optional<Classification> classification,
        Optional<Action> action,
        List<RequirementElement> expectedRequirementElements,
        List<NodeMatchDecision> nodeMatchDecisions
    ) {
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.classification = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.expectedRequirementElements = List.copyOf(expectedRequirementElements);
        this.nodeMatchDecisions = List.copyOf(nodeMatchDecisions);
    }

    public static WorkflowState replay(List<ApplicationEvent> events) {
        Objects.requireNonNull(events, "events must not be null");

        RequirementElementCollector collector = new RequirementElementCollector();
        Provenance provenance = null;
        Classification classification = null;
        Action action = null;
        List<RequirementElement> expectedRequirementElements = List.of();
        List<NodeMatchDecision> allNodeMatchDecisions = new ArrayList<>();

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
            if (event instanceof NodeResolutionDecidedEvent decidedEvent) {
                allNodeMatchDecisions.add(decidedEvent.decision());
            }
        }

        List<NodeMatchDecision> relevantNodeMatchDecisions =
            filterRelevantNodeMatchDecisions(
                expectedRequirementElements,
                allNodeMatchDecisions
            );

        return new WorkflowState(
            Optional.ofNullable(provenance),
            Optional.ofNullable(classification),
            Optional.ofNullable(action),
            expectedRequirementElements,
            relevantNodeMatchDecisions
        );
    }

    private static List<NodeMatchDecision> filterRelevantNodeMatchDecisions(
        List<RequirementElement> expectedRequirementElements,
        List<NodeMatchDecision> nodeMatchDecisions
    ) {
        if (expectedRequirementElements.isEmpty()) {
            return distinctValues(nodeMatchDecisions);
        }

        Set<RequirementElement> relevantRequirementElements =
            new LinkedHashSet<>(expectedRequirementElements);
        return distinctValues(nodeMatchDecisions.stream()
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

    public List<NodeMatchDecision> nodeMatchDecisions() {
        return nodeMatchDecisions;
    }

    public boolean isComplete() {
        return provenance.isPresent() &&
            classification.isPresent() &&
            action.isPresent() &&
            nodeMatchDecisions.size() == expectedResolutionDecisionCount();
    }
}
