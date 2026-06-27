package io.fekav.req.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequirementIngestionTest {

    @Test
    void createsRequirementIngestionAndRaisesEvent_whenInputIsValid() {
        SourceProvenance provenance = new SourceProvenance("API_REQUEST", "req-123");

        RequirementIngestion ingestion = RequirementIngestion.create(
            provenance,
            "  The system shall export reports.  ",
            "The system shall export reports."
        );

        assertThat(ingestion.id()).isNotNull();
        assertThat(ingestion.provenance()).isEqualTo(provenance);
        assertThat(ingestion.originalText()).isEqualTo("  The system shall export reports.  ");
        assertThat(ingestion.normalizedText()).isEqualTo("The system shall export reports.");
        assertThat(ingestion.domainEvents())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(RequirementIngestedEvent.class);
                RequirementIngestedEvent event = (RequirementIngestedEvent) domainEvent;
                assertThat(event.ingestionId()).isEqualTo(ingestion.id());
                assertThat(event.provenance()).isEqualTo(provenance);
                assertThat(event.originalText()).isEqualTo(ingestion.originalText());
                assertThat(event.normalizedText()).isEqualTo(ingestion.normalizedText());
            });
    }

    @Test
    void throwsInvalidRequirementIngestionException_whenOriginalTextIsBlank() {
        SourceProvenance provenance = new SourceProvenance("API_REQUEST", "req-123");

        assertThatThrownBy(() -> RequirementIngestion.create(provenance, " ", "normalized"))
            .isInstanceOf(InvalidRequirementIngestionException.class)
            .hasMessage("original text must not be blank");
    }

    @Test
    void throwsInvalidRequirementIngestionException_whenNormalizedTextIsBlank() {
        SourceProvenance provenance = new SourceProvenance("API_REQUEST", "req-123");

        assertThatThrownBy(() -> RequirementIngestion.create(provenance, "raw", " "))
            .isInstanceOf(InvalidRequirementIngestionException.class)
            .hasMessage("normalized text must not be blank");
    }
}
