package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.classification.application.RequirementClassificationService;
import io.fekav.req.classification.domain.ClassificationRationale;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.classification.domain.RequirementConceptType;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.entityextraction.application.RequirementSyntaxExtraction;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.entityextraction.domain.RequirementSyntaxType;
import io.fekav.req.shared.model.RawRequirementText;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class RestControllerTestIT {

    @InjectMock
    RequirementSyntaxExtraction requirementSyntaxExtraction;

    @InjectMock
    RequirementClassificationService requirementClassificationService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reset(requirementSyntaxExtraction);
        reset(requirementClassificationService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("requirementTexts")
    void returnsExtractedEntities_whenExtractEntitiesCommandIsPosted(
        String requirementText,
        String subject,
        String action,
        String targetObject,
        String constraint,
        String condition
    ) throws Exception {
        // Given
        when(requirementSyntaxExtraction.extractRequirementSyntax(new RawRequirementText(requirementText)))
            .thenReturn(requirementSyntax(subject, action, targetObject, constraint, condition));

        // When
        RequirementSyntax result =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(commandRequest(requirementText))
            .when()
                .post("/c")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(RequirementSyntax.class);

        // Then
        assertThat(result.syntaxElements())
            .containsEntry(RequirementSyntaxType.SUBJECT, subject)
            .containsEntry(RequirementSyntaxType.ACTION, action)
            .containsEntry(RequirementSyntaxType.OBJECT, targetObject)
            .containsEntry(RequirementSyntaxType.CONSTRAINT, constraint)
            .containsEntry(RequirementSyntaxType.CONDITION, condition);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("classificationRequirementTexts")
    void returnsRequirementClassification_whenClassifyRequirementCommandIsPosted(
        String requirementText,
        RequirementConceptType conceptType,
        RequirementProperty property,
        double confidenceScore,
        String rationale
    ) throws Exception {
        RequirementClassification classification = new RequirementClassification(
            conceptType,
            property,
            new ConfidenceScore(confidenceScore),
            new ClassificationRationale(rationale)
        );
        when(requirementClassificationService.classifyRequirement(new RawRequirementText(requirementText)))
            .thenReturn(classification);

        RequirementClassification result =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(classificationCommandRequest(requirementText))
            .when()
                .post("/c")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(RequirementClassification.class);

        assertThat(result.conceptType()).isEqualTo(conceptType);
        assertThat(result.property()).isEqualTo(property);
        assertThat(result.confidenceScore()).isEqualTo(new ConfidenceScore(confidenceScore));
        assertThat(result.rationale()).isEqualTo(new ClassificationRationale(rationale));
    }

    static Stream<Arguments> requirementTexts() {
        return Stream.of(
            Arguments.of(
                "If a customer cancels an order before shipment, the commerce system must refund the payment within 24 hours.",
                "commerce system",
                "must refund",
                "payment",
                "within 24 hours",
                "customer cancels an order before shipment"
            ),
            Arguments.of(
                "The reporting dashboard shall export monthly usage metrics as a CSV file.",
                "reporting dashboard",
                "shall export",
                "monthly usage metrics",
                "as a CSV file",
                ""
            ),
            Arguments.of(
                "When sensor temperature exceeds 80 degrees Celsius, the monitoring service must notify the operator immediately.",
                "monitoring service",
                "must notify",
                "operator",
                "immediately",
                "sensor temperature exceeds 80 degrees Celsius"
            )
        );
    }

    static Stream<Arguments> classificationRequirementTexts() {
        return Stream.of(
            Arguments.of(
                "The checkout service must support guest checkout.",
                RequirementConceptType.REQUIREMENT,
                RequirementProperty.FUNCTIONAL,
                0.94,
                "The text assigns a verifiable obligation to the service."
            ),
            Arguments.of(
                "Make checkout better for returning customers.",
                RequirementConceptType.GOAL,
                RequirementProperty.FUNCTIONAL,
                0.42,
                "The wording is ambiguous, so this is a forced best-fit classification."
            )
        );
    }

    private String commandRequest(String requirementText) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "command",
            "ExtractEntitiesCommand",
            "payload",
            Map.of("rawText", requirementText)
        ));
    }

    private String classificationCommandRequest(String requirementText) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "command",
            "ClassifyRequirementCommand",
            "payload",
            Map.of("rawText", requirementText)
        ));
    }

    private RequirementSyntax requirementSyntax(
        String subject,
        String action,
        String targetObject,
        String constraint,
        String condition
    ) {
        return new RequirementSyntax(Map.of(
            RequirementSyntaxType.SUBJECT,
            subject,
            RequirementSyntaxType.ACTION,
            action,
            RequirementSyntaxType.OBJECT,
            targetObject,
            RequirementSyntaxType.CONSTRAINT,
            constraint,
            RequirementSyntaxType.CONDITION,
            condition
        ));
    }

}
