package io.fekav.req.search.application;

import io.fekav.platform.cqrs.QueryHandler;
import io.fekav.req.search.domain.RequirementsResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FindRequirementsQueryHandler
    implements QueryHandler<RequirementsResult, FindRequirementsQuery> {

    private final RequirementsFinder requirementsReader;

    @Inject
    public FindRequirementsQueryHandler(RequirementsFinder requirementsReader) {
        this.requirementsReader = requirementsReader;
    }

    @Override
    public RequirementsResult handle(FindRequirementsQuery query) {
        if (conceptMissing(query.concept())) {
            throw new IllegalArgumentException(
                "concept must not be blank"
            );
        }
        return requirementsReader.read(query);
    }

    @Override
    public Class<FindRequirementsQuery> queryType() {
        return FindRequirementsQuery.class;
    }

    private boolean conceptMissing(String concept) {
        return concept == null || concept.isBlank();
    }
}
