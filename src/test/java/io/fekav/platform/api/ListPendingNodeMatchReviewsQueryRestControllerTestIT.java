package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.review.application.NodeMatchReviewProjection;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class ListPendingNodeMatchReviewsQueryRestControllerTestIT {

    @Inject
    NodeMatchReviewProjection projection;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void returnsPendingReviews_whenQueryIsPosted() throws Exception {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "checkout service");
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
                .asString();

        // Then
        JsonNode responseJson = objectMapper.readTree(response);
        String reviewId = NodeMatchReviewId.from(correlationId, element).value();
        JsonNode reviewJson = StreamSupport
            .stream(responseJson.path("reviews").spliterator(), false)
            .filter(review -> reviewId.equals(review.path("reviewId").asText()))
            .findFirst()
            .orElseThrow();
        assertThat(reviewJson.at("/reviewId").asText())
            .isEqualTo(reviewId);
        assertThat(reviewJson.at("/correlationId/value").asText())
            .isEqualTo(correlationId.value().toString());
        assertThat(reviewJson.at("/requirementElement/type").asText())
            .isEqualTo("SUBJECT");
        assertThat(reviewJson.at("/candidates/0/candidate/candidateKey").asText())
            .isEqualTo("checkout-service");
        assertThat(reviewJson.at("/requestedAt").asText())
            .isEqualTo("2026-07-07T10:15:30Z");
    }

    private static RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("checkout-service", "Checkout Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
        );
    }
}
