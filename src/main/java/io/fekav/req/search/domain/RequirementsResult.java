package io.fekav.req.search.domain;

import java.util.List;
import java.util.Objects;

public record RequirementsResult(List<RequirementView> requirements) {

    public RequirementsResult {
        Objects.requireNonNull(requirements, "requirements must not be null");
        if (requirements.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("requirements must not contain null");
        }
        requirements = List.copyOf(requirements);
    }
}
