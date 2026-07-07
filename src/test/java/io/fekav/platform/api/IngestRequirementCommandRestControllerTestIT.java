package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.action;
import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.executeCommandAsJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.classification.application.ClassificationService;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.extraction.application.SyntaxExtraction;
import io.fekav.req.graphchange.application.PersistRequirementGraphChange;
import io.fekav.req.graphchange.application.RequirementGraphChangePort;
import io.fekav.req.graphchange.application.RequirementGraphChangeResult;
import io.fekav.req.resolution.domain.NodeMatchingResult;
import io.fekav.req.resolution.domain.NodeMatchingService;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.shared.model.RequirementElement;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@TestProfile(IngestRequirementCommandRestControllerTestIT.IngestionWorkflowProfile.class)
@Tag("integration")
class IngestRequirementCommandRestControllerTestIT {

    private static final String COMMAND = "IngestRequirementCommand";
    private static final String ORIGINAL_TEXT =
        "  The checkout service must support guest checkout.  ";

    @InjectMock
    ClassificationService classificationService;

    @InjectMock
    SyntaxExtraction syntaxExtraction;

    @InjectMock
    NodeRetrievalService nodeRetrievalService;

    @InjectMock
    NodeMatchingService nodeMatchingService;

    @InjectMock
    RequirementGraphChangePort graphChangePort;

    ObjectMapper objectMapper;
    String requestBody;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(
            classificationService,
            syntaxExtraction,
            nodeRetrievalService,
            nodeMatchingService,
            graphChangePort
        );
        when(classificationService.classifyRequirement(new RawText(ORIGINAL_TEXT)))
            .thenReturn(classification());
        when(syntaxExtraction.extractSyntax(new RawText(ORIGINAL_TEXT)))
            .thenReturn(action(
                "checkout service",
                "must support",
                "guest checkout",
                "",
                ""
            ));
        when(nodeRetrievalService.retrieveCandidates(any(RequirementElement.class)))
            .thenAnswer(invocation ->
                new CandidateNodeMatch(invocation.getArgument(0), List.of())
            );
        when(nodeMatchingService.evaluateMatch(any(CandidateNodeMatch.class)))
            .thenAnswer(invocation -> {
                CandidateNodeMatch match = invocation.getArgument(0);
                return NodeMatchingResult.decided(autoCreateDecision(
                    match.requirementElement()
                ));
            });
        when(graphChangePort.persist(any(PersistRequirementGraphChange.class)))
            .thenReturn(RequirementGraphChangeResult.created());
        requestBody = commandRequest(
            objectMapper,
            COMMAND,
            Map.of("originalText", ORIGINAL_TEXT)
        );
    }

    @Test
    void returnsCorrelationId_whenIngestRequirementCommandIsPosted() throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/correlationId/value").asText()).isNotBlank();
    }

    @Test
    void returnsRecordedStatus_whenRequirementIsRecorded() throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/status").asText()).isEqualTo("RECORDED");
    }

    @Test
    void returnsReasonMessage_whenRequirementIsRecorded() throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.at("/message").asText()).isEqualTo("Requirement recorded.");
    }

    @Test
    void omitsReceiptData_whenIngestRequirementCommandIsPosted()
        throws Exception {
        // Act
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        // Assert
        assertThat(result.has("eventId")).isFalse();
        assertThat(result.has("occurredAt")).isFalse();
        assertThat(result.has("provenance")).isFalse();
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The sentence expresses a verifiable obligation.")
        );
    }

    private NodeMatchDecision autoCreateDecision(RequirementElement element) {
        return new NodeMatchDecision(
            element,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    public static class IngestionWorkflowProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "req.orchestration.enabled",
                "true",
                "req.graphchange.enabled",
                "true"
            );
        }
    }
}
