package io.fekav.req.shared.kg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.summary.ResultSummary;

@ExtendWith(MockitoExtension.class)
class Neo4jSchemaInitializerTest {

    @Mock
    Driver driver;

    @Mock
    Session session;

    @Mock
    TransactionContext transaction;

    @Mock
    Result result;

    @Mock
    ResultSummary resultSummary;

    Neo4jSchemaInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new Neo4jSchemaInitializer(driver);
        when(driver.session()).thenReturn(session);
        when(session.run(anyString())).thenReturn(result);
        when(result.consume()).thenReturn(resultSummary);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeWrite(any());
    }

    @Test
    void prunesGraphAndCreatesSchemaOnStartupByDefault() {
        // When
        initializer.onStart(null);

        // Then
        assertThat(capturedSessionStatements())
            .containsExactly(
                "MATCH (n) DETACH DELETE n",
                "CREATE CONSTRAINT requirement_id IF NOT EXISTS FOR (r:Requirement) REQUIRE r.id IS UNIQUE",
                "CREATE CONSTRAINT provenance_id IF NOT EXISTS FOR (p:Provenance) REQUIRE p.id IS UNIQUE",
                "CREATE CONSTRAINT concept_canonical_name IF NOT EXISTS FOR (c:Concept) REQUIRE c.canonicalName IS UNIQUE",
                "CREATE CONSTRAINT predicate_canonical_name IF NOT EXISTS FOR (p:Predicate) REQUIRE p.canonicalName IS UNIQUE",
                "CREATE CONSTRAINT qualifier_identity IF NOT EXISTS FOR (q:Qualifier) REQUIRE (q.qualifierKind, q.canonicalText) IS UNIQUE",
                "CREATE CONSTRAINT assertion_key IF NOT EXISTS FOR (a:Assertion) REQUIRE a.assertionKey IS UNIQUE",
                "CREATE CONSTRAINT requirement_type_code IF NOT EXISTS FOR (t:RequirementType) REQUIRE t.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_type_label IF NOT EXISTS FOR (t:RequirementType) REQUIRE t.label IS UNIQUE",
                "CREATE CONSTRAINT requirement_property_code IF NOT EXISTS FOR (p:RequirementProperty) REQUIRE p.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_property_label IF NOT EXISTS FOR (p:RequirementProperty) REQUIRE p.label IS UNIQUE",
                "CREATE CONSTRAINT requirement_relation_type_code IF NOT EXISTS FOR (t:RequirementRelationType) REQUIRE t.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_relation_type_label IF NOT EXISTS FOR (t:RequirementRelationType) REQUIRE t.label IS UNIQUE",
                "CREATE CONSTRAINT allowed_requirement_relation_key IF NOT EXISTS FOR (a:AllowedRequirementRelation) REQUIRE (a.sourceTypeCode, a.relationTypeCode, a.targetTypeCode) IS UNIQUE",
                "CREATE INDEX requirement_type IF NOT EXISTS FOR (r:Requirement) ON (r.type)",
                "CREATE INDEX requirement_property IF NOT EXISTS FOR (r:Requirement) ON (r.property)",
                "CREATE INDEX requirement_raw_text IF NOT EXISTS FOR (r:Requirement) ON (r.rawText)",
                "CREATE INDEX qualifier_canonical_text IF NOT EXISTS FOR (q:Qualifier) ON (q.canonicalText)"
            )
            .allSatisfy(statement ->
                assertThat(statement)
                    .doesNotContain("RequirementElement")
                    .doesNotContain(":Action")
                    .doesNotContain("actionText")
            );
        verify(session).close();
    }

    @Test
    void skipsGraphPruningWhenConfiguredOff() {
        // Given
        initializer = new Neo4jSchemaInitializer(driver, false);

        // When
        initializer.onStart(null);

        // Then
        assertThat(capturedSessionStatements())
            .doesNotContain("MATCH (n) DETACH DELETE n")
            .startsWith(
                "CREATE CONSTRAINT requirement_id IF NOT EXISTS FOR (r:Requirement) REQUIRE r.id IS UNIQUE"
            );
    }

    @Test
    void seedsOntologyOnStartup() {
        // When
        initializer.onStart(null);

        // Then
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction, atLeastOnce()).run(statements.capture(), parameters.capture());

        assertThat(statements.getAllValues())
            .map(this::singleLine)
            .contains(
                "UNWIND $types AS type MERGE (t:RequirementType {code: type.code}) SET t.label = type.label",
                "UNWIND $properties AS property MERGE (p:RequirementProperty {code: property.code}) SET p.label = property.label",
                "UNWIND $relationTypes AS relationType MERGE (t:RequirementRelationType {code: relationType.code}) SET t.label = relationType.label",
                "UNWIND $allowedRelations AS relation MERGE (:AllowedRequirementRelation { sourceTypeCode: relation.sourceTypeCode, relationTypeCode: relation.relationTypeCode, targetTypeCode: relation.targetTypeCode })"
            );

        List<Map<String, Object>> parameterValues = parameters.getAllValues();

        assertThat(stringMapList(parameterValue(parameterValues, "types")))
            .contains(
                Map.of("code", "GOAL", "label", "Goal"),
                Map.of("code", "NEED", "label", "Need"),
                Map.of("code", "REQUIREMENT", "label", "Requirement")
            );

        assertThat(stringMapList(parameterValue(parameterValues, "properties")))
            .contains(
                Map.of("code", "FUNCTIONAL", "label", "Functional"),
                Map.of("code", "QUALITY", "label", "Quality")
            );
        assertThat(stringMapList(parameterValue(parameterValues, "relationTypes")))
            .contains(
                Map.of("code", "REFINES", "label", "Refines"),
                Map.of("code", "SATISFIES", "label", "Satisfies"),
                Map.of("code", "CONFLICTS_WITH", "label", "Conflicts with"),
                Map.of("code", "DEPENDS_ON", "label", "Depends on")
            );
        assertThat(stringMapList(parameterValue(parameterValues, "allowedRelations")))
            .contains(
                Map.of(
                    "sourceTypeCode",
                    "NEED",
                    "relationTypeCode",
                    "SATISFIES",
                    "targetTypeCode",
                    "GOAL"
                ),
                Map.of(
                    "sourceTypeCode",
                    "REQUIREMENT",
                    "relationTypeCode",
                    "REFINES",
                    "targetTypeCode",
                    "NEED"
                ),
                Map.of(
                    "sourceTypeCode",
                    "REQUIREMENT",
                    "relationTypeCode",
                    "DEPENDS_ON",
                    "targetTypeCode",
                    "REQUIREMENT"
                ),
                Map.of(
                    "sourceTypeCode",
                    "REQUIREMENT",
                    "relationTypeCode",
                    "CONFLICTS_WITH",
                    "targetTypeCode",
                    "REQUIREMENT"
                )
            );
    }

    private List<String> capturedSessionStatements() {
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        verify(session, atLeastOnce()).run(statements.capture());
        return statements.getAllValues().stream().map(this::singleLine).toList();
    }

    private Object parameterValue(List<Map<String, Object>> parameters, String key) {
        return parameters
            .stream()
            .filter(parameter -> parameter.containsKey(key))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing parameter key: " + key))
            .get(key);
    }

    private String singleLine(String statement) {
        return statement.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> stringMapList(Object value) {
        return (List<Map<String, String>>) value;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
