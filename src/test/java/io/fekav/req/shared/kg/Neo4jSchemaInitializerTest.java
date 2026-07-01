package io.fekav.req.shared.kg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                "CREATE CONSTRAINT requirement_element_id IF NOT EXISTS FOR (e:RequirementElement) REQUIRE e.id IS UNIQUE",
                "CREATE CONSTRAINT requirement_type_code IF NOT EXISTS FOR (t:RequirementType) REQUIRE t.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_type_label IF NOT EXISTS FOR (t:RequirementType) REQUIRE t.label IS UNIQUE",
                "CREATE CONSTRAINT requirement_property_code IF NOT EXISTS FOR (p:RequirementProperty) REQUIRE p.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_property_label IF NOT EXISTS FOR (p:RequirementProperty) REQUIRE p.label IS UNIQUE",
                "CREATE CONSTRAINT requirement_element_type_code IF NOT EXISTS FOR (e:RequirementElementType) REQUIRE e.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_element_type_label IF NOT EXISTS FOR (e:RequirementElementType) REQUIRE e.label IS UNIQUE",
                "CREATE CONSTRAINT requirement_relation_type_code IF NOT EXISTS FOR (t:RequirementRelationType) REQUIRE t.code IS UNIQUE",
                "CREATE CONSTRAINT requirement_relation_type_label IF NOT EXISTS FOR (t:RequirementRelationType) REQUIRE t.label IS UNIQUE",
                "CREATE CONSTRAINT allowed_requirement_relation_key IF NOT EXISTS FOR (a:AllowedRequirementRelation) REQUIRE (a.sourceTypeCode, a.relationTypeCode, a.targetTypeCode) IS UNIQUE",
                "CREATE INDEX requirement_type IF NOT EXISTS FOR (r:Requirement) ON (r.type)",
                "CREATE INDEX requirement_property IF NOT EXISTS FOR (r:Requirement) ON (r.property)",
                "CREATE INDEX requirement_raw_text IF NOT EXISTS FOR (r:Requirement) ON (r.rawText)",
                "CREATE INDEX requirement_element_text IF NOT EXISTS FOR (e:RequirementElement) ON (e.text)"
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
                "UNWIND $requirementElementTypes AS requirementElementType MERGE (e:RequirementElementType {code: requirementElementType.code}) SET e.label = requirementElementType.label",
                "UNWIND $relationTypes AS relationType MERGE (t:RequirementRelationType {code: relationType.code}) SET t.label = relationType.label",
                "UNWIND $allowedRelations AS relation MERGE (:AllowedRequirementRelation { sourceTypeCode: relation.sourceTypeCode, relationTypeCode: relation.relationTypeCode, targetTypeCode: relation.targetTypeCode })",
                "UNWIND $requirements AS requirement MERGE (r:Requirement {id: requirement.id}) SET r.rawText = requirement.rawText, r.type = requirement.type, r.property = requirement.property, r.sample = true MERGE (p:Provenance {id: requirement.provenanceId}) SET p.source = 'sample', p.rawText = requirement.rawText MERGE (r)-[:HAS_PROVENANCE]->(p)",
                "UNWIND $requirementElements AS element MATCH (r:Requirement {id: element.requirementId}) MERGE (e:RequirementElement {id: element.id}) SET e.type = element.type, e.text = element.text, e.sample = true MERGE (r)-[:HAS_REQUIREMENT_ELEMENT]->(e)",
                "MATCH (need:Requirement {id: $needId}) MATCH (goal:Requirement {id: $goalId}) MERGE (need)-[:SATISFIES]->(goal)",
                "MATCH (requirement:Requirement {id: $requirementId}) MATCH (need:Requirement {id: $needId}) MERGE (requirement)-[:REFINES]->(need)"
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
                    .containsEntry("requirementId", "sample-requirement-1")
                    .containsEntry("needId", "sample-need-1")
            );
    }

    private String singleLine(String statement) {
        return statement.replaceAll("\\s+", " ").trim();
    }

    private ArgumentCaptor<Map<String, Object>> parametersCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
