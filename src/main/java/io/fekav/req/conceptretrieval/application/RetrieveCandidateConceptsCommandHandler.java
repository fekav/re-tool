package io.fekav.req.conceptretrieval.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetrieveCandidateConceptsCommandHandler
        implements CommandHandler<ConceptCandidatesRetrievedEvent, RetrieveCandidateConceptsCommand> {

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
    public ConceptCandidatesRetrievedEvent handle(RetrieveCandidateConceptsCommand command) {
        CandidateConceptMatch match =
            conceptRetrievalService.retrieveCandidates(command.selectedTerm());
        ConceptCandidatesRetrievedEvent event = ConceptCandidatesRetrievedEvent.create(match);

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<RetrieveCandidateConceptsCommand> commandType() {
        return RetrieveCandidateConceptsCommand.class;
    }
}
