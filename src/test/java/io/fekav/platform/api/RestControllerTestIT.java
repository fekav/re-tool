package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.conceptretrieval.domain.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatch;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.conceptretrieval.domain.RetrievedCandidateConcept;
import io.fekav.req.conceptretrieval.domain.RetrievalEvidence;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;
import io.fekav.req.classification.application.ClassificationService;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.application.ExtractSyntaxResponse;
import io.fekav.req.syntaxextraction.application.SyntaxExtraction;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class RestControllerTestIT {

    @InjectMock
    SyntaxExtraction syntaxExtraction;

    @InjectMock
    ClassificationService requirementClassificationService;

    @InjectMock
    ConceptRetrievalService conceptRetrievalService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reset(syntaxExtraction);
        reset(requirementClassificationService);
        reset(conceptRetrievalService);
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
        when(syntaxExtraction.extractSyntax(new RawText(requirementText)))
            .thenReturn(action(subject, action, targetObject, constraint, condition));

        // When
        ExtractSyntaxResponse result =
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
                .as(ExtractSyntaxResponse.class);

        // Then
        assertThat(result.syntaxElements().SUBJECT()).isEqualTo(subject);
        assertThat(result.syntaxElements().ACTION()).isEqualTo(action);
        assertThat(result.syntaxElements().OBJECT()).isEqualTo(targetObject);
        assertThat(result.syntaxElements().CONSTRAINT()).isEqualTo(responseSet(constraint));
        assertThat(result.syntaxElements().CONDITION()).isEqualTo(responseSet(condition));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("classificationRequirementTexts")
    void returnsRequirementClassification_whenClassifyRequirementCommandIsPosted(
        String requirementText,
        RequirementType conceptType,
        RequirementProperty property,
        double confidenceScore,
        String rationale
    ) throws Exception {
        Classification classification = new Classification(
            conceptType,
            property,
            new ConfidenceScore(confidenceScore),
            new Rationale(rationale)
        );
        when(requirementClassificationService.classifyRequirement(new RawText(requirementText)))
            .thenReturn(classification);

        Classification result =
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
                .as(Classification.class);

        assertThat(result.conceptType()).isEqualTo(conceptType);
        assertThat(result.property()).isEqualTo(property);
        assertThat(result.confidenceScore()).isEqualTo(new ConfidenceScore(confidenceScore));
        assertThat(result.rationale()).isEqualTo(new Rationale(rationale));
    }

    @Test
    void returnsCandidateConceptMatches_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        // Given
        RetrievalEvidence subjectEvidence = new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'billing service' to graph candidate 'billing service'",
            1.0
        );
        RetrievalEvidence actionEvidence = new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'must refund' to graph candidate 'Refund Service'",
            1.0
        );
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(List.of(
            new CandidateConceptMatch(
                new SelectedTerm("SUBJECT", "billing service"),
                List.of(new RetrievedCandidateConcept(
                    new CandidateConcept(
                        "sample-syntax-requirement-1-subject",
                        "billing service",
                        "SyntaxElement"
                    ),
                    List.of(subjectEvidence)
                ))
            ),
            new CandidateConceptMatch(
                new SelectedTerm("ACTION", "must refund"),
                List.of(new RetrievedCandidateConcept(
                    new CandidateConcept(
                        "sample-action-refund",
                        "Refund Service",
                        "SystemComponent"
                    ),
                    List.of(actionEvidence)
                ))
            ),
            new CandidateConceptMatch(
                new SelectedTerm("OBJECT", "unknown workflow"),
                List.of()
            )
        ));
        when(conceptRetrievalService.retrieveCandidates(anyCollection()))
            .thenReturn(matchSet);

        // When
        String responseBody =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(retrieveCandidateConceptsCommandRequest())
            .when()
                .post("/c")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .asString();
        CandidateConceptMatchSet result =
            objectMapper.readValue(responseBody, CandidateConceptMatchSet.class);
        JsonNode responseJson = objectMapper.readTree(responseBody);

        // Then
        assertThat(result.matches()).hasSize(3);
        assertThat(result.matches().getFirst().selectedTerm().syntaxRole()).isEqualTo("SUBJECT");
        assertThat(result.matches().getFirst().selectedTerm().text()).isEqualTo("billing service");
        assertThat(responseJson.at("/matches/0/evidence").isMissingNode()).isTrue();
        assertThat(responseJson.at("/matches/0/candidates/0/candidate/candidateKey").asText())
            .isEqualTo("sample-syntax-requirement-1-subject");
        assertThat(responseJson.at("/matches/0/candidates/0/candidate/conceptId").isMissingNode())
            .isTrue();
        assertThat(responseJson.at("/matches/0/candidates/0/evidence/0/lookupRanks").isMissingNode())
            .isTrue();
        assertThat(responseJson.at("/matches/0/candidates/0/evidence/0/appliedLookupMethods")
            .isMissingNode()).isTrue();
        assertThat(result.matches().getFirst().candidates())
            .singleElement()
            .satisfies(retrievedCandidate -> {
                assertThat(retrievedCandidate.candidate().candidateKey())
                    .isEqualTo("sample-syntax-requirement-1-subject");
                assertThat(retrievedCandidate.candidate().label()).isEqualTo("billing service");
                assertThat(retrievedCandidate.candidate().conceptType()).isEqualTo("SyntaxElement");
                assertThat(retrievedCandidate.evidence())
                    .singleElement()
                    .satisfies(evidence -> {
                        assertThat(evidence.policyName()).isEqualTo("conceptName");
                        assertThat(evidence.evidenceText())
                            .isEqualTo(
                                "Matched concept name 'billing service' to graph candidate " +
                                    "'billing service'"
                            );
                        assertThat(evidence.score()).isEqualTo(1.0);
                    });
            });
        assertThat(result.matches().get(1).selectedTerm().syntaxRole()).isEqualTo("ACTION");
        assertThat(result.matches().get(1).selectedTerm().text()).isEqualTo("must refund");
        assertThat(responseJson.at("/matches/1/evidence").isMissingNode()).isTrue();
        assertThat(responseJson.at("/matches/1/candidates/0/candidate/candidateKey").asText())
            .isEqualTo("sample-action-refund");
        assertThat(responseJson.at("/matches/1/candidates/0/candidate/conceptId").isMissingNode())
            .isTrue();
        assertThat(responseJson.at("/matches/1/candidates/0/evidence/0/lookupRanks").isMissingNode())
            .isTrue();
        assertThat(responseJson.at("/matches/1/candidates/0/evidence/0/appliedLookupMethods")
            .isMissingNode()).isTrue();
        assertThat(result.matches().get(1).candidates())
            .singleElement()
            .satisfies(retrievedCandidate -> {
                assertThat(retrievedCandidate.candidate().candidateKey())
                    .isEqualTo("sample-action-refund");
                assertThat(retrievedCandidate.evidence().getFirst().policyName())
                    .isEqualTo("conceptName");
                assertThat(retrievedCandidate.evidence().getFirst().evidenceText())
                    .isEqualTo(
                        "Matched concept name 'must refund' to graph candidate " +
                            "'Refund Service'"
                    );
                assertThat(retrievedCandidate.evidence().getFirst().score()).isEqualTo(1.0);
            });
        assertThat(result.matches().get(2).selectedTerm().syntaxRole()).isEqualTo("OBJECT");
        assertThat(result.matches().get(2).selectedTerm().text()).isEqualTo("unknown workflow");
        assertThat(result.matches().get(2).candidates()).isEmpty();
        assertThat(responseJson.at("/matches/2/evidence").isMissingNode()).isTrue();

        ArgumentCaptor<Collection<SelectedTerm>> selectedTerms =
            selectedTermsCaptor();
        verify(conceptRetrievalService).retrieveCandidates(selectedTerms.capture());
        assertThat(selectedTerms.getValue())
            .extracting(
                SelectedTerm::syntaxRole,
                SelectedTerm::text
            )
            .containsExactly(
                tuple(
                    "SUBJECT",
                    "billing service"
                ),
                tuple(
                    "ACTION",
                    "must refund"
                ),
                tuple(
                    "OBJECT",
                    "unknown workflow"
                )
            );
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
                RequirementType.REQUIREMENT,
                RequirementProperty.FUNCTIONAL,
                0.94,
                "The text assigns a verifiable obligation to the service."
            ),
            Arguments.of(
                "Make checkout better for returning customers.",
                RequirementType.GOAL,
                RequirementProperty.FUNCTIONAL,
                0.42,
                "The wording is ambiguous, so this is a forced best-fit classification."
            )
        );
    }

    private String commandRequest(String requirementText) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "command",
            "ExtractSyntaxCommand",
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

    private String retrieveCandidateConceptsCommandRequest() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "command",
            "RetrieveCandidateConceptsCommand",
            "payload",
            Map.of("selectedTerms", List.of(
                Map.of(
                    "syntaxRole",
                    "SUBJECT",
                    "text",
                    "billing service"
                ),
                Map.of(
                    "syntaxRole",
                    "ACTION",
                    "text",
                    "must refund"
                ),
                Map.of(
                    "syntaxRole",
                    "OBJECT",
                    "text",
                    "unknown workflow"
                )
            ))
        ));
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Collection<SelectedTerm>>
            selectedTermsCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }

    private Set<String> responseSet(String value) {
        return value.isBlank()
            ? Set.of()
            : Set.of(value);
    }

    private Action action(
        String subject,
        String action,
        String targetObject,
        String constraint,
        String condition
    ) {
        Set<Condition> conditions = condition.isBlank()
            ? Set.of()
            : Set.of(new Condition(condition));
        Set<Constraint> constraints = constraint.isBlank()
            ? Set.of()
            : Set.of(new Constraint(constraint));

        return new Action(
            ElementId.create(),
            action,
            new Subject(ElementId.create(), subject),
            new TargetObject(ElementId.create(), targetObject),
            conditions,
            constraints
        );
    }

}
