package io.fekav.req.shared;

import java.util.Optional;

import io.fekav.req.shared.model.Requirement;
import io.fekav.req.shared.model.RequirementId;

/**
 * Outbound Port
 */
public interface RequirementRepository {
    void save(Requirement requirement);
    Optional<Requirement> findById(RequirementId id);
}
