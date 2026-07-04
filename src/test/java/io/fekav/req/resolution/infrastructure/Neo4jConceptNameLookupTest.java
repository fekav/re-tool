package io.fekav.req.resolution.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.InternalRecord;

import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

@ExtendWith(MockitoExtension.class)
class Neo4jConceptNameLookupTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @Mock
    Result result;

    @InjectMocks
    Neo4jConceptNameLookup lookup;

    @BeforeEach
    void setUp() {
        when(driver.session()).thenReturn(session);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeRead(any());
        when(transaction.run(anyString(), anyMap())).thenReturn(result);
    }

    @Test
    void returnsRequirementElementCandidatesInDeterministicGraphOrder() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("sample-requirement-element-1-subject", "billing service", "SUBJECT"),
            record("sample-requirement-element-2-subject", "payment adapter", "SUBJECT")
        ));

        // When
        var candidates = lookup.findCandidates(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.conceptType()
            )
            .containsExactly(
                tuple("sample-requirement-element-1-subject", "billing service", "SUBJECT"),
                tuple("sample-requirement-element-2-subject", "payment adapter", "SUBJECT")
            );

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:RequirementElement)")
            .contains("candidate.text = $text")
            .contains("candidate.type = $requirementElement")
            .contains("toString(coalesce(candidate.id, candidate.text)) AS candidateKey")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("candidate.label")
            .doesNotContain("candidate.alias")
            .doesNotContain("candidate.requirementElement")
            .doesNotContain("candidate.actionText")
            .doesNotContain("$allowedConceptTypes");
        assertThat(parameters.getValue())
            .containsEntry("text", "billing service")
            .containsEntry("requirementElement", "SUBJECT")
            .containsOnlyKeys("text", "requirementElement");
        verify(session).close();
    }

    @Test
    void returnsActionCandidatesByActionText() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("sample-action-1", "log", "ACTION")
        ));

        // When
        var candidates = lookup.findCandidates(new RequirementElement(RequirementElementType.ACTION, "log"));

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.conceptType()
            )
            .containsExactly(tuple("sample-action-1", "log", "ACTION"));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Action)")
            .contains("candidate.actionText = $text")
            .contains("RETURN candidateKey, candidateLabel, 'ACTION' AS conceptType")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("candidate.type = $requirementElement")
            .doesNotContain("RequirementElement");
        assertThat(parameters.getValue())
            .containsEntry("text", "log")
            .containsOnlyKeys("text");
        verify(session).close();
    }

    @Test
    void returnsEmptyList_whenGraphHasNoConceptNameHits() {
        // Given
        when(result.stream()).thenReturn(Stream.empty());

        // When
        var candidates = lookup.findCandidates(new RequirementElement(RequirementElementType.ACTION, "must refund"));

        // Then
        assertThat(candidates).isEmpty();
    }

    private Record record(String candidateKey, String label, String conceptType) {
        return new InternalRecord(
            java.util.List.of("candidateKey", "candidateLabel", "conceptType"),
            java.util.List.of(value(candidateKey), value(label), value(conceptType))
        );
    }

    private Value value(String value) {
        return value == null ? Values.NULL : Values.value(value);
    }

    private String singleLine(String statement) {
        return statement.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
