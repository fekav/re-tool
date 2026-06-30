package io.fekav.req.conceptmatching.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DecideConceptMatchesCommandHandler
        implements CommandHandler<ConceptMatchDecisionSet, DecideConceptMatchesCommand> {

    private final ConceptMatchingService conceptMatchingService;

    @Inject
    public DecideConceptMatchesCommandHandler(
        ConceptMatchingService conceptMatchingService
    ) {
        this.conceptMatchingService = conceptMatchingService;
    }

    @Override
    public ConceptMatchDecisionSet handle(DecideConceptMatchesCommand command) {
        return conceptMatchingService.decideMatches(new CandidateConceptMatchSet(command.matches()));
    }

    @Override
    public Class<DecideConceptMatchesCommand> commandType() {
        return DecideConceptMatchesCommand.class;
    }
}
