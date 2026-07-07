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

import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

@ExtendWith(MockitoExtension.class)
class Neo4jNodeNameLookupTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @Mock
    Result result;

    @InjectMocks
    Neo4jNodeNameLookup lookup;

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
    void returnsConceptCandidatesForSubjectTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("billing service", "billing service", "CONCEPT"),
            record("payment adapter", "payment adapter", "CONCEPT")
        ));

        // When
        var candidates = lookup.findCandidates(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service")
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(
                tuple("billing service", "billing service", NodeType.CONCEPT),
                tuple("payment adapter", "payment adapter", NodeType.CONCEPT)
            );

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Concept)")
            .contains("candidate.canonicalName = $text")
            .contains("RETURN candidateKey, candidateLabel, 'CONCEPT' AS nodeType")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("candidate.actionText")
            .doesNotContain("RequirementElement")
            .doesNotContain("toLower")
            .doesNotContain("toLowerCase");
        assertThat(parameters.getValue())
            .containsEntry("text", "billing service")
            .containsOnlyKeys("text");
        verify(session).close();
    }

    @Test
    void returnsPredicateCandidatesForActionTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("log", "log", "PREDICATE")
        ));

        // When
        var candidates = lookup.findCandidates(
            new RequirementElement(RequirementElementType.ACTION, "log")
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple("log", "log", NodeType.PREDICATE));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Predicate)")
            .contains("candidate.canonicalName = $text")
            .contains("RETURN candidateKey, candidateLabel, 'PREDICATE' AS nodeType")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("candidate.actionText")
            .doesNotContain("RequirementElement")
            .doesNotContain("toLower");
        assertThat(parameters.getValue())
            .containsEntry("text", "log")
            .containsOnlyKeys("text");
        verify(session).close();
    }

    @Test
    void returnsQualifierCandidatesForConditionTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record(
                "CONDITION::before the billing service logs a failed payment attempt",
                "before the billing service logs a failed payment attempt",
                "QUALIFIER"
            )
        ));

        // When
        var candidates = lookup.findCandidates(new RequirementElement(
            RequirementElementType.CONDITION,
            "before the billing service logs a failed payment attempt"
        ));

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple(
                "CONDITION::before the billing service logs a failed payment attempt",
                "before the billing service logs a failed payment attempt",
                NodeType.QUALIFIER
            ));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Qualifier)")
            .contains("candidate.canonicalText = $text")
            .contains("candidate.qualifierKind = $qualifierKind")
            .contains("RETURN candidateKey, candidateLabel, 'QUALIFIER' AS nodeType")
            .doesNotContain("RequirementElement")
            .doesNotContain("Action")
            .doesNotContain("toLower");
        assertThat(parameters.getValue())
            .containsEntry(
                "text",
                "before the billing service logs a failed payment attempt"
            )
            .containsEntry("qualifierKind", "CONDITION")
            .containsOnlyKeys("text", "qualifierKind");
    }

    @Test
    void returnsEmptyList_whenGraphHasNoNodeNameHits() {
        // Given
        when(result.stream()).thenReturn(Stream.empty());

        // When
        var candidates = lookup.findCandidates(new RequirementElement(RequirementElementType.ACTION, "must refund"));

        // Then
        assertThat(candidates).isEmpty();
    }

    @Test
    void returnsCompatibleConceptCandidatesForSubjectTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("billing service", "billing service", "CONCEPT"),
            record("payment adapter", "payment adapter", "CONCEPT")
        ));

        // When
        var candidates = lookup.findCompatibleCandidates(
            new RequirementElement(RequirementElementType.SUBJECT, "billing")
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(
                tuple("billing service", "billing service", NodeType.CONCEPT),
                tuple("payment adapter", "payment adapter", NodeType.CONCEPT)
            );

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Concept)")
            .contains("RETURN candidateKey, candidateLabel, 'CONCEPT' AS nodeType")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("WHERE candidate.canonicalName = $text")
            .doesNotContain("candidate.qualifierKind = $qualifierKind");
        assertThat(parameters.getValue()).isEmpty();
    }

    @Test
    void returnsCompatibleConceptCandidatesForObjectTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("invoice", "invoice", "CONCEPT")
        ));

        // When
        var candidates = lookup.findCompatibleCandidates(
            new RequirementElement(RequirementElementType.OBJECT, "invoice record")
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple("invoice", "invoice", NodeType.CONCEPT));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Concept)")
            .contains("RETURN candidateKey, candidateLabel, 'CONCEPT' AS nodeType")
            .doesNotContain("WHERE candidate.canonicalName = $text");
        assertThat(parameters.getValue()).isEmpty();
    }

    @Test
    void returnsCompatiblePredicateCandidatesForActionTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record("log", "log", "PREDICATE")
        ));

        // When
        var candidates = lookup.findCompatibleCandidates(
            new RequirementElement(RequirementElementType.ACTION, "log event")
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple("log", "log", NodeType.PREDICATE));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Predicate)")
            .contains("RETURN candidateKey, candidateLabel, 'PREDICATE' AS nodeType")
            .contains("ORDER BY candidateLabel ASC, candidateKey ASC")
            .doesNotContain("WHERE candidate.canonicalName = $text")
            .doesNotContain("candidate.qualifierKind = $qualifierKind");
        assertThat(parameters.getValue()).isEmpty();
    }

    @Test
    void returnsCompatibleQualifierCandidatesForConditionTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record(
                "CONDITION::before payment capture",
                "before payment capture",
                "QUALIFIER"
            )
        ));

        // When
        var candidates = lookup.findCompatibleCandidates(
            new RequirementElement(
                RequirementElementType.CONDITION,
                "before the billing service logs"
            )
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple(
                "CONDITION::before payment capture",
                "before payment capture",
                NodeType.QUALIFIER
            ));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Qualifier)")
            .contains("candidate.qualifierKind = $qualifierKind")
            .contains("RETURN candidateKey, candidateLabel, 'QUALIFIER' AS nodeType")
            .doesNotContain("candidate.canonicalText = $text");
        assertThat(parameters.getValue())
            .containsEntry("qualifierKind", "CONDITION")
            .containsOnlyKeys("qualifierKind");
    }

    @Test
    void returnsCompatibleQualifierCandidatesForConstraintTerms() {
        // Given
        when(result.stream()).thenReturn(Stream.of(
            record(
                "CONSTRAINT::within two seconds",
                "within two seconds",
                "QUALIFIER"
            )
        ));

        // When
        var candidates = lookup.findCompatibleCandidates(
            new RequirementElement(
                RequirementElementType.CONSTRAINT,
                "within two seconds"
            )
        );

        // Then
        assertThat(candidates)
            .extracting(
                candidate -> candidate.candidateKey(),
                candidate -> candidate.label(),
                candidate -> candidate.nodeType()
            )
            .containsExactly(tuple(
                "CONSTRAINT::within two seconds",
                "within two seconds",
                NodeType.QUALIFIER
            ));

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(singleLine(query.getValue()))
            .contains("MATCH (candidate:Qualifier)")
            .contains("candidate.qualifierKind = $qualifierKind")
            .doesNotContain("candidate.canonicalText = $text");
        assertThat(parameters.getValue())
            .containsEntry("qualifierKind", "CONSTRAINT")
            .containsOnlyKeys("qualifierKind");
    }

    private Record record(String candidateKey, String label, String nodeType) {
        return new InternalRecord(
            java.util.List.of("candidateKey", "candidateLabel", "nodeType"),
            java.util.List.of(value(candidateKey), value(label), value(nodeType))
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
