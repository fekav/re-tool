package io.fekav.req.entityextraction.application;

import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.shared.model.RawRequirementText;

public interface RequirementSyntaxExtraction {

    RequirementSyntax extractRequirementSyntax(RawRequirementText rawRequirementText);
}
