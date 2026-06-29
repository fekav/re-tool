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

import io.fekav.req.conceptretrieval.application.CandidateLookupScope;
import io.fekav.req.conceptretrieval.domain.CandidateLookupHit;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;

@ExtendWith(MockitoExtension.class)
class Neo4jAliasCandidateLookupTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @Mock
    Result result;

    @InjectMocks
    Neo4jAliasCandidateLookup lookup;

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
    void returnsAliasHitsInDeterministicGraphOrder() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("concept-1", "Refund Service", "SystemComponent"),
            record("concept-2", "Returns API", "SystemComponent")
        ));

        // When
        var hits = lookup.findCandidates(
            new SelectedTerm("ACTION", "must refund"),
            CandidateLookupScope.forSelectedTerm(new SelectedTerm("ACTION", "must refund"))
        );

        // Then
        assertThat(lookup.lookupMethodName()).isEqualTo("alias");
        assertThat(lookup.lookupWeight()).isEqualTo(0.7);
        assertThat(hits)
            .extracting(
                hit -> hit.candidate().candidateKey(),
                hit -> hit.candidate().label(),
                CandidateLookupHit::lookupMethodName
            )
            .containsExactly(
                tuple("concept-1", "Refund Service", "alias"),
                tuple("concept-2", "Returns API", "alias")
            );
        assertThat(hits)
            .allSatisfy(hit ->
                assertThat(hit.evidenceText()).contains("alias")
            );

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("candidate.alias = $text OR $text IN coalesce(candidate.aliases, [])")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC");
        assertThat(parameters.getValue())
            .containsEntry("text", "must refund")
            .containsEntry("syntaxRole", "ACTION");
        verify(session).close();
    }

    @Test
    void returnsEmptyList_whenGraphHasNoAliasHits() {
        // Given
        when(result.stream()).thenReturn(Stream.empty());

        // When
        var hits = lookup.findCandidates(
            new SelectedTerm("ACTION", "must refund"),
            CandidateLookupScope.forSelectedTerm(new SelectedTerm("ACTION", "must refund"))
        );

        // Then
        assertThat(hits).isEmpty();
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

    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
