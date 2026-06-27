package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fekav.req.syntaxextraction.domain.MissingRequirementSyntaxElementException;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;
import io.fekav.req.syntaxextraction.domain.RequirementSyntaxType;

class RequirementSyntaxTest {

    @Test
    void acceptsRequirementSyntax_whenSubjectActionAndObjectArePresent() {
        RequirementSyntax requirementSyntax = new RequirementSyntax(Map.of(
            RequirementSyntaxType.SUBJECT, "reporting dashboard",
            RequirementSyntaxType.ACTION, "shall export",
            RequirementSyntaxType.OBJECT, "monthly usage metrics"
        ));

        assertThat(requirementSyntax.syntaxElements())
            .containsEntry(RequirementSyntaxType.SUBJECT, "reporting dashboard")
            .containsEntry(RequirementSyntaxType.ACTION, "shall export")
            .containsEntry(RequirementSyntaxType.OBJECT, "monthly usage metrics")
            .doesNotContainKey(RequirementSyntaxType.CONSTRAINT)
            .doesNotContainKey(RequirementSyntaxType.CONDITION);
    }

    @Test
    void throwsMissingRequirementSyntaxElement_whenSubjectIsMissing() {
        Map<RequirementSyntaxType, String> syntaxElements = new EnumMap<>(RequirementSyntaxType.class);
        syntaxElements.put(RequirementSyntaxType.ACTION, "shall export");
        syntaxElements.put(RequirementSyntaxType.OBJECT, "monthly usage metrics");

        assertThatThrownBy(() -> new RequirementSyntax(syntaxElements))
            .isInstanceOf(MissingRequirementSyntaxElementException.class)
            .hasMessage("requirement syntax subject is required");
    }

    @Test
    void throwsMissingRequirementSyntaxElement_whenActionIsBlank() {
        Map<RequirementSyntaxType, String> syntaxElements = new EnumMap<>(RequirementSyntaxType.class);
        syntaxElements.put(RequirementSyntaxType.SUBJECT, "reporting dashboard");
        syntaxElements.put(RequirementSyntaxType.ACTION, " ");
        syntaxElements.put(RequirementSyntaxType.OBJECT, "monthly usage metrics");

        assertThatThrownBy(() -> new RequirementSyntax(syntaxElements))
            .isInstanceOf(MissingRequirementSyntaxElementException.class)
            .hasMessage("requirement syntax action is required");
    }

    @Test
    void throwsMissingRequirementSyntaxElement_whenObjectIsMissing() {
        Map<RequirementSyntaxType, String> syntaxElements = new EnumMap<>(RequirementSyntaxType.class);
        syntaxElements.put(RequirementSyntaxType.SUBJECT, "reporting dashboard");
        syntaxElements.put(RequirementSyntaxType.ACTION, "shall export");

        assertThatThrownBy(() -> new RequirementSyntax(syntaxElements))
            .isInstanceOf(MissingRequirementSyntaxElementException.class)
            .hasMessage("requirement syntax object is required");
    }
}
