package io.fekav.req.shared.kg;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

@ApplicationScoped
public class Neo4jSchemaInitializer {

    private static final String SAMPLE_GOAL_ID = "sample-goal-1";
    private static final String SAMPLE_NEED_ID = "sample-need-1";
    private static final String SAMPLE_REQUIREMENT_ID = "sample-requirement-1";

    private static final List<String> SCHEMA_STATEMENTS = List.of(
        """
        CREATE CONSTRAINT requirement_id IF NOT EXISTS
        FOR (r:Requirement) REQUIRE r.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT provenance_id IF NOT EXISTS
        FOR (p:Provenance) REQUIRE p.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT syntax_element_id IF NOT EXISTS
        FOR (e:SyntaxElement) REQUIRE e.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_type_code IF NOT EXISTS
        FOR (t:RequirementType) REQUIRE t.code IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_type_label IF NOT EXISTS
        FOR (t:RequirementType) REQUIRE t.label IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_property_code IF NOT EXISTS
        FOR (p:RequirementProperty) REQUIRE p.code IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_property_label IF NOT EXISTS
        FOR (p:RequirementProperty) REQUIRE p.label IS UNIQUE
        """,
        """
        CREATE CONSTRAINT syntax_role_code IF NOT EXISTS
        FOR (r:SyntaxRole) REQUIRE r.code IS UNIQUE
        """,
        """
        CREATE CONSTRAINT syntax_role_label IF NOT EXISTS
        FOR (r:SyntaxRole) REQUIRE r.label IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_relation_type_code IF NOT EXISTS
        FOR (t:RequirementRelationType) REQUIRE t.code IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_relation_type_label IF NOT EXISTS
        FOR (t:RequirementRelationType) REQUIRE t.label IS UNIQUE
        """,
        """
        CREATE CONSTRAINT allowed_requirement_relation_key IF NOT EXISTS
        FOR (a:AllowedRequirementRelation)
        REQUIRE (a.sourceTypeCode, a.relationTypeCode, a.targetTypeCode) IS UNIQUE
        """,
        """
        CREATE INDEX requirement_type IF NOT EXISTS
        FOR (r:Requirement) ON (r.type)
        """,
        """
        CREATE INDEX requirement_property IF NOT EXISTS
        FOR (r:Requirement) ON (r.property)
        """,
        """
        CREATE INDEX requirement_raw_text IF NOT EXISTS
        FOR (r:Requirement) ON (r.rawText)
        """,
        """
        CREATE INDEX syntax_element_text IF NOT EXISTS
        FOR (e:SyntaxElement) ON (e.text)
        """
    );

    private static final List<Map<String, String>> REQUIREMENT_TYPES = List.of(
        Map.of("code", "GOAL", "label", "Goal"),
        Map.of("code", "NEED", "label", "Need"),
        Map.of("code", "REQUIREMENT", "label", "Requirement")
    );

    private static final List<Map<String, String>> REQUIREMENT_PROPERTIES = List.of(
        Map.of("code", "FUNCTIONAL", "label", "Functional"),
        Map.of("code", "QUALITY", "label", "Quality")
    );

    private static final List<Map<String, String>> SYNTAX_ROLES = List.of(
        Map.of("code", "SUBJECT", "label", "Subject"),
        Map.of("code", "ACTION", "label", "Action"),
        Map.of("code", "OBJECT", "label", "Object"),
        Map.of("code", "CONDITION", "label", "Condition"),
        Map.of("code", "CONSTRAINT", "label", "Constraint")
    );

    private static final List<Map<String, String>> REQUIREMENT_RELATION_TYPES = List.of(
        Map.of("code", "REFINES", "label", "Refines"),
        Map.of("code", "SATISFIES", "label", "Satisfies"),
        Map.of("code", "CONFLICTS_WITH", "label", "Conflicts with"),
        Map.of("code", "DEPENDS_ON", "label", "Depends on")
    );

    private static final List<Map<String, String>> ALLOWED_REQUIREMENT_RELATIONS = List.of(
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

    private static final List<Map<String, String>> SAMPLE_REQUIREMENTS = List.of(
        Map.of(
            "id",
            SAMPLE_GOAL_ID,
            "rawText",
            "Reduce failed customer onboarding by 30%.",
            "type",
            "GOAL",
            "property",
            "QUALITY",
            "provenanceId",
            "sample-provenance-goal-1"
        ),
        Map.of(
            "id",
            SAMPLE_NEED_ID,
            "rawText",
            "Support agents need visibility into failed payment attempts.",
            "type",
            "NEED",
            "property",
            "FUNCTIONAL",
            "provenanceId",
            "sample-provenance-need-1"
        ),
        Map.of(
            "id",
            SAMPLE_REQUIREMENT_ID,
            "rawText",
            "The billing service must log failed payment attempts with reason codes.",
            "type",
            "REQUIREMENT",
            "property",
            "FUNCTIONAL",
            "provenanceId",
            "sample-provenance-requirement-1"
        )
    );

    private static final List<Map<String, String>> SAMPLE_SYNTAX_ELEMENTS = List.of(
        Map.of(
            "id",
            "sample-syntax-requirement-1-subject",
            "requirementId",
            SAMPLE_REQUIREMENT_ID,
            "role",
            "SUBJECT",
            "text",
            "billing service"
        ),
        Map.of(
            "id",
            "sample-syntax-requirement-1-action",
            "requirementId",
            SAMPLE_REQUIREMENT_ID,
            "role",
            "ACTION",
            "text",
            "log"
        ),
        Map.of(
            "id",
            "sample-syntax-requirement-1-object",
            "requirementId",
            SAMPLE_REQUIREMENT_ID,
            "role",
            "OBJECT",
            "text",
            "failed payment attempts"
        ),
        Map.of(
            "id",
            "sample-syntax-requirement-1-constraint",
            "requirementId",
            SAMPLE_REQUIREMENT_ID,
            "role",
            "CONSTRAINT",
            "text",
            "with reason codes"
        )
    );

    private final Driver driver;

    @Inject
    Neo4jSchemaInitializer(Driver driver) {
        this.driver = driver;
    }

    void onStart(@Observes StartupEvent event) {
        try (Session session = driver.session()) {
            createSchema(session);
            seedOntology(session);
            seedSampleData(session);
        }
    }

    private void createSchema(Session session) {
        for (String statement : SCHEMA_STATEMENTS) {
            session.run(statement).consume();
        }
    }

    private void seedOntology(Session session) {
        session.executeWrite(tx -> {
            tx.run(
                """
                UNWIND $types AS type
                MERGE (t:RequirementType {code: type.code})
                SET t.label = type.label
                """,
                parameters("types", REQUIREMENT_TYPES)
            );

            tx.run(
                """
                UNWIND $properties AS property
                MERGE (p:RequirementProperty {code: property.code})
                SET p.label = property.label
                """,
                parameters("properties", REQUIREMENT_PROPERTIES)
            );

            tx.run(
                """
                UNWIND $roles AS role
                MERGE (r:SyntaxRole {code: role.code})
                SET r.label = role.label
                """,
                parameters("roles", SYNTAX_ROLES)
            );

            tx.run(
                """
                UNWIND $relationTypes AS relationType
                MERGE (t:RequirementRelationType {code: relationType.code})
                SET t.label = relationType.label
                """,
                parameters("relationTypes", REQUIREMENT_RELATION_TYPES)
            );

            tx.run(
                """
                UNWIND $allowedRelations AS relation
                MERGE (:AllowedRequirementRelation {
                    sourceTypeCode: relation.sourceTypeCode,
                    relationTypeCode: relation.relationTypeCode,
                    targetTypeCode: relation.targetTypeCode
                })
                """,
                parameters("allowedRelations", ALLOWED_REQUIREMENT_RELATIONS)
            );

            return null;
        });
    }

    private void seedSampleData(Session session) {
        session.executeWrite(tx -> {
            tx.run(
                """
                UNWIND $requirements AS requirement
                MERGE (r:Requirement {id: requirement.id})
                SET r.rawText = requirement.rawText,
                    r.type = requirement.type,
                    r.property = requirement.property,
                    r.sample = true
                MERGE (p:Provenance {id: requirement.provenanceId})
                SET p.source = 'sample',
                    p.rawText = requirement.rawText
                MERGE (r)-[:HAS_PROVENANCE]->(p)
                """,
                parameters("requirements", SAMPLE_REQUIREMENTS)
            );

            tx.run(
                """
                UNWIND $syntaxElements AS element
                MATCH (r:Requirement {id: element.requirementId})
                MERGE (e:SyntaxElement {id: element.id})
                SET e.role = element.role,
                    e.text = element.text,
                    e.sample = true
                MERGE (r)-[:HAS_SYNTAX_ELEMENT]->(e)
                """,
                parameters("syntaxElements", SAMPLE_SYNTAX_ELEMENTS)
            );

            tx.run(
                """
                MATCH (need:Requirement {id: $needId})
                MATCH (goal:Requirement {id: $goalId})
                MERGE (need)-[:SATISFIES]->(goal)
                """,
                Map.of("needId", SAMPLE_NEED_ID, "goalId", SAMPLE_GOAL_ID)
            );

            tx.run(
                """
                MATCH (requirement:Requirement {id: $requirementId})
                MATCH (need:Requirement {id: $needId})
                MERGE (requirement)-[:REFINES]->(need)
                """,
                Map.of(
                    "requirementId",
                    SAMPLE_REQUIREMENT_ID,
                    "needId",
                    SAMPLE_NEED_ID
                )
            );

            return null;
        });
    }

    private Map<String, Object> parameters(String key, Object value) {
        return Map.of(key, value);
    }
}
