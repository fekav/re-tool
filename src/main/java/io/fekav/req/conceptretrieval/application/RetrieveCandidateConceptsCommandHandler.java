package io.fekav.req.conceptretrieval.application;

import java.util.List;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptCandidatesReadyEvent;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetrieveCandidateConceptsCommandHandler
        implements CommandHandler<CandidateConceptMatchSet, RetrieveCandidateConceptsCommand> {

    private final EventPublisher eventPublisher;
    private final ConceptRetrievalService conceptRetrievalService;

    @Inject
    public RetrieveCandidateConceptsCommandHandler(
        EventPublisher eventPublisher,
        ConceptRetrievalService conceptRetrievalService
    ) {
        this.eventPublisher = eventPublisher;
        this.conceptRetrievalService = conceptRetrievalService;
    }

    @Override
    public CandidateConceptMatchSet handle(RetrieveCandidateConceptsCommand command) {
        CandidateConceptMatchSet matches =
            conceptRetrievalService.retrieveCandidates(command.selectedTerms());
        List<ApplicationEvent> events = matches
            .matches()
            .stream()
            .map(ConceptCandidatesReadyEvent::create)
            .map(ApplicationEvent.class::cast)
            .toList();

        eventPublisher.publishApplicationEvents(events);

        return matches;
    }

    @Override
    public Class<RetrieveCandidateConceptsCommand> commandType() {
        return RetrieveCandidateConceptsCommand.class;
    }
}
