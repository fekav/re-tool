package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.executeCommandAsJson;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
import static io.fekav.platform.api.RestControllerCommandTestSupport.selectedTermCommandRequest;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.resolution.domain.NodeMatchingService;
import io.fekav.req.resolution.domain.NodeMatchingResult;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class ResolveNodeCommandRestControllerTestIT {

    private static final String COMMAND = "ResolveNodeCommand";
    private static final RequirementElement SUBJECT_ELEMENT =
        new RequirementElement(RequirementElementType.SUBJECT, "billing service");
    private static final CandidateNode SUBJECT_CANDIDATE =
        new CandidateNode(
            "sample-subject-billing-service",
            "Billing Service",
            NodeType.CONCEPT
        );
    private static final RetrievalEvidence SUBJECT_EVIDENCE =
        new RetrievalEvidence(
            "nodeName",
            "Matched node name 'billing service' to graph candidate 'Billing Service'",
            1.0
        );
    private static final String AUTO_MAP_RATIONALE =
        "Unique top candidate reached auto-map threshold 1.0 with score 1.0.";

    @InjectMock
    NodeRetrievalService nodeRetrievalService;

    @InjectMock
    NodeMatchingService nodeMatchingService;

    ObjectMapper objectMapper;
    String requestBody;
    CandidateNodeMatch candidateMatch;
    RetrievedCandidateNode retrievedSubjectCandidate;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(nodeRetrievalService, nodeMatchingService);
        retrievedSubjectCandidate =
            new RetrievedCandidateNode(SUBJECT_CANDIDATE, List.of(SUBJECT_EVIDENCE));
        candidateMatch = new CandidateNodeMatch(
            SUBJECT_ELEMENT,
            List.of(retrievedSubjectCandidate)
        );
        when(nodeRetrievalService.retrieveCandidates(any(RequirementElement.class)))
            .thenReturn(candidateMatch);
        when(nodeMatchingService.evaluateMatch(any(CandidateNodeMatch.class)))
            .thenReturn(NodeMatchingResult.decided(autoMapDecision()));
        requestBody = selectedTermCommandRequest(objectMapper, COMMAND, SUBJECT_ELEMENT);
    }

    @Test
    void serializesDecisionRequirementElement_whenResolveNodeCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/requirementElement/type").asText())
            .isEqualTo(SUBJECT_ELEMENT.type().name());
        assertThat(responseJson.at("/decision/requirementElement/text").asText())
            .isEqualTo(SUBJECT_ELEMENT.text());
    }

    @Test
    void serializesNoMatch_whenResolveNodeCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/match").isMissingNode()).isTrue();
    }

    @Test
    void serializesDecisionStatus_whenResolveNodeCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/status").asText())
            .isEqualTo("AUTO_MAP_EXISTING");
    }

    @Test
    void serializesDecisionCandidateKey_whenResolveNodeCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
    }

    @Test
    void serializesDecisionRationale_whenResolveNodeCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/rationale").asText())
            .isEqualTo(AUTO_MAP_RATIONALE);
    }

    @Test
    void serializesReviewRequest_whenResolveNodeCommandRequiresReview()
        throws Exception {
        when(nodeMatchingService.evaluateMatch(any(CandidateNodeMatch.class)))
            .thenReturn(NodeMatchingResult.reviewRequired(new NodeMatchReviewRequest(
                SUBJECT_ELEMENT,
                List.of(retrievedSubjectCandidate),
                "Candidate needs review before mapping"
            )));

        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision").isMissingNode()).isTrue();
        assertThat(responseJson.at("/reviewRequest/requirementElement/type").asText())
            .isEqualTo(SUBJECT_ELEMENT.type().name());
        assertThat(responseJson.at("/reviewRequest/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
        assertThat(responseJson.at("/reviewRequest/rationale").asText())
            .isEqualTo("Candidate needs review before mapping");
    }

    @Test
    void    PendingReview_whenSameReviewRequiredNodeIsResolvedRepeatedly()
        throws Exception {
        // Given
        RequirementElement repeatedElement =
            new RequirementElement(RequirementElementType.SUBJECT, "notification component");
        RetrievedCandidateNode candidate = new RetrievedCandidateNode(
            new CandidateNode(
                "notification service",
                "notification service",
                NodeType.CONCEPT
            ),
            List.of(new RetrievalEvidence(
                "tokenOverlap",
                "Matched selected term 'notification component' to graph candidate " +
                    "'notification service' by token overlap",
                0.5
            ))
        );
        when(nodeRetrievalService.retrieveCandidates(any(RequirementElement.class)))
            .thenAnswer(invocation -> new CandidateNodeMatch(
                invocation.getArgument(0),
                List.of(candidate)
            ));
        when(nodeMatchingService.evaluateMatch(any(CandidateNodeMatch.class)))
            .thenAnswer(invocation -> {
                CandidateNodeMatch match = invocation.getArgument(0);
                return NodeMatchingResult.reviewRequired(new NodeMatchReviewRequest(
                    match.requirementElement(),
                    match.candidates(),
                    "Top candidate score 0.5 is below auto-map threshold 1.0; " +
                        "human review is required before mapping."
                ));
            });
        String repeatedRequestBody =
            selectedTermCommandRequest(objectMapper, COMMAND, repeatedElement);

        // When
        postCommandForBody(repeatedRequestBody);
        postCommandForBody(repeatedRequestBody);
        JsonNode responseJson = objectMapper.readTree(
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(
                    """
                    {
                      "query": "ListPendingNodeMatchReviewsQuery",
                      "payload": {}
                    }
                    """
                )
            .when()
                .post("/q")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .asString()
        );

        // Then
        List<JsonNode> repeatedElementReviews = StreamSupport
            .stream(responseJson.path("reviews").spliterator(), false)
            .filter(review ->
                "SUBJECT".equals(review.at("/requirementElement/type").asText()) &&
                    "notification component".equals(
                        review.at("/requirementElement/text").asText()
                    )
            )
            .toList();
        assertThat(repeatedElementReviews).hasSize(1);
    }

    @Test
    void passesRequirementElementThroughResolutionServices_whenResolveNodeCommandIsPosted()
        throws Exception {
        executeCommandAsJson(objectMapper, requestBody);

        ArgumentCaptor<RequirementElement> requirementElement =
            ArgumentCaptor.forClass(RequirementElement.class);
        ArgumentCaptor<CandidateNodeMatch> match =
            ArgumentCaptor.forClass(CandidateNodeMatch.class);
        verify(nodeRetrievalService).retrieveCandidates(requirementElement.capture());
        verify(nodeMatchingService).evaluateMatch(match.capture());
        assertThat(requirementElement.getValue()).isEqualTo(SUBJECT_ELEMENT);
        assertThat(match.getValue()).isEqualTo(candidateMatch);
    }

    private NodeMatchDecision autoMapDecision() {
        return new NodeMatchDecision(
            SUBJECT_ELEMENT,
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(retrievedSubjectCandidate),
            AUTO_MAP_RATIONALE
        );
    }
}
