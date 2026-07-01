package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
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
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class RetrieveCandidateConceptsCommandRestControllerTestIT {

    private static final SelectedTerm SUBJECT_TERM =
        new SelectedTerm(RequirementElement.SUBJECT, "billing service");
    private static final SelectedTerm OBJECT_TERM =
        new SelectedTerm(RequirementElement.OBJECT, "unknown workflow");
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
        when(conceptRetrievalService.retrieveCandidates(any(SelectedTerm.class)))
            .thenReturn(candidateConceptMatch());
        requestBody = retrieveCandidateConceptsCommandRequest(SUBJECT_TERM);
    }

    @Test
    void returnsSelectedTerm_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/match/selectedTerm/requirementElement").asText())
            .isEqualTo(SUBJECT_TERM.requirementElement().name());
        assertThat(result.at("/match/selectedTerm/text").asText())
            .isEqualTo(SUBJECT_TERM.text());
    }

    @Test
    void returnsCandidate_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

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
        JsonNode result = executeCommandAsJson(requestBody);

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
            retrieveCandidateConceptsCommandRequest(OBJECT_TERM)
        );

        assertThat(result.at("/match/selectedTerm/text").asText()).isEqualTo(OBJECT_TERM.text());
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
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at(jsonPath).isMissingNode()).isTrue();
    }

    @Test
    void passesSelectedTermToRetrievalService_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        executeCommandAsJson(requestBody);

        ArgumentCaptor<SelectedTerm> selectedTerm =
            ArgumentCaptor.forClass(SelectedTerm.class);
        verify(conceptRetrievalService).retrieveCandidates(selectedTerm.capture());
        assertThat(selectedTerm.getValue()).isEqualTo(SUBJECT_TERM);
    }

    private JsonNode executeCommandAsJson(String requestBody) throws Exception {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    private String retrieveCandidateConceptsCommandRequest(SelectedTerm selectedTerm)
        throws Exception {
        return commandRequest(
            objectMapper,
            "RetrieveCandidateConceptsCommand",
            Map.of("selectedTerm", selectedTermPayload(selectedTerm))
        );
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

    private Map<String, Object> selectedTermPayload(SelectedTerm selectedTerm) {
        return Map.of(
            "requirementElement",
            selectedTerm.requirementElement(),
            "text",
            selectedTerm.text()
        );
    }
}
