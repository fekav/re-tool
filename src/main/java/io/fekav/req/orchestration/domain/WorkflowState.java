package io.fekav.req.orchestration.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchResult;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.syntaxextraction.domain.Action;

public final class WorkflowState {

    private final Optional<Provenance> provenance;
    private final Optional<Classification> classification;
    private final Optional<Action> action;
    private final List<RequirementElement> expectedRequirementElements;
    private final List<CandidateConceptMatch> retrievedCandidateMatches;
    private final List<ConceptMatchResult> conceptMatchResults;

    private WorkflowState(
        Optional<Provenance> provenance,
        Optional<Classification> classification,
        Optional<Action> action,
        List<RequirementElement> expectedRequirementElements,
        List<CandidateConceptMatch> retrievedCandidateMatches,
        List<ConceptMatchResult> conceptMatchResults
    ) {
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.classification = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.expectedRequirementElements = List.copyOf(expectedRequirementElements);
        this.retrievedCandidateMatches = List.copyOf(retrievedCandidateMatches);
        this.conceptMatchResults = List.copyOf(conceptMatchResults);
    }

    public static WorkflowState replay(List<ApplicationEvent> events) {
        Objects.requireNonNull(events, "events must not be null");

        RequirementElementCollector collector = new RequirementElementCollector();
        Provenance provenance = null;
        Classification classification = null;
        Action action = null;
        List<RequirementElement> expectedRequirementElements = List.of();
        List<CandidateConceptMatch> allRetrievedCandidateMatches = new ArrayList<>();
        List<ConceptMatchResult> allConceptMatchResults = new ArrayList<>();

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
            if (event instanceof ConceptCandidatesRetrievedEvent retrievedEvent) {
                allRetrievedCandidateMatches.add(retrievedEvent.match());
            }
            if (event instanceof ConceptMatchEvaluatedEvent evaluatedEvent) {
                allConceptMatchResults.add(new ConceptMatchResult(
                    evaluatedEvent.match(),
                    evaluatedEvent.decision()
                ));
            }
        }

        List<CandidateConceptMatch> relevantRetrievedCandidateMatches =
            filterRelevantRetrievedCandidateMatches(
                expectedRequirementElements,
                allRetrievedCandidateMatches
            );
        List<ConceptMatchResult> relevantConceptMatchResults =
            filterRelevantConceptMatchResults(
                allRetrievedCandidateMatches,
                relevantRetrievedCandidateMatches,
                allConceptMatchResults
            );

        return new WorkflowState(
            Optional.ofNullable(provenance),
            Optional.ofNullable(classification),
            Optional.ofNullable(action),
            expectedRequirementElements,
            relevantRetrievedCandidateMatches,
            relevantConceptMatchResults
        );
    }

    private static List<CandidateConceptMatch> filterRelevantRetrievedCandidateMatches(
        List<RequirementElement> expectedRequirementElements,
        List<CandidateConceptMatch> retrievedCandidateMatches
    ) {
        if (expectedRequirementElements.isEmpty()) {
            return distinctValues(retrievedCandidateMatches);
        }

        Set<RequirementElement> expectedElements =
            new LinkedHashSet<>(expectedRequirementElements);
        return distinctValues(retrievedCandidateMatches.stream()
            .filter(match -> expectedElements.contains(match.requirementElement()))
            .toList());
    }

    private static List<ConceptMatchResult> filterRelevantConceptMatchResults(
        List<CandidateConceptMatch> allRetrievedCandidateMatches,
        List<CandidateConceptMatch> relevantRetrievedCandidateMatches,
        List<ConceptMatchResult> conceptMatchResults
    ) {
        if (allRetrievedCandidateMatches.isEmpty()) {
            return distinctValues(conceptMatchResults);
        }

        Set<CandidateConceptMatch> relevantMatches =
            new LinkedHashSet<>(relevantRetrievedCandidateMatches);
        return distinctValues(conceptMatchResults.stream()
            .filter(result -> relevantMatches.contains(result.match()))
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

    public int expectedRetrievalCount() {
        return expectedRequirementElements.size();
    }

    public List<CandidateConceptMatch> retrievedCandidateMatches() {
        return retrievedCandidateMatches;
    }

    public int expectedMatchDecisionCount() {
        return retrievedCandidateMatches.size();
    }

    public List<ConceptMatchResult> conceptMatchResults() {
        return conceptMatchResults;
    }

    public boolean isComplete() {
        return provenance.isPresent() &&
            classification.isPresent() &&
            action.isPresent() &&
            retrievedCandidateMatches.size() == expectedRetrievalCount() &&
            conceptMatchResults.size() == expectedMatchDecisionCount();
    }
}
