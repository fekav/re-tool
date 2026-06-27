package io.fekav.req.syntaxextraction.application;

import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.Action;

public interface SyntaxExtraction {

    Action extractSyntax(RawText rawRequirementText);
}
