package io.fekav.req.conceptretrieval.infrastructure;

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
    void returnsConceptNameCandidatesInDeterministicGraphOrder() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("concept-1", "Billing API", "SystemComponent"),
            record("concept-2", "Billing Service", null)
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
                tuple("concept-1", "Billing API", "SystemComponent"),
                tuple("concept-2", "Billing Service", null)
            );

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("candidate.label = $text")
            .contains("candidate.text = $text")
            .contains("candidate.alias = $text")
            .contains("$text IN coalesce(candidate.aliases, [])")
            .contains("candidate.requirementElement IS NULL")
            .contains("candidate.requirementElement = $requirementElement")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("$allowedConceptTypes");
        assertThat(parameters.getValue())
            .containsEntry("text", "billing service")
            .containsEntry("requirementElement", "SUBJECT")
            .containsOnlyKeys("text", "requirementElement");
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
