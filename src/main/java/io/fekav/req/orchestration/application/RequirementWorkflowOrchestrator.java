package io.fekav.req.orchestration.application;

import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.application.ClassifyRequirementCommand;
import io.fekav.req.conceptmatching.application.EvaluateConceptMatchCommand;
import io.fekav.req.conceptretrieval.application.RetrieveCandidateConceptsCommand;
import io.fekav.req.orchestration.domain.RequirementElementCollector;
import io.fekav.req.orchestration.domain.WorkflowState;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.CorrelationId;
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
    private final RequirementElementCollector requirementElementCollector;
    private final boolean enabled;

    @Inject
    public RequirementWorkflowOrchestrator(
        EventStore eventStore,
        CommandBus commandBus,
        EventPublisher eventPublisher,
        @ConfigProperty(name = "req.orchestration.enabled", defaultValue = "false")
        boolean enabled
    ) {
        this(
            eventStore,
            commandBus,
            eventPublisher,
            enabled,
            new RequirementElementCollector()
        );
    }

    RequirementWorkflowOrchestrator(
        EventStore eventStore,
        CommandBus commandBus,
        EventPublisher eventPublisher
    ) {
        this(eventStore, commandBus, eventPublisher, true, new RequirementElementCollector());
    }

    RequirementWorkflowOrchestrator(
        EventStore eventStore,
        CommandBus commandBus,
        EventPublisher eventPublisher,
        boolean enabled,
        RequirementElementCollector requirementElementCollector
    ) {
        this.eventStore = eventStore;
        this.commandBus = commandBus;
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
        this.requirementElementCollector = requirementElementCollector;
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
            requirementElementCollector.collectFrom(event.action())
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
            commandBus.dispatch(new EvaluateConceptMatchCommand(
                event.correlationId(),
                event.match()
            ));
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
        boolean stored = eventStore.appendIfAbsent(correlationId, event);
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

    private record WorkflowTransition(
        boolean stored,
        WorkflowState before,
        WorkflowState after
    ) {
    }
}
