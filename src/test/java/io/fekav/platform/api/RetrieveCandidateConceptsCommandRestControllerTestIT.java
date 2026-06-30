package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
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
import io.fekav.req.shared.model.CandidateConceptMatchSet;
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
    private static final SelectedTerm ACTION_TERM =
        new SelectedTerm(RequirementElement.ACTION, "must refund");
    private static final SelectedTerm OBJECT_TERM =
        new SelectedTerm(RequirementElement.OBJECT, "unknown workflow");
    private static final CandidateConcept SUBJECT_CANDIDATE =
        new CandidateConcept(
            "sample-syntax-requirement-1-subject",
            "billing service",
            "SyntaxElement"
        );
    private static final CandidateConcept ACTION_CANDIDATE =
        new CandidateConcept(
            "sample-action-refund",
            "Refund Service",
            "SystemComponent"
        );
    private static final RetrievalEvidence SUBJECT_EVIDENCE =
        new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'billing service' to graph candidate 'billing service'",
            1.0
        );
    private static final RetrievalEvidence ACTION_EVIDENCE =
        new RetrievalEvidence(
            "conceptName",
            "Matched concept name 'must refund' to graph candidate 'Refund Service'",
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
        when(conceptRetrievalService.retrieveCandidates(anyCollection()))
            .thenReturn(candidateConceptMatchSet());
        requestBody = retrieveCandidateConceptsCommandRequest();
    }

    @Test
    void returnsThreeMatches_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(result.matches()).hasSize(3);
    }

    @Test
    void returnsSubjectSelectedTerm_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(result.matches().getFirst().selectedTerm()).isEqualTo(SUBJECT_TERM);
    }

    @Test
    void returnsActionSelectedTerm_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(result.matches().get(1).selectedTerm()).isEqualTo(ACTION_TERM);
    }

    @Test
    void returnsObjectSelectedTerm_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(result.matches().get(2).selectedTerm()).isEqualTo(OBJECT_TERM);
    }

    @Test
    void returnsSubjectCandidate_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(subjectCandidate(result).candidate()).isEqualTo(SUBJECT_CANDIDATE);
    }

    @Test
    void returnsSubjectEvidence_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(subjectCandidate(result).evidence()).containsExactly(SUBJECT_EVIDENCE);
    }

    @Test
    void returnsActionCandidate_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(actionCandidate(result).candidate()).isEqualTo(ACTION_CANDIDATE);
    }

    @Test
    void returnsActionEvidence_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(actionCandidate(result).evidence()).containsExactly(ACTION_EVIDENCE);
    }

    @Test
    void returnsEmptyObjectCandidates_whenRetrieveCandidateConceptsCommandIsPosted() {
        CandidateConceptMatchSet result = executeCommand();

        assertThat(result.matches().get(2).candidates()).isEmpty();
    }

    @Test
    void serializesSubjectCandidateKey_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/matches/0/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
    }

    @Test
    void serializesActionCandidateKey_whenRetrieveCandidateConceptsCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/matches/1/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(ACTION_CANDIDATE.candidateKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/matches/0/evidence",
        "/matches/1/evidence",
        "/matches/2/evidence",
        "/matches/0/candidates/0/candidate/conceptId",
        "/matches/1/candidates/0/candidate/conceptId",
        "/matches/0/candidates/0/evidence/0/lookupRanks",
        "/matches/1/candidates/0/evidence/0/lookupRanks",
        "/matches/0/candidates/0/evidence/0/appliedLookupMethods",
        "/matches/1/candidates/0/evidence/0/appliedLookupMethods"
    })
    void omitsDeprecatedFields_whenCandidateConceptMatchesAreSerialized(String jsonPath)
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at(jsonPath).isMissingNode()).isTrue();
    }

    @Test
    void passesSelectedTermsToRetrievalService_whenRetrieveCandidateConceptsCommandIsPosted() {
        executeCommand();

        ArgumentCaptor<Collection<SelectedTerm>> selectedTerms =
            selectedTermsCaptor();
        verify(conceptRetrievalService).retrieveCandidates(selectedTerms.capture());
        assertThat(selectedTerms.getValue())
            .extracting(SelectedTerm::requirementElement, SelectedTerm::text)
            .containsExactly(
                tuple(SUBJECT_TERM.requirementElement(), SUBJECT_TERM.text()),
                tuple(ACTION_TERM.requirementElement(), ACTION_TERM.text()),
                tuple(OBJECT_TERM.requirementElement(), OBJECT_TERM.text())
            );
    }

    private CandidateConceptMatchSet executeCommand() {
        return postCommandForResponse(requestBody, CandidateConceptMatchSet.class);
    }

    private JsonNode executeCommandAsJson() throws Exception {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    private String retrieveCandidateConceptsCommandRequest() throws Exception {
        return commandRequest(
            objectMapper,
            "RetrieveCandidateConceptsCommand",
            Map.of("selectedTerms", List.of(
                selectedTermPayload(SUBJECT_TERM),
                selectedTermPayload(ACTION_TERM),
                selectedTermPayload(OBJECT_TERM)
            ))
        );
    }

    private CandidateConceptMatchSet candidateConceptMatchSet() {
        return new CandidateConceptMatchSet(List.of(
            new CandidateConceptMatch(
                SUBJECT_TERM,
                List.of(new RetrievedCandidateConcept(
                    SUBJECT_CANDIDATE,
                    List.of(SUBJECT_EVIDENCE)
                ))
            ),
            new CandidateConceptMatch(
                ACTION_TERM,
                List.of(new RetrievedCandidateConcept(
                    ACTION_CANDIDATE,
                    List.of(ACTION_EVIDENCE)
                ))
            ),
            new CandidateConceptMatch(
                OBJECT_TERM,
                List.of()
            )
        ));
    }

    private RetrievedCandidateConcept subjectCandidate(
        CandidateConceptMatchSet result
    ) {
        return result.matches().getFirst().candidates().getFirst();
    }

    private RetrievedCandidateConcept actionCandidate(
        CandidateConceptMatchSet result
    ) {
        return result.matches().get(1).candidates().getFirst();
    }

    private Map<String, Object> selectedTermPayload(SelectedTerm selectedTerm) {
        return Map.of(
            "requirementElement",
            selectedTerm.requirementElement(),
            "text",
            selectedTerm.text()
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Collection<SelectedTerm>> selectedTermsCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }
}
