package io.fekav.req.graphchange.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.graphchange.application.PersistRequirementGraphChange;
import io.fekav.req.graphchange.application.RequirementGraphChangeResult;
import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.graphchange.domain.GraphQualifier;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.SourceMetadata;

@ExtendWith(MockitoExtension.class)
class Neo4jRequirementGraphChangeAdapterTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @InjectMocks
    Neo4jRequirementGraphChangeAdapter adapter;

    @BeforeEach
    void setUp() {
        when(driver.session()).thenReturn(session);
    }

    @Test
    void returnsUnchanged_whenRequirementWasAlreadyPersisted() {
        // Arrange
        readTransactions();
        Result requirementExists = booleanResult(true);
        when(transaction.run(anyString(), anyMap()))
            .thenReturn(requirementExists);
        PersistRequirementGraphChange graphChange = graphChange(List.of());

        // Act
        RequirementGraphChangeResult result = adapter.persist(graphChange);

        // Assert
        assertThat(result).isEqualTo(RequirementGraphChangeResult.unchanged());
        verify(session, never()).executeWrite(any());
        verify(session).close();
    }

    @Test
    void returnsKnown_whenAssertionAlreadyExistsForSubjectPredicateObject() {
        // Arrange
        readTransactions();
        Result requirementMissing = booleanResult(false);
        Result assertionExists = booleanResult(true);
        when(transaction.run(anyString(), anyMap()))
            .thenReturn(requirementMissing, assertionExists);
        PersistRequirementGraphChange graphChange = graphChange(List.of());

        // Act
        RequirementGraphChangeResult result = adapter.persist(graphChange);

        // Assert
        assertThat(result)
            .isEqualTo(RequirementGraphChangeResult.known(graphChange.assertionIdentity()));
        verify(session, never()).executeWrite(any());

        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction, org.mockito.Mockito.times(2))
            .run(anyString(), parameters.capture());
        assertThat(parameters.getAllValues().get(1))
            .containsEntry("assertionKey", graphChange.assertionIdentity().assertionKey())
            .containsOnlyKeys("assertionKey");
    }

    @Test
    void writesRequirementAndAssertionInOneTransaction_whenAssertionIsNew() {
        // Arrange
        readTransactions();
        writeTransactions();
        Result requirementMissing = booleanResult(false);
        Result assertionMissing = booleanResult(false);
        Result writeResult = writeResult();
        when(transaction.run(anyString(), anyMap()))
            .thenReturn(requirementMissing, assertionMissing, writeResult);
        PersistRequirementGraphChange graphChange = graphChange(List.of());

        // Act
        RequirementGraphChangeResult result = adapter.persist(graphChange);

        // Assert
        assertThat(result).isEqualTo(RequirementGraphChangeResult.created());

        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction, org.mockito.Mockito.times(3))
            .run(statements.capture(), parameters.capture());
        assertThat(statements.getAllValues())
            .map(this::singleLine)
            .anySatisfy(statement ->
                assertThat(statement)
                    .contains("MATCH (r:Requirement {id: $requirementId})")
                    .contains("RETURN count(r) > 0 AS exists")
            )
            .anySatisfy(statement ->
                assertThat(statement)
                    .contains("MATCH (a:Assertion {assertionKey: $assertionKey})")
                    .contains("RETURN count(a) > 0 AS exists")
            )
            .anySatisfy(statement ->
                assertThat(statement)
                    .contains("MERGE (r:Requirement {id: $requirementId})")
                    .contains("r.rawText = $rawText")
                    .contains("r.type = $type")
                    .contains("r.property = $property")
                    .contains("MERGE (p:Provenance {id: $provenanceId})")
                    .contains("MERGE (subject:Concept {canonicalName: $subjectKey})")
                    .contains("MERGE (predicate:Predicate {canonicalName: $predicateKey})")
                    .contains("MERGE (object:Concept {canonicalName: $objectKey})")
                    .contains("MERGE (a:Assertion {assertionKey: $assertionKey})")
                    .contains("MERGE (r)-[:ASSERTS]->(a)")
                    .contains("MERGE (a)-[:HAS_SUBJECT]->(subject)")
                    .contains("MERGE (a)-[:HAS_PREDICATE]->(predicate)")
                    .contains("MERGE (a)-[:HAS_OBJECT]->(object)")
                    .contains("UNWIND $qualifiers AS qualifier")
            );

        assertThat(parameters.getAllValues().get(2))
            .containsEntry("requirementId", graphChange.requirementId())
            .containsEntry("rawText", graphChange.provenance().getOriginalText().text())
            .containsEntry("source", "API-Request")
            .containsEntry("type", "REQUIREMENT")
            .containsEntry("property", "FUNCTIONAL")
            .containsEntry("assertionKey", "login form|must validate|credentials")
            .containsEntry("subjectKey", "login form")
            .containsEntry("predicateKey", "must validate")
            .containsEntry("objectKey", "credentials")
            .containsEntry("qualifiers", List.of());
    }

    @Test
    void writesQualifiersWithAssertion_whenConditionAndConstraintArePresent() {
        // Arrange
        readTransactions();
        writeTransactions();
        Result requirementMissing = booleanResult(false);
        Result assertionMissing = booleanResult(false);
        Result writeResult = writeResult();
        when(transaction.run(anyString(), anyMap()))
            .thenReturn(requirementMissing, assertionMissing, writeResult);
        PersistRequirementGraphChange graphChange = graphChange(List.of(
            qualifier(RequirementElementType.CONDITION, "before authentication"),
            qualifier(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
        ));

        // Act
        adapter.persist(graphChange);

        // Assert
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction, org.mockito.Mockito.times(3))
            .run(anyString(), parameters.capture());
        assertThat(qualifierParameters(parameters.getAllValues().get(2)))
            .containsExactly(
                Map.of(
                    "qualifierKind",
                    "CONDITION",
                    "canonicalText",
                    "before authentication"
                ),
                Map.of(
                    "qualifierKind",
                    "CONSTRAINT",
                    "canonicalText",
                    "within 200 milliseconds"
                )
            );
    }

    private void readTransactions() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeRead(any());
    }

    private void writeTransactions() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeWrite(any());
    }

    private PersistRequirementGraphChange graphChange(List<GraphQualifier> qualifiers) {
        CorrelationId correlationId =
            new CorrelationId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        return new PersistRequirementGraphChange(
            correlationId,
            correlationId.value().toString(),
            provenance(),
            classification(),
            assertionIdentity(),
            qualifiers
        );
    }

    private Provenance provenance() {
        return Provenance.create(
            ElementId.create(),
            new OriginalText("The login form must validate credentials."),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        );
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The sentence expresses a verifiable obligation.")
        );
    }

    private AssertionIdentity assertionIdentity() {
        return new AssertionIdentity(
            new GraphNodeReference(NodeType.CONCEPT, "login form", "login form"),
            new GraphNodeReference(NodeType.PREDICATE, "must validate", "must validate"),
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "credentials")
        );
    }

    private GraphQualifier qualifier(RequirementElementType kind, String canonicalText) {
        return new GraphQualifier(
            kind,
            new GraphNodeReference(
                NodeType.QUALIFIER,
                kind.name() + "::" + canonicalText,
                canonicalText
            )
        );
    }

    private Result booleanResult(boolean value) {
        Result result = org.mockito.Mockito.mock(Result.class);
        when(result.single()).thenReturn(record(value));
        return result;
    }

    private Result writeResult() {
        return org.mockito.Mockito.mock(Result.class);
    }

    private Record record(boolean exists) {
        return new InternalRecord(
            java.util.List.of("exists"),
            java.util.List.of(value(exists))
        );
    }

    private Value value(boolean value) {
        return Values.value(value);
    }

    private String singleLine(String statement) {
        return statement.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> qualifierParameters(Map<String, Object> parameters) {
        return (List<Map<String, String>>) parameters.get("qualifiers");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
