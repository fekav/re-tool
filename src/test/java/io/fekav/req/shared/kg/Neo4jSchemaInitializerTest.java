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

    private static final String AUTOMATIC_RETRY_ASSERTION_KEY =
        "payment recovery service|schedule|payment retries";
    private static final String QUARANTINE_ANOMALIES_ASSERTION_KEY =
        "payment recovery service|quarantine|failed renewal payments";

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
                "CREATE CONSTRAINT mention_id IF NOT EXISTS FOR (m:Mention) REQUIRE m.id IS UNIQUE",
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
                "CREATE INDEX mention_text IF NOT EXISTS FOR (m:Mention) ON (m.text)",
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
    void seedsOntologyAndSampleRequirementGraphOnStartup() {
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
                "UNWIND $allowedRelations AS relation MERGE (:AllowedRequirementRelation { sourceTypeCode: relation.sourceTypeCode, relationTypeCode: relation.relationTypeCode, targetTypeCode: relation.targetTypeCode })",
                "UNWIND $requirements AS requirement MERGE (r:Requirement {id: requirement.id}) SET r.rawText = requirement.rawText, r.type = requirement.type, r.property = requirement.property, r.sample = true MERGE (p:Provenance {id: requirement.provenanceId}) SET p.source = 'sample', p.rawText = requirement.rawText MERGE (r)-[:HAS_PROVENANCE]->(p)",
                "UNWIND $assertions AS assertion MATCH (r:Requirement {id: assertion.requirementId}) MERGE (subject:Concept {canonicalName: assertion.subjectName}) SET subject.sample = true MERGE (predicate:Predicate {canonicalName: assertion.predicateName}) SET predicate.sample = true MERGE (object:Concept {canonicalName: assertion.objectName}) SET object.sample = true MERGE (a:Assertion {assertionKey: assertion.assertionKey}) SET a.subjectCanonicalName = assertion.subjectName, a.predicateCanonicalName = assertion.predicateName, a.objectCanonicalName = assertion.objectName, a.sample = true MERGE (r)-[:ASSERTS]->(a) MERGE (a)-[:HAS_SUBJECT]->(subject) MERGE (a)-[:HAS_PREDICATE]->(predicate) MERGE (a)-[:HAS_OBJECT]->(object)",
                "UNWIND $assertionQualifiers AS qualifier MATCH (a:Assertion {assertionKey: qualifier.assertionKey}) MERGE (q:Qualifier { qualifierKind: qualifier.qualifierKind, canonicalText: qualifier.canonicalText }) SET q.sample = true MERGE (a)-[:HAS_QUALIFIER]->(q)",
                "UNWIND $conceptMentions AS mention MATCH (r:Requirement {id: mention.requirementId}) MATCH (c:Concept {canonicalName: mention.canonicalName}) MERGE (m:Mention {id: mention.id}) SET m.text = mention.text, m.role = mention.role, m.sample = true MERGE (r)-[:HAS_MENTION]->(m) MERGE (m)-[:DENOTES]->(c)",
                "UNWIND $predicateMentions AS mention MATCH (r:Requirement {id: mention.requirementId}) MATCH (p:Predicate {canonicalName: mention.canonicalName}) MERGE (m:Mention {id: mention.id}) SET m.text = mention.text, m.role = 'ACTION', m.sample = true MERGE (r)-[:HAS_MENTION]->(m) MERGE (m)-[:DENOTES]->(p)",
                "UNWIND $qualifierMentions AS mention MATCH (r:Requirement {id: mention.requirementId}) MATCH (q:Qualifier { qualifierKind: mention.qualifierKind, canonicalText: mention.canonicalText }) MERGE (m:Mention {id: mention.id}) SET m.text = mention.text, m.role = mention.qualifierKind, m.sample = true MERGE (r)-[:HAS_MENTION]->(m) MERGE (m)-[:DENOTES]->(q)",
                "UNWIND $satisfiesRelations AS relation MATCH (need:Requirement {id: relation.sourceRequirementId}) MATCH (goal:Requirement {id: relation.targetRequirementId}) MERGE (need)-[:SATISFIES]->(goal)",
                "UNWIND $refinesRelations AS relation MATCH (requirement:Requirement {id: relation.sourceRequirementId}) MATCH (need:Requirement {id: relation.targetRequirementId}) MERGE (requirement)-[:REFINES]->(need)",
                "UNWIND $dependsOnRelations AS relation MATCH (source:Requirement {id: relation.sourceRequirementId}) MATCH (target:Requirement {id: relation.targetRequirementId}) MERGE (source)-[:DEPENDS_ON]->(target)",
                "UNWIND $conflictsWithRelations AS relation MATCH (source:Requirement {id: relation.sourceRequirementId}) MATCH (target:Requirement {id: relation.targetRequirementId}) MERGE (source)-[:CONFLICTS_WITH]->(target)"
            )
            .allSatisfy(statement ->
                assertThat(statement)
                    .doesNotContain("RequirementElement")
                    .doesNotContain("HAS_ACTION")
                    .doesNotContain("actionText")
            );

        List<Map<String, Object>> parameterValues = parameters.getAllValues();

        assertThat(stringMapList(parameterValue(parameterValues, "types")))
            .contains(
                Map.of("code", "GOAL", "label", "Goal"),
                Map.of("code", "NEED", "label", "Need"),
                Map.of("code", "REQUIREMENT", "label", "Requirement")
            );

        List<Map<String, String>> requirements =
            stringMapList(parameterValue(parameterValues, "requirements"));
        assertThat(requirements).hasSize(6);
        assertThat(requirements)
            .extracting(requirement ->
                requirement.get("type") + "/" + requirement.get("property")
            )
            .containsExactlyInAnyOrder(
                "GOAL/FUNCTIONAL",
                "GOAL/QUALITY",
                "NEED/FUNCTIONAL",
                "NEED/QUALITY",
                "REQUIREMENT/FUNCTIONAL",
                "REQUIREMENT/QUALITY"
            );
        assertThat(requirements)
            .contains(
                Map.of(
                    "id",
                    "sample-requirement-automatic-retry",
                    "rawText",
                    "The payment recovery service must schedule one retry after every failed renewal payment is recorded.",
                    "type",
                    "REQUIREMENT",
                    "property",
                    "FUNCTIONAL",
                    "provenanceId",
                    "sample-provenance-requirement-automatic-retry"
                ),
                Map.of(
                    "id",
                    "sample-requirement-quarantine-anomalies",
                    "rawText",
                    "The payment recovery service must quarantine failed renewal payments that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late.",
                    "type",
                    "REQUIREMENT",
                    "property",
                    "QUALITY",
                    "provenanceId",
                    "sample-provenance-requirement-quarantine-anomalies"
                )
            );

        List<Map<String, String>> assertions =
            stringMapList(parameterValue(parameterValues, "assertions"));
        assertThat(assertions)
            .hasSize(2)
            .contains(
                Map.of(
                    "requirementId",
                    "sample-requirement-automatic-retry",
                    "assertionKey",
                    AUTOMATIC_RETRY_ASSERTION_KEY,
                    "subjectName",
                    "payment recovery service",
                    "predicateName",
                    "schedule",
                    "objectName",
                    "payment retries"
                ),
                Map.of(
                    "requirementId",
                    "sample-requirement-quarantine-anomalies",
                    "assertionKey",
                    QUARANTINE_ANOMALIES_ASSERTION_KEY,
                    "subjectName",
                    "payment recovery service",
                    "predicateName",
                    "quarantine",
                    "objectName",
                    "failed renewal payments"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "assertionQualifiers")))
            .contains(
                Map.of(
                    "assertionKey",
                    AUTOMATIC_RETRY_ASSERTION_KEY,
                    "qualifierKind",
                    "CONDITION",
                    "canonicalText",
                    "after every failed renewal payment is recorded"
                ),
                Map.of(
                    "assertionKey",
                    QUARANTINE_ANOMALIES_ASSERTION_KEY,
                    "qualifierKind",
                    "CONDITION",
                    "canonicalText",
                    "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "conceptMentions")))
            .contains(
                Map.of(
                    "id",
                    "sample-mention-retry-subject",
                    "requirementId",
                    "sample-requirement-automatic-retry",
                    "role",
                    "SUBJECT",
                    "text",
                    "payment recovery service",
                    "canonicalName",
                    "payment recovery service"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "predicateMentions")))
            .contains(
                Map.of(
                    "id",
                    "sample-mention-quarantine-predicate",
                    "requirementId",
                    "sample-requirement-quarantine-anomalies",
                    "text",
                    "quarantine",
                    "canonicalName",
                    "quarantine"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "qualifierMentions")))
            .contains(
                Map.of(
                    "id",
                    "sample-mention-quarantine-anomaly-condition",
                    "requirementId",
                    "sample-requirement-quarantine-anomalies",
                    "qualifierKind",
                    "CONDITION",
                    "text",
                    "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late",
                    "canonicalText",
                    "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "satisfiesRelations")))
            .containsExactlyInAnyOrder(
                requirementRelation(
                    "sample-need-automatic-retry-decision",
                    "sample-goal-payment-recovery-flow"
                ),
                requirementRelation(
                    "sample-need-payment-anomaly-evidence",
                    "sample-goal-payment-recovery-resilience"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "refinesRelations")))
            .containsExactlyInAnyOrder(
                requirementRelation(
                    "sample-requirement-automatic-retry",
                    "sample-need-automatic-retry-decision"
                ),
                requirementRelation(
                    "sample-requirement-quarantine-anomalies",
                    "sample-need-payment-anomaly-evidence"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "dependsOnRelations")))
            .containsExactly(
                requirementRelation(
                    "sample-requirement-automatic-retry",
                    "sample-requirement-quarantine-anomalies"
                )
            );

        assertThat(stringMapList(parameterValue(parameterValues, "conflictsWithRelations")))
            .containsExactly(
                requirementRelation(
                    "sample-requirement-quarantine-anomalies",
                    "sample-requirement-automatic-retry"
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

    private Map<String, String> requirementRelation(
        String sourceRequirementId,
        String targetRequirementId
    ) {
        return Map.of(
            "sourceRequirementId",
            sourceRequirementId,
            "targetRequirementId",
            targetRequirementId
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
