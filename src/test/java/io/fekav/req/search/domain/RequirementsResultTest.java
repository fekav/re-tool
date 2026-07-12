package io.fekav.req.search.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RequirementsResultTest {

    @Test
    void copiesRequirementsAndRejectsExternalMutations() {
        // Given
        List<RequirementView> mutableRequirements = new ArrayList<>();
        mutableRequirements.add(new RequirementView(
            "The payment service must log failed transactions.",
            "REQUIREMENT",
            "FUNCTIONAL"
        ));

        // When
        RequirementsResult result = new RequirementsResult(mutableRequirements);
        mutableRequirements.add(new RequirementView(
            "The API should respond within 2 seconds.",
            "REQUIREMENT",
            "QUALITY"
        ));

        // Then
        assertThat(result.requirements()).hasSize(1);
    }

    @Test
    void rejectsNullRequirementsList() {
        assertThatThrownBy(() -> new RequirementsResult(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirements must not be null");
    }

    @Test
    void rejectsNullRequirementEntry() {
        List<RequirementView> requirements = new ArrayList<>();
        requirements.add(new RequirementView("text", "REQUIREMENT", "FUNCTIONAL"));
        requirements.add(null);

        assertThatThrownBy(() -> new RequirementsResult(requirements))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirements must not contain null");
    }
}
