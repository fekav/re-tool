package io.fekav.req.shared;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.fekav.req.shared.model.Requirement;
import io.fekav.req.shared.model.RequirementId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryRequirementRepository implements RequirementRepository{

    private final Map<RequirementId, Requirement> requirements = new ConcurrentHashMap<>();
 
    @Override
    public void save(Requirement requirement) {
        requirements.put(requirement.getId(), requirement);
    }
 
    @Override
    public Optional<Requirement> findById(RequirementId id) {
        return Optional.ofNullable(requirements.get(id));
    }
}
