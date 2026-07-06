package io.fekav.req.graphchange.application;

public interface RequirementGraphChangePort {

    RequirementGraphChangeResult persist(PersistRequirementGraphChange graphChange);
}
