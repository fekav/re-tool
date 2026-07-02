package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.executeCommandAsJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class IngestRequirementCommandRestControllerTestIT {

    private static final String COMMAND = "IngestRequirementCommand";
    private static final String ORIGINAL_TEXT =
        "  The checkout service must support guest checkout.  ";

    ObjectMapper objectMapper;
    String requestBody;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        requestBody = commandRequest(
            objectMapper,
            COMMAND,
            Map.of("originalText", ORIGINAL_TEXT)
        );
    }

    @Test
    void returnsOriginalText_whenIngestRequirementCommandIsPosted() throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/provenance/originalText/text").asText())
            .isEqualTo(ORIGINAL_TEXT);
    }

    @Test
    void returnsApiRequestSource_whenIngestRequirementCommandIsPosted() throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/provenance/sourceMetadata/sourceName/value").asText())
            .isEqualTo("API-Request");
    }

    @Test
    void returnsSameOccurrenceTimeAsProvenance_whenIngestRequirementCommandIsPosted()
        throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/occurredAt").asText())
            .isEqualTo(result.at("/provenance/ingestedAt").asText());
    }

    @Test
    void omitsTransientRequirementData_whenIngestRequirementCommandIsPosted()
        throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.has("requirementId")).isFalse();
        assertThat(result.has("rawText")).isFalse();
    }
}
