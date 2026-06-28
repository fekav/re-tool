package io.fekav.req.conceptretrieval.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import io.fekav.req.conceptretrieval.application.RetrieveCandidateConceptsCommand;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;

@ExtendWith(MockitoExtension.class)
class ExactMatchConceptRetrievalPolicyTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    Result hitResult;

    @Mock
    Result noHitResult;

    @Mock
    org.neo4j.driver.Record firstRecord;

    @Mock
    org.neo4j.driver.Record secondRecord;

    @Mock
    Value firstConceptId;

    @Mock
    Value firstLabel;

    @Mock
    Value firstConceptType;

    @Mock
    Value secondConceptId;

    @Mock
    Value secondLabel;

    @Mock
    Value secondConceptType;

    @InjectMocks
    ExactMatchConceptRetrievalPolicy policy;

    @Test
    void isApplicationScopedBean() {
        // When / Then
        assertThat(ExactMatchConceptRetrievalPolicy.class)
                .hasAnnotation(ApplicationScoped.class);
    }

    @Test
    void returnsCandidateEvidence_whenGraphCandidatesMatchSelectedTerm() {
        // Given
        when(driver.session()).thenReturn(session);
        RetrieveCandidateConceptsCommand.SelectedTermInput term =
                selectedTerm("SUBJECT", "billing service");
        when(session.run(anyString(), anyMap())).thenReturn(hitResult);
        when(hitResult.stream()).thenReturn(Stream.of(firstRecord));
        recordValues(
                firstRecord,
                firstConceptId,
                firstLabel,
                firstConceptType,
                "sample-syntax-requirement-1-subject",
                "billing service",
                "SUBJECT");

        // When
        CandidateConceptMatchSet matchSet = policy.retrieveCandidates(List.of(term));

        // Then
        assertThat(matchSet.matches())
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.syntaxRole()).isEqualTo("SUBJECT");
                    assertThat(match.text()).isEqualTo("billing service");
                    assertThat(match.candidates())
                            .extracting("conceptId", "label", "conceptType")
                            .containsExactly(
                                    org.assertj.core.groups.Tuple.tuple(
                                            "sample-syntax-requirement-1-subject",
                                            "billing service",
                                            "SUBJECT"));
                    assertThat(match.evidence())
                            .extracting("policyName", "evidenceText", "score")
                            .containsExactly(
                                    org.assertj.core.groups.Tuple.tuple(
                                            "exactMatch",
                                            "Matched graph candidate 'billing service'",
                                            1.0));
                });
        QueryCall queryCall = onlyQueryCall();
        assertThat(queryCall.query())
                .contains("MATCH (e:SyntaxElement)")
                .contains("WHERE e.text = $label")
                .contains("MATCH (r:Requirement)")
                .contains("WHERE r.rawText = $label")
                .contains("MATCH (t:RequirementType)")
                .contains("MATCH (p:RequirementProperty)")
                .contains("MATCH (r:SyntaxRole)")
                .contains("MATCH (t:RequirementRelationType)")
                .contains("ORDER BY label ASC, conceptId ASC")
                .doesNotContain(":Concept");
        assertThat(queryCall.parameters())
                .containsEntry("label", "billing service");
        verify(session).close();
    }

    @Test
    void returnsNoMatchEvidence_whenNoGraphCandidatesMatchSelectedTerm() {
        // Given
        when(driver.session()).thenReturn(session);
        RetrieveCandidateConceptsCommand.SelectedTermInput term =
                selectedTerm("ACTION", "must refund");
        when(session.run(anyString(), anyMap())).thenReturn(noHitResult);
        when(noHitResult.stream()).thenReturn(Stream.empty());

        // When
        CandidateConceptMatchSet matchSet = policy.retrieveCandidates(List.of(term));

        // Then
        assertThat(matchSet.matches())
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.syntaxRole()).isEqualTo("ACTION");
                    assertThat(match.text()).isEqualTo("must refund");
                    assertThat(match.candidates()).isEmpty();
                    assertThat(match.evidence())
                            .extracting("policyName", "evidenceText", "score")
                            .containsExactly(org.assertj.core.groups.Tuple.tuple(
                                    "exactMatch",
                                    "No graph candidate matched 'must refund'",
                                    null));
                });
    }

    @Test
    void queriesEachSelectedTermByExactText() {
        // Given
        when(driver.session()).thenReturn(session);
        RetrieveCandidateConceptsCommand.SelectedTermInput subject =
                selectedTerm("SUBJECT", "reporting dashboard");
        RetrieveCandidateConceptsCommand.SelectedTermInput object =
                selectedTerm("OBJECT", "monthly usage metrics");
        when(session.run(anyString(), anyMap())).thenReturn(hitResult, noHitResult);
        when(hitResult.stream()).thenReturn(Stream.empty());
        when(noHitResult.stream()).thenReturn(Stream.empty());

        // When
        policy.retrieveCandidates(List.of(subject, object));

        // Then
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(session, org.mockito.Mockito.times(2))
                .run(anyString(), parameters.capture());
        assertThat(parameters.getAllValues())
                .extracting(params -> params.get("label"))
                .containsExactly("reporting dashboard", "monthly usage metrics");
    }

    @SuppressWarnings("unchecked")
    private QueryCall onlyQueryCall() {
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(session).run(query.capture(), parameters.capture());

        return new QueryCall(query.getValue(), parameters.getValue());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }

    private void recordValues(
            org.neo4j.driver.Record record,
            Value conceptId,
            Value label,
            Value conceptType,
            String conceptIdValue,
            String labelValue,
            String conceptTypeValue) {
        when(record.get("conceptId")).thenReturn(conceptId);
        when(record.get("label")).thenReturn(label);
        when(record.get("conceptType")).thenReturn(conceptType);
        when(conceptId.asString()).thenReturn(conceptIdValue);
        when(label.asString()).thenReturn(labelValue);
        when(conceptType.asString(null)).thenReturn(conceptTypeValue);
    }

    private RetrieveCandidateConceptsCommand.SelectedTermInput selectedTerm(
            String syntaxRole,
            String text) {
        return new RetrieveCandidateConceptsCommand.SelectedTermInput(
                syntaxRole,
                text);
    }

    private record QueryCall(
            String query,
            Map<String, Object> parameters) {
    }
}
