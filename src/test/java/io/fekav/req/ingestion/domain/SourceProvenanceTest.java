package io.fekav.req.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceProvenanceTest {

    @Test
    void storesTrimmedSourceValues_whenValuesHaveSurroundingWhitespace() {
        SourceProvenance provenance = new SourceProvenance(" API_REQUEST ", " req-123 ");

        assertThat(provenance.sourceType()).isEqualTo("API_REQUEST");
        assertThat(provenance.sourceId()).isEqualTo("req-123");
    }

    @Test
    void throwsInvalidRequirementIngestionException_whenSourceTypeIsBlank() {
        assertThatThrownBy(() -> new SourceProvenance(" ", "req-123"))
            .isInstanceOf(InvalidRequirementIngestionException.class)
            .hasMessage("source type must not be blank");
    }

    @Test
    void throwsInvalidRequirementIngestionException_whenSourceIdIsBlank() {
        assertThatThrownBy(() -> new SourceProvenance("API_REQUEST", " "))
            .isInstanceOf(InvalidRequirementIngestionException.class)
            .hasMessage("source id must not be blank");
    }
}
