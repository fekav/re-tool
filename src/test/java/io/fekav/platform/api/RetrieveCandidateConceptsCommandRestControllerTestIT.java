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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
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
class RetrieveCandidateConceptsCommandRestControllerTestIT {

    private static final String COMMAND = "RetrieveCandidateConceptsCommand";
    private static final RequirementElement SUBJECT_TERM =
        new RequirementElement(RequirementElementType.SUBJECT, "billing service");
    private static final RequirementElement OBJECT_TERM =
        new RequirementElement(RequirementElementType.OBJECT, "unknown workflow");
    private static final CandidateConcept SUBJECT_CANDIDATE =
        new CandidateConcept(
            "sample-syntax-requirement-1-subject",
            "billing service",
            "SyntaxElement"
        );
    private static final RetrievalEvidence SUBJECT_EVIDENCE =
        new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'billing service' to graph candidate 'billing service'",
            1.0
        );

    @InjectMock
    ConceptRetrievalService conceptRetrievalService;

    ObjectMapper objectMapper;
    String requestBody;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(conceptRetrievalService);
        when(conceptRetrievalService.retrieveCandidates(any(RequirementElement.class)))
            .thenReturn(candidateConceptMatch());
        requestBody = selectedTermCommandRequest(objectMapper, COMMAND, SUBJECT_TERM);
    }

    @Test
    void returnsSelectedTerm_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        assertThat(result.at("/match/requirementElement/type").asText())
            .isEqualTo(SUBJECT_TERM.type().name());
        assertThat(result.at("/match/requirementElement/text").asText())
            .isEqualTo(SUBJECT_TERM.text());
    }

    @Test
    void returnsCandidate_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        assertThat(result.at("/match/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
        assertThat(result.at("/match/candidates/0/candidate/label").asText())
            .isEqualTo(SUBJECT_CANDIDATE.label());
        assertThat(result.at("/match/candidates/0/candidate/conceptType").asText())
            .isEqualTo(SUBJECT_CANDIDATE.conceptType());
    }

    @Test
    void returnsEvidence_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        assertThat(result.at("/match/candidates/0/evidence/0/policyName").asText())
            .isEqualTo(SUBJECT_EVIDENCE.policyName());
        assertThat(result.at("/match/candidates/0/evidence/0/evidenceText").asText())
            .isEqualTo(SUBJECT_EVIDENCE.evidenceText());
        assertThat(result.at("/match/candidates/0/evidence/0/score").asDouble())
            .isEqualTo(SUBJECT_EVIDENCE.score());
    }

    @Test
    void returnsEmptyCandidates_whenRetrievedMatchHasNoCandidates()
        throws Exception {
        when(conceptRetrievalService.retrieveCandidates(OBJECT_TERM))
            .thenReturn(new CandidateConceptMatch(OBJECT_TERM, List.of()));

        JsonNode result = executeCommandAsJson(
            objectMapper,
            selectedTermCommandRequest(objectMapper, COMMAND, OBJECT_TERM)
        );

        assertThat(result.at("/match/requirementElement/text").asText()).isEqualTo(OBJECT_TERM.text());
        assertThat(result.at("/match/candidates").size()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/match/evidence",
        "/match/candidates/0/candidate/conceptId",
        "/match/candidates/0/evidence/0/lookupRanks",
        "/match/candidates/0/evidence/0/appliedLookupMethods"
    })
    void omitsDeprecatedFields_whenCandidateConceptMatchIsSerialized(String jsonPath)
        throws Exception {
        JsonNode result = executeCommandAsJson(objectMapper, requestBody);

        assertThat(result.at(jsonPath).isMissingNode()).isTrue();
    }

    @Test
    void passesSelectedTermToRetrievalService_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        executeCommandAsJson(objectMapper, requestBody);

        ArgumentCaptor<RequirementElement> selectedTerm =
            ArgumentCaptor.forClass(RequirementElement.class);
        verify(conceptRetrievalService).retrieveCandidates(selectedTerm.capture());
        assertThat(selectedTerm.getValue()).isEqualTo(SUBJECT_TERM);
    }

    private CandidateConceptMatch candidateConceptMatch() {
        return new CandidateConceptMatch(
            SUBJECT_TERM,
            List.of(new RetrievedCandidateConcept(
                SUBJECT_CANDIDATE,
                List.of(SUBJECT_EVIDENCE)
            ))
        );
    }
}
