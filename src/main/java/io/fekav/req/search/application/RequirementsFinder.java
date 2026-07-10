package io.fekav.req.queryrequirements.application;

import io.fekav.req.queryrequirements.domain.RequirementsResult;

public interface RequirementsFinder {

    RequirementsResult read(FindRequirementsQuery query);
}
