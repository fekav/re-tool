package io.fekav.req.conceptretrieval.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetrieveCandidateConceptsCommandHandler
        implements CommandHandler<CandidateConceptMatchSet, RetrieveCandidateConceptsCommand> {

    private final ConceptRetrievalService conceptRetrievalService;

    @Inject
    public RetrieveCandidateConceptsCommandHandler(
        ConceptRetrievalService conceptRetrievalService
    ) {
        this.conceptRetrievalService = conceptRetrievalService;
    }

    @Override
    public CandidateConceptMatchSet handle(RetrieveCandidateConceptsCommand command) {
        return conceptRetrievalService.retrieveCandidates(command.selectedTerms());
    }

    @Override
    public Class<RetrieveCandidateConceptsCommand> commandType() {
        return RetrieveCandidateConceptsCommand.class;
    }
}
