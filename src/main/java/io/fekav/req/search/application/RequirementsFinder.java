package io.fekav.req.search.application;

import io.fekav.req.search.domain.RequirementsResult;

public interface RequirementsFinder {

    RequirementsResult read(FindRequirementsQuery query);
}
