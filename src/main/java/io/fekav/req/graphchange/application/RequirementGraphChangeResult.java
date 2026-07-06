package io.fekav.req.graphchange.application;

import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.ingestion.application.IngestRequirementResult;
import io.fekav.req.shared.event.RequirementKnownEvent;

public sealed interface RequirementGraphChangeResult
    permits
        RequirementGraphChangeResult.RequirementCreated,
        RequirementGraphChangeResult.RequirementKnown,
        RequirementGraphChangeResult.RequirementUnchanged {

    void publishFollowUp(CorrelationId correlationId, EventPublisher eventPublisher);

    IngestRequirementResult toIngestionResult(CorrelationId correlationId);

    static RequirementGraphChangeResult created() {
        return new RequirementCreated();
    }

    static RequirementGraphChangeResult known(AssertionIdentity assertionIdentity) {
        return new RequirementKnown(assertionIdentity);
    }

    static RequirementGraphChangeResult unchanged() {
        return new RequirementUnchanged();
    }

    record RequirementCreated() implements RequirementGraphChangeResult {

        @Override
        public void publishFollowUp(
            CorrelationId correlationId,
            EventPublisher eventPublisher
        ) {
        }

        @Override
        public IngestRequirementResult toIngestionResult(CorrelationId correlationId) {
            return IngestRequirementResult.recorded(correlationId);
        }
    }

    record RequirementKnown(AssertionIdentity assertionIdentity)
        implements RequirementGraphChangeResult {

        public RequirementKnown {
            Objects.requireNonNull(
                assertionIdentity,
                "assertionIdentity must not be null"
            );
        }

        @Override
        public void publishFollowUp(
            CorrelationId correlationId,
            EventPublisher eventPublisher
        ) {
            Objects.requireNonNull(correlationId, "correlationId must not be null");
            Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
            eventPublisher.publish(RequirementKnownEvent.create(
                correlationId,
                assertionIdentity.subject(),
                assertionIdentity.predicate(),
                assertionIdentity.object()
            ));
        }

        @Override
        public IngestRequirementResult toIngestionResult(CorrelationId correlationId) {
            return IngestRequirementResult.alreadyExists(correlationId);
        }
    }

    record RequirementUnchanged() implements RequirementGraphChangeResult {

        @Override
        public void publishFollowUp(
            CorrelationId correlationId,
            EventPublisher eventPublisher
        ) {
        }

        @Override
        public IngestRequirementResult toIngestionResult(CorrelationId correlationId) {
            return IngestRequirementResult.alreadyExists(correlationId);
        }
    }
}
