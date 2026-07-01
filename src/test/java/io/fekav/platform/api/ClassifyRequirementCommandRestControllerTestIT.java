package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
import io.fekav.req.shared.model.RawText;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class ClassifyRequirementCommandRestControllerTestIT {

    private static final String REQUIREMENT_TEXT =
        "The checkout service must support guest checkout.";
    private static final RequirementType CONCEPT_TYPE =
        RequirementType.REQUIREMENT;
    private static final RequirementProperty PROPERTY =
        RequirementProperty.FUNCTIONAL;
    private static final double CONFIDENCE_SCORE = 0.94;
    private static final String RATIONALE =
        "The text assigns a verifiable obligation to the service.";

    @InjectMock
    ClassificationService requirementClassificationService;

    ObjectMapper objectMapper;
    String requestBody;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(requirementClassificationService);
        when(requirementClassificationService.classifyRequirement(new RawText(REQUIREMENT_TEXT)))
            .thenReturn(classification(
                CONCEPT_TYPE,
                PROPERTY,
                CONFIDENCE_SCORE,
                RATIONALE
            ));
        requestBody = classifyRequirementCommandRequest(REQUIREMENT_TEXT);
    }

    @Test
    void returnsRawText_whenClassifyRequirementCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/rawText/text").asText()).isEqualTo(REQUIREMENT_TEXT);
    }

    @Test
    void returnsConceptType_whenClassifyRequirementCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/classification/conceptType").asText())
            .isEqualTo(CONCEPT_TYPE.name());
    }

    @Test
    void returnsProperty_whenClassifyRequirementCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/classification/property").asText())
            .isEqualTo(PROPERTY.name());
    }

    @Test
    void returnsConfidenceScore_whenClassifyRequirementCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/classification/confidenceScore/value").asDouble())
            .isEqualTo(CONFIDENCE_SCORE);
    }

    @Test
    void returnsRationale_whenClassifyRequirementCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/classification/rationale/text").asText()).isEqualTo(RATIONALE);
    }

    @Test
    void returnsGoalConceptType_whenServiceClassifiesGoal()
        throws Exception {
        String requirementText = "Make checkout better for returning customers.";
        when(requirementClassificationService.classifyRequirement(new RawText(requirementText)))
            .thenReturn(classification(
                RequirementType.GOAL,
                RequirementProperty.FUNCTIONAL,
                0.42,
                "The wording is ambiguous, so this is a forced best-fit classification."
            ));

        JsonNode result = executeCommandAsJson(
            classifyRequirementCommandRequest(requirementText)
        );

        assertThat(result.at("/classification/conceptType").asText())
            .isEqualTo(RequirementType.GOAL.name());
    }

    private JsonNode executeCommandAsJson(String requestBody) throws Exception {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    private String classifyRequirementCommandRequest(String requirementText)
        throws Exception {
        return commandRequest(
            objectMapper,
            "ClassifyRequirementCommand",
            Map.of("rawText", requirementText)
        );
    }

    private Classification classification(
        RequirementType conceptType,
        RequirementProperty property,
        double confidenceScore,
        String rationale
    ) {
        return new Classification(
            conceptType,
            property,
            new ConfidenceScore(confidenceScore),
            new Rationale(rationale)
        );
    }
}
