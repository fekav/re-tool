package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
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
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.model.CandidateConcept;
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
class DecideConceptMatchesCommandRestControllerTestIT {

    private static final SelectedTerm SUBJECT_TERM =
        new SelectedTerm(RequirementElement.SUBJECT, "billing service");
    private static final SelectedTerm OBJECT_TERM =
        new SelectedTerm(RequirementElement.OBJECT, "unknown workflow");
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
        when(conceptMatchingService.decideMatches(any(CandidateConceptMatchSet.class)))
            .thenReturn(conceptMatchDecisionSet());
        requestBody = decideConceptMatchesCommandRequest();
    }

    @Test
    void returnsTwoDecisions_whenDecideConceptMatchesCommandIsPosted() {
        ConceptMatchDecisionSet result = executeCommand();

        assertThat(result.decisions()).hasSize(2);
    }

    @Test
    void serializesAutoMapStatus_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/0/status").asText())
            .isEqualTo("AUTO_MAP_EXISTING");
    }

    @Test
    void serializesAutoMapCandidatesArray_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/0/candidates").isArray()).isTrue();
    }

    @Test
    void serializesAutoMapRationale_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/0/rationale").asText())
            .isEqualTo(AUTO_MAP_RATIONALE);
    }

    @Test
    void serializesAutoMapCandidateKey_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/0/candidates/0/candidate/candidateKey").asText())
            .isEqualTo(SUBJECT_CANDIDATE.candidateKey());
    }

    @Test
    void serializesAutoCreateStatus_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/1/status").asText())
            .isEqualTo("AUTO_CREATE_NEW");
    }

    @Test
    void serializesAutoCreateCandidatesArray_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/1/candidates").isArray()).isTrue();
    }

    @Test
    void serializesAutoCreateEmptyCandidates_whenDecideConceptMatchesCommandIsPosted()
        throws Exception {
        JsonNode responseJson = executeCommandAsJson();

        assertThat(responseJson.at("/decisions/1/candidates").size()).isZero();
    }

    void passesSelectedTermsToMatchingService_whenDecideConceptMatchesCommandIsPosted() {
        executeCommand();

        CandidateConceptMatchSet matches = capturedMatches();
        assertThat(matches.matches())
            .extracting(
                match -> match.selectedTerm().requirementElement(),
                match -> match.selectedTerm().text()
            )
            .containsExactly(
                tuple(SUBJECT_TERM.requirementElement(), SUBJECT_TERM.text()),
                tuple(OBJECT_TERM.requirementElement(), OBJECT_TERM.text())
            );
    }

    @Test
    void passesSubjectCandidateToMatchingService_whenDecideConceptMatchesCommandIsPosted() {
        executeCommand();

        CandidateConceptMatchSet matches = capturedMatches();
        assertThat(matches.matches().getFirst().candidates())
            .containsExactly(retrievedSubjectCandidate);
    }

    @Test
    void passesObjectWithoutCandidatesToMatchingService_whenDecideConceptMatchesCommandIsPosted() {
        executeCommand();

        CandidateConceptMatchSet matches = capturedMatches();
        assertThat(matches.matches().get(1).candidates()).isEmpty();
    }

    private ConceptMatchDecisionSet executeCommand() {
        return postCommandForResponse(requestBody, ConceptMatchDecisionSet.class);
    }

    private JsonNode executeCommandAsJson() throws Exception {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    private CandidateConceptMatchSet capturedMatches() {
        ArgumentCaptor<CandidateConceptMatchSet> matches =
            ArgumentCaptor.forClass(CandidateConceptMatchSet.class);
        verify(conceptMatchingService).decideMatches(matches.capture());
        return matches.getValue();
    }

    private ConceptMatchDecisionSet conceptMatchDecisionSet() {
        return new ConceptMatchDecisionSet(List.of(
            new ConceptMatchDecision(
                SUBJECT_TERM,
                ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
                List.of(retrievedSubjectCandidate),
                AUTO_MAP_RATIONALE
            ),
            new ConceptMatchDecision(
                OBJECT_TERM,
                ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
                List.of(),
                "No existing candidates found; auto-creating concept from selected term."
            )
        ));
    }

    private String decideConceptMatchesCommandRequest() throws Exception {
        return commandRequest(
            objectMapper,
            "DecideConceptMatchesCommand",
            Map.of("matches", List.of(
                candidateMatchPayload(
                    SUBJECT_TERM,
                    List.of(retrievedCandidatePayload(retrievedSubjectCandidate))
                ),
                candidateMatchPayload(OBJECT_TERM, List.of())
            ))
        );
    }

    private Map<String, Object> candidateMatchPayload(
        SelectedTerm selectedTerm,
        List<Map<String, Object>> candidates
    ) {
        return Map.of(
            "selectedTerm",
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

    private Map<String, Object> selectedTermPayload(SelectedTerm selectedTerm) {
        return Map.of(
            "requirementElement",
            selectedTerm.requirementElement(),
            "text",
            selectedTerm.text()
        );
    }
}
