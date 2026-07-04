package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.executeCommandAsJson;
import static io.fekav.platform.api.RestControllerCommandTestSupport.selectedTermCommandRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.resolution.domain.ConceptMatchingService;
import io.fekav.req.resolution.domain.ConceptRetrievalService;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class ResolveConceptCommandRestControllerTestIT {

    private static final String COMMAND = "ResolveConceptCommand";
    private static final RequirementElement SUBJECT_ELEMENT =
        new RequirementElement(RequirementElementType.SUBJECT, "billing service");
    private static final CandidateConcept SUBJECT_CANDIDATE =
        new CandidateConcept(
            "sample-subject-billing-service",
            "Billing Service",
            "SystemComponent"
        );
    private static final RetrievalEvidence SUBJECT_EVIDENCE =
        new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'billing service' to graph candidate 'Billing Service'",
            1.0
        );
    private static final String AUTO_MAP_RATIONALE =
        "Unique top candidate reached auto-map threshold 1.0 with score 1.0.";

    @InjectMock
    ConceptRetrievalService conceptRetrievalService;

    @InjectMock
    ConceptMatchingService conceptMatchingService;

    ObjectMapper objectMapper;
    String requestBody;
    CandidateConceptMatch candidateMatch;
    RetrievedCandidateConcept retrievedSubjectCandidate;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(conceptRetrievalService, conceptMatchingService);
        retrievedSubjectCandidate =
            new RetrievedCandidateConcept(SUBJECT_CANDIDATE, List.of(SUBJECT_EVIDENCE));
        candidateMatch = new CandidateConceptMatch(
            SUBJECT_ELEMENT,
            List.of(retrievedSubjectCandidate)
        );
        when(conceptRetrievalService.retrieveCandidates(any(RequirementElement.class)))
            .thenReturn(candidateMatch);
        when(conceptMatchingService.evaluateMatch(any(CandidateConceptMatch.class)))
            .thenReturn(autoMapDecision());
        requestBody = selectedTermCommandRequest(objectMapper, COMMAND, SUBJECT_ELEMENT);
    }

    @Test
    void serializesDecisionRequirementElement_whenResolveConceptCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/requirementElement/type").asText())
            .isEqualTo(SUBJECT_ELEMENT.type().name());
        assertThat(responseJson.at("/decision/requirementElement/text").asText())
            .isEqualTo(SUBJECT_ELEMENT.text());
    }

    @Test
    void serializesNoMatch_whenResolveConceptCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/match").isMissingNode()).isTrue();
    }

    @Test
    void serializesDecisionStatus_whenResolveConceptCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/status").asText())
            .isEqualTo("AUTO_MAP_EXISTING");
    }

    @Test
    void serializesDecisionCandidateKey_whenResolveConceptCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
    }

    @Test
    void serializesDecisionRationale_whenResolveConceptCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/rationale").asText())
            .isEqualTo(AUTO_MAP_RATIONALE);
    }

    @Test
    void passesRequirementElementThroughResolutionServices_whenResolveConceptCommandIsPosted()
        throws Exception {
        executeCommandAsJson(objectMapper, requestBody);

        ArgumentCaptor<RequirementElement> requirementElement =
            ArgumentCaptor.forClass(RequirementElement.class);
        ArgumentCaptor<CandidateConceptMatch> match =
            ArgumentCaptor.forClass(CandidateConceptMatch.class);
        verify(conceptRetrievalService).retrieveCandidates(requirementElement.capture());
        verify(conceptMatchingService).evaluateMatch(match.capture());
        assertThat(requirementElement.getValue()).isEqualTo(SUBJECT_ELEMENT);
        assertThat(match.getValue()).isEqualTo(candidateMatch);
    }

    private ConceptMatchDecision autoMapDecision() {
        return new ConceptMatchDecision(
            SUBJECT_ELEMENT,
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(retrievedSubjectCandidate),
            AUTO_MAP_RATIONALE
        );
    }
}
