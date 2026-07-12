package io.fekav.req.search.application;

import io.fekav.platform.cqrs.Query;
import io.fekav.req.search.domain.RequirementsResult;

public record FindRequirementsQuery(
    String concept
) implements Query<RequirementsResult> {

    public FindRequirementsQuery {
        concept = concept == null ? null : concept.strip();
    }
}
