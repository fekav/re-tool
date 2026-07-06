package io.fekav.req.ingestion.application;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.fekav.platform.messaging.CorrelationId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IngestionOutcomeStore {

    private final ConcurrentMap<CorrelationId, IngestRequirementResult> outcomes =
        new ConcurrentHashMap<>();

    public void record(IngestRequirementResult result) {
        Objects.requireNonNull(result, "result must not be null");
        outcomes.putIfAbsent(result.correlationId(), result);
    }

    public Optional<IngestRequirementResult> consume(CorrelationId correlationId) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        return Optional.ofNullable(outcomes.remove(correlationId));
    }
}
