package io.fekav.req.conceptmatching.application;

import java.util.List;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.event.ConceptMatchReviewRequestedEvent;
import io.fekav.req.shared.event.CreateConceptRequestedEvent;
import io.fekav.req.shared.event.ExistingConceptProposedEvent;
import io.fekav.req.shared.event.MapExistingConceptRequestedEvent;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DecideConceptMatchesCommandHandler
        implements CommandHandler<ConceptMatchDecisionSet, DecideConceptMatchesCommand> {

    private final EventPublisher eventPublisher;
    private final ConceptMatchingService conceptMatchingService;

    @Inject
    public DecideConceptMatchesCommandHandler(
        EventPublisher eventPublisher,
        ConceptMatchingService conceptMatchingService
    ) {
        this.eventPublisher = eventPublisher;
        this.conceptMatchingService = conceptMatchingService;
    }

    @Override
    public ConceptMatchDecisionSet handle(DecideConceptMatchesCommand command) {
        ConceptMatchDecisionSet decisions =
            conceptMatchingService.decideMatches(new CandidateConceptMatchSet(command.matches()));
        List<ApplicationEvent> events = decisions
            .decisions()
            .stream()
            .map(this::eventFor)
            .toList();

        eventPublisher.publishApplicationEvents(events);

        return decisions;
    }

    private ApplicationEvent eventFor(ConceptMatchDecision decision) {
        ConceptMatchDecisionStatus status = decision.status();
        return switch (status) {
            case AUTO_MAP_EXISTING -> MapExistingConceptRequestedEvent.create(decision);
            case PROPOSE_EXISTING -> ExistingConceptProposedEvent.create(decision);
            case REVIEW_REQUIRED -> ConceptMatchReviewRequestedEvent.create(decision);
            case AUTO_CREATE_NEW -> CreateConceptRequestedEvent.create(decision);
        };
    }

    @Override
    public Class<DecideConceptMatchesCommand> commandType() {
        return DecideConceptMatchesCommand.class;
    }
}
