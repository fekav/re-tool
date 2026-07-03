package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.executeCommandAsJson;
import static io.fekav.platform.api.RestControllerCommandTestSupport.selectedTermPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class EvaluateConceptMatchCommandRestControllerTestIT {

    private static final String COMMAND = "EvaluateConceptMatchCommand";
    private static final RequirementElement SUBJECT_TERM =
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
    ConceptMatchingService conceptMatchingService;

    ObjectMapper objectMapper;
    String requestBody;
    RetrievedCandidateConcept retrievedSubjectCandidate;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(conceptMatchingService);
        retrievedSubjectCandidate =
            new RetrievedCandidateConcept(SUBJECT_CANDIDATE, List.of(SUBJECT_EVIDENCE));
        when(conceptMatchingService.evaluateMatch(any(CandidateConceptMatch.class)))
            .thenReturn(autoMapDecision());
        requestBody = evaluateConceptMatchCommandRequest();
    }

    @Test
    void serializesDecisionRequirementElement_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/requirementElement/type").asText())
            .isEqualTo(SUBJECT_TERM.type().name());
        assertThat(responseJson.at("/decision/requirementElement/text").asText())
            .isEqualTo(SUBJECT_TERM.text());
    }

    @Test
    void serializesNoMatch_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/match").isMissingNode()).isTrue();
    }

    @Test
    void serializesDecisionStatus_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/status").asText())
            .isEqualTo("AUTO_MAP_EXISTING");
    }

    @Test
    void serializesDecisionCandidatesArray_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/candidates").isArray()).isTrue();
    }

    @Test
    void serializesDecisionRationale_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/rationale").asText())
            .isEqualTo(AUTO_MAP_RATIONALE);
    }

    @Test
    void serializesDecisionCandidateKey_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson(objectMapper, requestBody);

        assertThat(responseJson.at("/decision/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
    }

    @Test
    void passesMatchToMatchingService_whenEvaluateConceptMatchCommandIsPosted()
        throws Exception {
        executeCommandAsJson(objectMapper, requestBody);

        CandidateConceptMatch match = capturedMatch();
        assertThat(match.requirementElement()).isEqualTo(SUBJECT_TERM);
        assertThat(match.candidates()).containsExactly(retrievedSubjectCandidate);
    }

    private CandidateConceptMatch capturedMatch() {
        ArgumentCaptor<CandidateConceptMatch> match =
            ArgumentCaptor.forClass(CandidateConceptMatch.class);
        verify(conceptMatchingService).evaluateMatch(match.capture());
        return match.getValue();
    }

    private ConceptMatchDecision autoMapDecision() {
        return new ConceptMatchDecision(
            SUBJECT_TERM,
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(retrievedSubjectCandidate),
            AUTO_MAP_RATIONALE
        );
    }

    private String evaluateConceptMatchCommandRequest() throws Exception {
        return commandRequest(
            objectMapper,
            COMMAND,
            Map.of("match", candidateMatchPayload(
                SUBJECT_TERM,
                List.of(retrievedCandidatePayload(retrievedSubjectCandidate))
            ))
        );
    }

    private Map<String, Object> candidateMatchPayload(
        RequirementElement selectedTerm,
        List<Map<String, Object>> candidates
    ) {
        return Map.of(
            "requirementElement",
            selectedTermPayload(selectedTerm),
            "candidates",
            candidates
        );
    }

    private Map<String, Object> retrievedCandidatePayload(
        RetrievedCandidateConcept retrievedCandidate
    ) {
        CandidateConcept candidate = retrievedCandidate.candidate();
        RetrievalEvidence evidence = retrievedCandidate.evidence().getFirst();
        return Map.of(
            "candidate",
            Map.of(
                "candidateKey",
                candidate.candidateKey(),
                "label",
                candidate.label(),
                "conceptType",
                candidate.conceptType()
            ),
            "evidence",
            List.of(Map.of(
                "policyName",
                evidence.policyName(),
                "evidenceText",
                evidence.evidenceText(),
                "score",
                evidence.score()
            ))
        );
    }
}
