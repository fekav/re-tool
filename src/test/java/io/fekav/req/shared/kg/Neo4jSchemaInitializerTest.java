package io.fekav.req.shared.kg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    private static final String RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY =
        "billing service|record|failed payment attempts";

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

    @InjectMocks
    Neo4jSchemaInitializer initializer;

    @BeforeEach
    void setUp() {
        when(driver.session()).thenReturn(session);
        when(session.run(anyString())).thenReturn(result);
        when(result.consume()).thenReturn(resultSummary);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(transaction);
        }).when(session).executeWrite(any());
    }

    @Test
    void createsGraphModelConstraintsAndIndexesOnStartup() {
        // When
        initializer.onStart(null);

        // Then
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).run(statements.capture());
        assertThat(statements.getAllValues())
            .map(this::singleLine)
            .containsExactly(
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
    void seedsOntologyAndSampleRequirementGraphOnStartup() {
        // When
        initializer.onStart(null);

        // Then
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameters = parametersCaptor();
        verify(transaction, org.mockito.Mockito.atLeastOnce())
            .run(statements.capture(), parameters.capture());

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
                "UNWIND $dependsOnRelations AS relation MATCH (source:Requirement {id: relation.sourceRequirementId}) MATCH (target:Requirement {id: relation.targetRequirementId}) MERGE (source)-[:DEPENDS_ON]->(target)"
            )
            .allSatisfy(statement ->
                assertThat(statement)
                    .doesNotContain("RequirementElement")
                    .doesNotContain(":Action")
                    .doesNotContain("HAS_ACTION")
                    .doesNotContain("actionText")
                    .doesNotContain("[:CONFLICTS_WITH]")
            );

        assertThat(parameters.getAllValues())
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("types")
                    .extractingByKey("types")
                    .asList()
                    .contains(
                        Map.of("code", "GOAL", "label", "Goal"),
                        Map.of("code", "NEED", "label", "Need"),
                        Map.of("code", "REQUIREMENT", "label", "Requirement")
                    )
            )
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("requirements")
                    .extractingByKey("requirements")
                    .asList()
                    .hasSize(12)
                    .contains(
                        Map.of(
                            "id",
                            "sample-goal-payment-support",
                            "rawText",
                            "Reduce payment-support escalations caused by failed payments by 25% in Q3.",
                            "type",
                            "GOAL",
                            "property",
                            "QUALITY",
                            "provenanceId",
                            "sample-provenance-goal-payment-support"
                        ),
                        Map.of(
                            "id",
                            "sample-need-audit-evidence",
                            "rawText",
                            "Finance auditors need complete evidence for failed payment attempts that affect customer invoices.",
                            "type",
                            "NEED",
                            "property",
                            "QUALITY",
                            "provenanceId",
                            "sample-provenance-need-audit-evidence"
                        ),
                        Map.of(
                            "id",
                            "sample-requirement-audit-record",
                            "rawText",
                            "The billing service must record failed payment attempts for audit review.",
                            "type",
                            "REQUIREMENT",
                            "property",
                            "FUNCTIONAL",
                            "provenanceId",
                            "sample-provenance-requirement-audit-record"
                        )
                    )
            )
            .anySatisfy(parameter -> {
                assertThat(parameter).containsKey("assertions");
                List<Map<String, String>> assertions =
                    stringMapList(parameter.get("assertions"));
                assertThat(assertions).hasSize(9);
                assertThat(assertions)
                    .filteredOn(assertion ->
                        RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY.equals(
                            assertion.get("assertionKey")
                        )
                    )
                    .extracting(assertion -> assertion.get("requirementId"))
                    .containsExactly(
                        "sample-requirement-record-failure",
                        "sample-requirement-audit-record"
                    );
            })
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("assertionQualifiers")
                    .extractingByKey("assertionQualifiers")
                    .asList()
                    .contains(
                        Map.of(
                            "assertionKey",
                            RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY,
                            "qualifierKind",
                            "CONSTRAINT",
                            "canonicalText",
                            "with provider code, decline reason, payment method, and customer account"
                        ),
                        Map.of(
                            "assertionKey",
                            RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY,
                            "qualifierKind",
                            "CONSTRAINT",
                            "canonicalText",
                            "for audit review"
                        )
                    )
            )
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("conceptMentions")
                    .extractingByKey("conceptMentions")
                    .asList()
                    .contains(Map.of(
                        "id",
                        "sample-mention-record-subject",
                        "requirementId",
                        "sample-requirement-record-failure",
                        "role",
                        "SUBJECT",
                        "text",
                        "billing service",
                        "canonicalName",
                        "billing service"
                    ))
            )
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("predicateMentions")
                    .extractingByKey("predicateMentions")
                    .asList()
                    .contains(Map.of(
                        "id",
                        "sample-mention-record-predicate",
                        "requirementId",
                        "sample-requirement-record-failure",
                        "text",
                        "record",
                        "canonicalName",
                        "record"
                    ))
            )
            .anySatisfy(parameter ->
                assertThat(parameter)
                    .containsKey("qualifierMentions")
                    .extractingByKey("qualifierMentions")
                    .asList()
                    .contains(Map.of(
                        "id",
                        "sample-mention-normalize-condition",
                        "requirementId",
                        "sample-requirement-normalize-codes",
                        "qualifierKind",
                        "CONDITION",
                        "text",
                        "before failed payment attempts are recorded",
                        "canonicalText",
                        "before failed payment attempts are recorded"
                    ))
            )
            .anySatisfy(parameter -> {
                assertThat(parameter).containsKey("satisfiesRelations");
                List<Map<String, String>> relations =
                    stringMapList(parameter.get("satisfiesRelations"));
                assertThat(relations)
                    .hasSize(2)
                    .contains(
                        requirementRelation(
                            "sample-need-support-visibility",
                            "sample-goal-payment-support"
                        ),
                        requirementRelation(
                            "sample-need-audit-evidence",
                            "sample-goal-payment-support"
                        )
                    );
            })
            .anySatisfy(parameter -> {
                assertThat(parameter).containsKey("refinesRelations");
                List<Map<String, String>> relations =
                    stringMapList(parameter.get("refinesRelations"));
                assertThat(relations)
                    .hasSize(10)
                    .contains(
                        requirementRelation(
                            "sample-requirement-record-failure",
                            "sample-need-support-visibility"
                        ),
                        requirementRelation(
                            "sample-requirement-record-failure",
                            "sample-need-audit-evidence"
                        ),
                        requirementRelation(
                            "sample-requirement-audit-retention",
                            "sample-need-audit-evidence"
                        )
                    );
            })
            .anySatisfy(parameter -> {
                assertThat(parameter).containsKey("dependsOnRelations");
                List<Map<String, String>> relations =
                    stringMapList(parameter.get("dependsOnRelations"));
                assertThat(relations)
                    .hasSize(9)
                    .contains(
                        requirementRelation(
                            "sample-requirement-support-dashboard",
                            "sample-requirement-record-failure"
                        ),
                        requirementRelation(
                            "sample-requirement-audit-export",
                            "sample-requirement-audit-retention"
                        ),
                        requirementRelation(
                            "sample-requirement-retry-suppression",
                            "sample-requirement-normalize-codes"
                        )
                    );
            });
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
