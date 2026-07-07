package io.fekav.req.extraction.application;

import io.fekav.req.extraction.domain.Action;
import io.fekav.req.shared.model.RawText;

public interface SyntaxExtraction {

    Action extractSyntax(RawText rawRequirementText);
}
