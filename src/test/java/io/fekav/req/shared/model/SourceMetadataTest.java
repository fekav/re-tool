package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceMetadataTest {

    @Test
    void apiRequestIdentifiesApiRequestSource() {
        // Act
        SourceMetadata sourceMetadata = SourceMetadata.apiRequest();

        // Assert
        assertThat(sourceMetadata.sourceName().value()).isEqualTo("API-Request");
    }

    @Test
    void throwsInvalidSourceMetadata_whenSourceNameIsBlank() {
        // Act / Assert
        assertThatThrownBy(() -> new SourceName(" "))
            .isInstanceOf(InvalidSourceMetadataException.class)
            .hasMessage("source name must not be blank");
    }
}
