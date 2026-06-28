package io.fekav.req.conceptretrieval.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetrieveCandidateConceptsCommandHandler
        implements CommandHandler<CandidateConceptMatchSet, RetrieveCandidateConceptsCommand> {

    private final ConceptRetrievalPolicy conceptRetrievalPolicy;

    @Inject
    public RetrieveCandidateConceptsCommandHandler(
        ConceptRetrievalPolicy conceptRetrievalPolicy
    ) {
        this.conceptRetrievalPolicy = conceptRetrievalPolicy;
    }

    @Override
    public CandidateConceptMatchSet handle(
        RetrieveCandidateConceptsCommand command
    ) {
        return conceptRetrievalPolicy.retrieveCandidates(command.selectedTerms());
    }

    @Override
    public Class<RetrieveCandidateConceptsCommand> commandType() {
        return RetrieveCandidateConceptsCommand.class;
    }
}
