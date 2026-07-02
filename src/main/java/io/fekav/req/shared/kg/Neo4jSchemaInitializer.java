package io.fekav.req.shared.kg;

import io.fekav.req.shared.model.RequirementElementType;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

@ApplicationScoped
public class Neo4jSchemaInitializer {

    private static final String SAMPLE_GOAL_ID = "sample-goal-1";
    private static final String SAMPLE_NEED_ID = "sample-need-1";
    private static final String SAMPLE_REQUIREMENT_ID = "sample-requirement-1";
    private static final String SAMPLE_REASON_CATEGORY_REQUIREMENT_ID =
        "sample-requirement-2";
    private static final String SAMPLE_DASHBOARD_REQUIREMENT_ID =
        "sample-requirement-3";
    private static final String SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID =
        "sample-requirement-4";

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
        CREATE CONSTRAINT requirement_element_id IF NOT EXISTS
        FOR (e:RequirementElement) REQUIRE e.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT action_id IF NOT EXISTS
        FOR (a:Action) REQUIRE a.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT action_requirement_id IF NOT EXISTS
        FOR (a:Action) REQUIRE a.requirementId IS UNIQUE
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
        CREATE CONSTRAINT requirement_element_type_code IF NOT EXISTS
        FOR (e:RequirementElementType) REQUIRE e.code IS UNIQUE
        """,
        """
        CREATE CONSTRAINT requirement_element_type_label IF NOT EXISTS
        FOR (e:RequirementElementType) REQUIRE e.label IS UNIQUE
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
        CREATE INDEX action_text IF NOT EXISTS
        FOR (a:Action) ON (a.actionText)
        """,
        """
        CREATE INDEX requirement_element_text IF NOT EXISTS
        FOR (e:RequirementElement) ON (e.text)
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

    private static final List<Map<String, String>> REQUIREMENT_ELEMENT_TYPES =
        Arrays.stream(RequirementElementType.values())
            .map(element -> Map.of("code", element.name(), "label", element.label()))
            .toList();

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
        ),
        Map.of(
            "id",
            SAMPLE_REASON_CATEGORY_REQUIREMENT_ID,
            "rawText",
            "The payment adapter must normalize provider decline codes into standard reason categories before the billing service logs a failed payment attempt.",
            "type",
            "REQUIREMENT",
            "property",
            "FUNCTIONAL",
            "provenanceId",
            "sample-provenance-requirement-2"
        ),
        Map.of(
            "id",
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "rawText",
            "The support dashboard must display failed payment attempts with customer account, timestamp, and standard reason category within five seconds.",
            "type",
            "REQUIREMENT",
            "property",
            "FUNCTIONAL",
            "provenanceId",
            "sample-provenance-requirement-3"
        ),
        Map.of(
            "id",
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "rawText",
            "The billing audit store must retain failed payment attempt records for 90 days in encrypted storage.",
            "type",
            "REQUIREMENT",
            "property",
            "QUALITY",
            "provenanceId",
            "sample-provenance-requirement-4"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ACTIONS = List.of(
        Map.of(
            "id",
            "sample-action-1",
            "requirementId",
            SAMPLE_REQUIREMENT_ID,
            "actionText",
            "log"
        ),
        Map.of(
            "id",
            "sample-action-2",
            "requirementId",
            SAMPLE_REASON_CATEGORY_REQUIREMENT_ID,
            "actionText",
            "normalize"
        ),
        Map.of(
            "id",
            "sample-action-3",
            "requirementId",
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "actionText",
            "display"
        ),
        Map.of(
            "id",
            "sample-action-4",
            "requirementId",
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "actionText",
            "retain"
        )
    );

    private static final List<Map<String, String>> SAMPLE_REQUIREMENT_ELEMENTS = List.of(
        Map.of(
            "id",
            "sample-requirement-element-1-subject",
            "actionId",
            "sample-action-1",
            "type",
            "SUBJECT",
            "text",
            "billing service"
        ),
        Map.of(
            "id",
            "sample-requirement-element-1-object",
            "actionId",
            "sample-action-1",
            "type",
            "OBJECT",
            "text",
            "failed payment attempts"
        ),
        Map.of(
            "id",
            "sample-requirement-element-1-constraint",
            "actionId",
            "sample-action-1",
            "type",
            "CONSTRAINT",
            "text",
            "with reason codes"
        ),
        Map.of(
            "id",
            "sample-requirement-element-2-subject",
            "actionId",
            "sample-action-2",
            "type",
            "SUBJECT",
            "text",
            "payment adapter"
        ),
        Map.of(
            "id",
            "sample-requirement-element-2-object",
            "actionId",
            "sample-action-2",
            "type",
            "OBJECT",
            "text",
            "provider decline codes"
        ),
        Map.of(
            "id",
            "sample-requirement-element-2-condition",
            "actionId",
            "sample-action-2",
            "type",
            "CONDITION",
            "text",
            "before the billing service logs a failed payment attempt"
        ),
        Map.of(
            "id",
            "sample-requirement-element-2-constraint",
            "actionId",
            "sample-action-2",
            "type",
            "CONSTRAINT",
            "text",
            "into standard reason categories"
        ),
        Map.of(
            "id",
            "sample-requirement-element-3-subject",
            "actionId",
            "sample-action-3",
            "type",
            "SUBJECT",
            "text",
            "support dashboard"
        ),
        Map.of(
            "id",
            "sample-requirement-element-3-object",
            "actionId",
            "sample-action-3",
            "type",
            "OBJECT",
            "text",
            "failed payment attempts"
        ),
        Map.of(
            "id",
            "sample-requirement-element-3-constraint",
            "actionId",
            "sample-action-3",
            "type",
            "CONSTRAINT",
            "text",
            "with customer account, timestamp, and standard reason category within five seconds"
        ),
        Map.of(
            "id",
            "sample-requirement-element-4-subject",
            "actionId",
            "sample-action-4",
            "type",
            "SUBJECT",
            "text",
            "billing audit store"
        ),
        Map.of(
            "id",
            "sample-requirement-element-4-object",
            "actionId",
            "sample-action-4",
            "type",
            "OBJECT",
            "text",
            "failed payment attempt records"
        ),
        Map.of(
            "id",
            "sample-requirement-element-4-constraint",
            "actionId",
            "sample-action-4",
            "type",
            "CONSTRAINT",
            "text",
            "for 90 days in encrypted storage"
        )
    );

    private static final List<Map<String, String>> SAMPLE_REFINES_RELATIONS =
        List.of(
            Map.of(
                "sourceRequirementId",
                SAMPLE_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_NEED_ID
            ),
            Map.of(
                "sourceRequirementId",
                SAMPLE_DASHBOARD_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_NEED_ID
            ),
            Map.of(
                "sourceRequirementId",
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_NEED_ID
            )
        );

    private static final List<Map<String, String>> SAMPLE_DEPENDS_ON_RELATIONS =
        List.of(
            Map.of(
                "sourceRequirementId",
                SAMPLE_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_REASON_CATEGORY_REQUIREMENT_ID
            ),
            Map.of(
                "sourceRequirementId",
                SAMPLE_DASHBOARD_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_REQUIREMENT_ID
            ),
            Map.of(
                "sourceRequirementId",
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
                "targetRequirementId",
                SAMPLE_REQUIREMENT_ID
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
                UNWIND $requirementElementTypes AS requirementElementType
                MERGE (e:RequirementElementType {code: requirementElementType.code})
                SET e.label = requirementElementType.label
                """,
                parameters("requirementElementTypes", REQUIREMENT_ELEMENT_TYPES)
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
                UNWIND $actions AS action
                MATCH (r:Requirement {id: action.requirementId})
                MERGE (a:Action {id: action.id})
                SET a.actionText = action.actionText,
                    a.requirementId = action.requirementId,
                    a.sample = true
                MERGE (r)-[:HAS_ACTION]->(a)
                """,
                parameters("actions", SAMPLE_ACTIONS)
            );

            tx.run(
                """
                UNWIND $requirementElements AS element
                MATCH (a:Action {id: element.actionId})
                MERGE (e:RequirementElement {id: element.id})
                SET e.type = element.type,
                    e.text = element.text,
                    e.sample = true
                MERGE (a)-[:HAS_REQUIREMENT_ELEMENT]->(e)
                """,
                parameters("requirementElements", SAMPLE_REQUIREMENT_ELEMENTS)
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
                UNWIND $refinesRelations AS relation
                MATCH (requirement:Requirement {id: relation.sourceRequirementId})
                MATCH (need:Requirement {id: relation.targetRequirementId})
                MERGE (requirement)-[:REFINES]->(need)
                """,
                parameters("refinesRelations", SAMPLE_REFINES_RELATIONS)
            );

            tx.run(
                """
                UNWIND $dependsOnRelations AS relation
                MATCH (source:Requirement {id: relation.sourceRequirementId})
                MATCH (target:Requirement {id: relation.targetRequirementId})
                MERGE (source)-[:DEPENDS_ON]->(target)
                """,
                parameters("dependsOnRelations", SAMPLE_DEPENDS_ON_RELATIONS)
            );

            return null;
        });
    }

    private Map<String, Object> parameters(String key, Object value) {
        return Map.of(key, value);
    }
}
