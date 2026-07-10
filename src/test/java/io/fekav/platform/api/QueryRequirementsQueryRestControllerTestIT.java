package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.queryrequirements.application.FindRequirementsQuery;
import io.fekav.req.queryrequirements.application.RequirementsFinder;
import io.fekav.req.queryrequirements.domain.RequirementView;
import io.fekav.req.queryrequirements.domain.RequirementsResult;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class QueryRequirementsQueryRestControllerTestIT {

    @InjectMock
    RequirementsFinder requirementsReader;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void returnsRequirementsWithRawTextTypeAndPropertyOnly() throws Exception {
        // Given
        when(requirementsReader.read(any(FindRequirementsQuery.class)))
            .thenReturn(new RequirementsResult(List.of(
                new RequirementView(
                    "Der Zahlungsdienst muss Lastschriften ausführen.",
                    "REQUIREMENT",
                    "FUNCTIONAL"
                )
            )));

        // When
        String responseBody = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "query": "FindRequirementsQuery",
                  "payload": {
                    "concept": "zahlungsdienst"
                  }
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
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode firstRequirement = response.path("requirements").get(0);
        assertThat(firstRequirement.path("rawText").asText())
            .isEqualTo("Der Zahlungsdienst muss Lastschriften ausführen.");
        assertThat(firstRequirement.path("type").asText()).isEqualTo("REQUIREMENT");
        assertThat(firstRequirement.path("property").asText()).isEqualTo("FUNCTIONAL");
        List<String> fieldNames = new ArrayList<>();
        firstRequirement.fieldNames().forEachRemaining(fieldNames::add);
        assertThat(fieldNames)
            .containsExactly("rawText", "type", "property");
    }

    @Test
    void returnsBadRequestWhenConceptIsMissing() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "query": "QueryRequirementsQuery",
                  "payload": {
                    "concept": " "
                  }
                }
                """
            )
        .when()
            .post("/q")
        .then()
            .statusCode(400);
    }
}
