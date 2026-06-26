package io.fekav.req.classification.application;

import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.RawText;

public interface ClassificationService {

    Classification classifyRequirement(RawText rawRequirementText);
}
