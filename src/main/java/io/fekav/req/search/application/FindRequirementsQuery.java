package io.fekav.req.queryrequirements.application;

import io.fekav.platform.cqrs.Query;
import io.fekav.req.queryrequirements.domain.RequirementsResult;

public record FindRequirementsQuery(
    String concept
) implements Query<RequirementsResult> {

    public FindRequirementsQuery {
        concept = concept == null ? null : concept.strip();
    }
}
