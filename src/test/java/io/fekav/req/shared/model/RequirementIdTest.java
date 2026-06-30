package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequirementIdTest {

    @Test
    void createsRequirementIdWithUuidValue() {
        RequirementId requirementId = RequirementId.create();

        assertThat(requirementId.value()).isNotNull();
    }
}
