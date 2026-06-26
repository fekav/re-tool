package io.fekav.req.classification.application;

import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.shared.model.RawText;

public interface RequirementClassificationService {

    RequirementClassification classifyRequirement(RawText rawRequirementText);
}
