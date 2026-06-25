package io.fekav.req.classification.application;

import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.shared.model.RawRequirementText;

public interface RequirementClassificationService {

    RequirementClassification classifyRequirement(RawRequirementText rawRequirementText);
}
