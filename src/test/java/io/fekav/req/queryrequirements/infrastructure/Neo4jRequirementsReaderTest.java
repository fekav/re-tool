package io.fekav.req.queryrequirements.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
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

import io.fekav.req.queryrequirements.application.FindRequirementsQuery;

@ExtendWith(MockitoExtension.class)
class Neo4jRequirementsReaderTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @InjectMocks
    Neo4jRequirementsReader reader;

    @BeforeEach
    void setUp() {
        when(driver.session()).thenReturn(session);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeRead(any());
    }

    @Test
    void resolvesConceptExactlyAfterNormalizationAndLoadsConceptRequirements() {
        // Given
        Result conceptRequirements = result(Stream.of(
            requirementRecord(
                "Der Zahlungsdienst muss Lastschriften ausführen.",
                "REQUIREMENT",
                "FUNCTIONAL"
            )
        ));
        when(transaction.run(anyString(), anyMap())).thenReturn(conceptRequirements);

        // When
        var result = reader.read(new FindRequirementsQuery("  Zählungsdienst  "));

        // Then
        assertThat(result.requirements()).hasSize(1);
        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(statement.capture(), parameters.capture());
        assertThat(singleLine(statement.getValue()))
            .contains("seed.canonicalName = $conceptName");
        assertThat(parameters.getValue())
            .containsEntry("conceptName", "zahlungsdienst");
    }

    @Test
    void returnsEmptyResultWhenNoConceptMatches() {
        // Given
        Result emptyResult = result(Stream.empty());
        when(transaction.run(anyString(), anyMap())).thenReturn(emptyResult);

        // When
        var result = reader.read(new FindRequirementsQuery("payments"));

        // Then
        assertThat(result.requirements()).isEmpty();
    }

    private Result result(Stream<Record> records) {
        Result result = org.mockito.Mockito.mock(Result.class);
        when(result.stream()).thenReturn(records);
        return result;
    }

    private Record conceptRecord(String canonicalName) {
        return new InternalRecord(
            java.util.List.of("canonicalName"),
            java.util.List.of(value(canonicalName))
        );
    }

    private Record requirementRecord(
        String rawText,
        String type,
        String property
    ) {
        return new InternalRecord(
            java.util.List.of("rawText", "type", "property"),
            java.util.List.of(value(rawText), value(type), value(property))
        );
    }

    private Value value(String value) {
        return Values.value(value);
    }

    private String singleLine(String statement) {
        return statement.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
