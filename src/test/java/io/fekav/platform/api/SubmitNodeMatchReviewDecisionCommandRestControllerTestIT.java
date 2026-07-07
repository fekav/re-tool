package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.review.application.NodeMatchReviewProjection;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
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
import jakarta.inject.Inject;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class SubmitNodeMatchReviewDecisionCommandRestControllerTestIT {

    @Inject
    NodeMatchReviewProjection projection;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    EventPublisher eventPublisher;

    @BeforeEach
    void resetPublisher() {
        reset(eventPublisher);
    }

    @Test
    void publishesReviewDecision_whenSubmitReviewDecisionCommandIsPosted()
        throws Exception {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "checkout service");
        String reviewId = NodeMatchReviewId.from(correlationId, element).value();
        projection.apply(new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            Instant.parse("2026-07-07T10:15:30Z"),
            correlationId,
            new NodeMatchReviewRequest(
                element,
                List.of(candidate()),
                "Candidate needs review before mapping"
            )
        ));

        // When
        String response =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(
                    """
                    {
                      "command": "SubmitNodeMatchReviewDecisionCommand",
                      "payload": {
                        "reviewId": "%s",
                        "decision": "MAP_EXISTING",
                        "candidateKey": "checkout-service",
                        "rationale": "Domain reviewer selected this candidate."
                      }
                    }
                    """.formatted(reviewId)
                )
            .when()
                .post("/c")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .asString();

        // Then
        JsonNode responseJson = objectMapper.readTree(response);
        assertThat(responseJson.at("/decision/status").asText())
            .isEqualTo("REVIEW_MAP_EXISTING");
        assertThat(responseJson.at("/decision/candidates/0/candidate/candidateKey").asText())
            .isEqualTo("checkout-service");
        assertThat(projection.pendingReview(reviewId)).isEmpty();

        ArgumentCaptor<ApplicationEvent> publishedEvent =
            ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        assertThat(publishedEvent.getValue())
            .isInstanceOfSatisfying(NodeResolutionDecidedEvent.class, event -> {
                assertThat(event.correlationId()).isEqualTo(correlationId);
                assertThat(event.decision().status())
                    .isEqualTo(NodeMatchDecisionStatus.REVIEW_MAP_EXISTING);
            });
    }

    @Test
    void returnsBadRequest_whenReviewDecisionCandidateIsInvalid() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "checkout service");
        String reviewId = NodeMatchReviewId.from(correlationId, element).value();
        projection.apply(new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            Instant.parse("2026-07-07T10:15:30Z"),
            correlationId,
            new NodeMatchReviewRequest(
                element,
                List.of(candidate()),
                "Candidate needs review before mapping"
            )
        ));

        // When / Then
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "command": "SubmitNodeMatchReviewDecisionCommand",
                  "payload": {
                    "reviewId": "%s",
                    "decision": "MAP_EXISTING",
                    "candidateKey": "shipping-service",
                    "rationale": "Domain reviewer selected this candidate."
                  }
                }
                """.formatted(reviewId)
            )
        .when()
            .post("/c")
        .then()
            .statusCode(400);
    }

    private static RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("checkout-service", "Checkout Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
        );
    }
}
