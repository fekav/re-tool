package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
    void returnsConceptType_whenClassifyRequirementCommandIsPosted() {
        Classification result = executeCommand(requestBody);

        assertThat(result.conceptType()).isEqualTo(CONCEPT_TYPE);
    }

    @Test
    void returnsProperty_whenClassifyRequirementCommandIsPosted() {
        Classification result = executeCommand(requestBody);

        assertThat(result.property()).isEqualTo(PROPERTY);
    }

    @Test
    void returnsConfidenceScore_whenClassifyRequirementCommandIsPosted() {
        Classification result = executeCommand(requestBody);

        assertThat(result.confidenceScore())
            .isEqualTo(new ConfidenceScore(CONFIDENCE_SCORE));
    }

    @Test
    void returnsRationale_whenClassifyRequirementCommandIsPosted() {
        Classification result = executeCommand(requestBody);

        assertThat(result.rationale()).isEqualTo(new Rationale(RATIONALE));
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

        Classification result = executeCommand(
            classifyRequirementCommandRequest(requirementText)
        );

        assertThat(result.conceptType()).isEqualTo(RequirementType.GOAL);
    }

    private Classification executeCommand(String requestBody) {
        return postCommandForResponse(requestBody, Classification.class);
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
