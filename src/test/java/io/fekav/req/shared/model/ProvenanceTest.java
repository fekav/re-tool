package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ProvenanceTest {

    @Test
    void recordsOriginalTextSourceMetadataAndIngestionTime() {
        // Arrange
        ElementId id = ElementId.create();
        OriginalText originalText = new OriginalText("  The system shall export reports.  ");
        SourceMetadata sourceMetadata = SourceMetadata.apiRequest();
        Instant ingestedAt = Instant.parse("2026-07-02T10:15:30Z");

        // Act
        Provenance provenance = Provenance.create(
            id,
            originalText,
            sourceMetadata,
            ingestedAt
        );

        // Assert
        assertThat(provenance.getId()).isEqualTo(id);
        assertThat(provenance.getOriginalText()).isEqualTo(originalText);
        assertThat(provenance.getSourceMetadata()).isEqualTo(sourceMetadata);
        assertThat(provenance.getIngestedAt()).isEqualTo(ingestedAt);
    }

    @Test
    void rejectsMissingOriginalText() {
        // Act / Assert
        assertThatThrownBy(() -> Provenance.create(
            ElementId.create(),
            null,
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T10:15:30Z")
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("originalText must not be null");
    }
}
