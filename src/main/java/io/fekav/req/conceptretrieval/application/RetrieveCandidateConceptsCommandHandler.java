package io.fekav.req.conceptretrieval.application;

import java.util.List;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;
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
        List<SelectedTerm> selectedTerms = command
            .selectedTerms()
            .stream()
            .map(selectedTerm -> new SelectedTerm(
                selectedTerm.syntaxRole(),
                selectedTerm.text()
            ))
            .toList();
        return conceptRetrievalService.retrieveCandidates(selectedTerms);
    }

    @Override
    public Class<RetrieveCandidateConceptsCommand> commandType() {
        return RetrieveCandidateConceptsCommand.class;
    }
}
