package io.fekav.req.orchestration.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.application.ClassifyRequirementCommand;
import io.fekav.req.conceptmatching.application.EvaluateConceptMatchCommand;
import io.fekav.req.conceptretrieval.application.RetrieveCandidateConceptsCommand;
import io.fekav.req.orchestration.domain.WorkflowState;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.syntaxextraction.application.ExtractSyntaxCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RequirementWorkflowOrchestrator {

    private final EventStore eventStore;
    private final CommandBus commandBus;
    private final EventPublisher eventPublisher;
    private final boolean enabled;

    @Inject
    public RequirementWorkflowOrchestrator(
        EventStore eventStore,
        CommandBus commandBus,
        EventPublisher eventPublisher,
        @ConfigProperty(name = "req.orchestration.enabled", defaultValue = "false")
        boolean enabled
    ) {
        this.eventStore = eventStore;
        this.commandBus = commandBus;
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
    }

    RequirementWorkflowOrchestrator(
        EventStore eventStore,
        CommandBus commandBus,
        EventPublisher eventPublisher
    ) {
        this(eventStore, commandBus, eventPublisher, true);
    }

    public void onRequirementIngested(@Observes RequirementIngestedEvent event) {
        if (!enabled) {
            return;
        }

        WorkflowTransition transition = remember(event.correlationId(), event);

        if (transition.stored()) {
            String rawText = event.provenance().getOriginalText().text();
            commandBus.dispatch(new ClassifyRequirementCommand(event.correlationId(), rawText));
            commandBus.dispatch(new ExtractSyntaxCommand(event.correlationId(), rawText));
        }
    }

    public void onRequirementClassified(@Observes RequirementClassifiedEvent event) {
        if (!enabled) {
            return;
        }

        WorkflowTransition transition = remember(event.correlationId(), event);

        publishCompletionIfAnalysisCompleted(event.correlationId(), transition);
    }

    public void onRequirementElementsExtracted(
        @Observes RequirementElementsExtractedEvent event
    ) {
        if (!enabled) {
            return;
        }

        WorkflowTransition transition = remember(event.correlationId(), event);

        if (transition.stored()) {
            newlyExpectedRequirementElements(transition)
                .forEach(element -> commandBus.dispatch(
                    new RetrieveCandidateConceptsCommand(event.correlationId(), element)
                ));
        }
    }

    public void onConceptCandidatesRetrieved(
        @Observes ConceptCandidatesRetrievedEvent event
    ) {
        if (!enabled) {
            return;
        }

        WorkflowTransition transition = remember(event.correlationId(), event);

        if (transition.stored()) {
            newlyRetrievedCandidateMatches(transition)
                .forEach(match -> commandBus.dispatch(new EvaluateConceptMatchCommand(
                    event.correlationId(),
                    match
                )));
        }
    }

    public void onConceptMatchEvaluated(@Observes ConceptMatchEvaluatedEvent event) {
        if (!enabled) {
            return;
        }

        WorkflowTransition transition = remember(event.correlationId(), event);

        publishCompletionIfAnalysisCompleted(event.correlationId(), transition);
    }

    private WorkflowTransition remember(
        CorrelationId correlationId,
        ApplicationEvent event
    ) {
        WorkflowState before = WorkflowState.replay(eventStore.load(correlationId));
        boolean stored = eventStore.appendIfAbsent(event);
        WorkflowState after = stored
            ? WorkflowState.replay(eventStore.load(correlationId))
            : before;

        return new WorkflowTransition(stored, before, after);
    }

    private void publishCompletionIfAnalysisCompleted(
        CorrelationId correlationId,
        WorkflowTransition transition
    ) {
        if (
            transition.stored() &&
                !transition.before().isComplete() &&
                transition.after().isComplete()
        ) {
            WorkflowState completedState = transition.after();
            eventPublisher.publish(RequirementAnalysisCompletedEvent.create(
                correlationId,
                completedState.provenance().orElseThrow(),
                completedState.classification().orElseThrow(),
                completedState.action().orElseThrow(),
                completedState.conceptMatchResults()
            ));
        }
    }

    private List<RequirementElement> newlyExpectedRequirementElements(
        WorkflowTransition transition
    ) {
        return valuesAdded(
            transition.before().expectedRequirementElements(),
            transition.after().expectedRequirementElements()
        );
    }

    private List<CandidateConceptMatch> newlyRetrievedCandidateMatches(
        WorkflowTransition transition
    ) {
        return valuesAdded(
            transition.before().retrievedCandidateMatches(),
            transition.after().retrievedCandidateMatches()
        );
    }

    private static <T> List<T> valuesAdded(List<T> before, List<T> after) {
        Set<T> previousValues = new LinkedHashSet<>(before);
        return after.stream()
            .filter(value -> !previousValues.contains(value))
            .toList();
    }

    private record WorkflowTransition(
        boolean stored,
        WorkflowState before,
        WorkflowState after
    ) {
    }
}
