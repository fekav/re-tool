package io.fekav.req.shared.kg;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

@ApplicationScoped
public class Neo4jSchemaInitializer {

    private static final String PRUNE_GRAPH_STATEMENT = """
        MATCH (n)
        DETACH DELETE n
        """;

    private static final String SAMPLE_FUNCTIONAL_GOAL_ID =
        "sample-goal-payment-recovery-flow";
    private static final String SAMPLE_QUALITY_GOAL_ID =
        "sample-goal-payment-recovery-resilience";
    private static final String SAMPLE_FUNCTIONAL_NEED_ID =
        "sample-need-automatic-retry-decision";
    private static final String SAMPLE_QUALITY_NEED_ID =
        "sample-need-payment-anomaly-evidence";
    private static final String SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID =
        "sample-requirement-automatic-retry";
    private static final String SAMPLE_QUARANTINE_REQUIREMENT_ID =
        "sample-requirement-quarantine-anomalies";

    private static final String AUTOMATIC_RETRY_ASSERTION_KEY = assertionKey(
        "payment recovery service",
        "schedule",
        "payment retries"
    );
    private static final String QUARANTINE_ANOMALIES_ASSERTION_KEY = assertionKey(
        "payment recovery service",
        "quarantine",
        "failed renewal payments"
    );

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
        CREATE CONSTRAINT mention_id IF NOT EXISTS
        FOR (m:Mention) REQUIRE m.id IS UNIQUE
        """,
        """
        CREATE CONSTRAINT concept_canonical_name IF NOT EXISTS
        FOR (c:Concept) REQUIRE c.canonicalName IS UNIQUE
        """,
        """
        CREATE CONSTRAINT predicate_canonical_name IF NOT EXISTS
        FOR (p:Predicate) REQUIRE p.canonicalName IS UNIQUE
        """,
        """
        CREATE CONSTRAINT qualifier_identity IF NOT EXISTS
        FOR (q:Qualifier) REQUIRE (q.qualifierKind, q.canonicalText) IS UNIQUE
        """,
        """
        CREATE CONSTRAINT assertion_key IF NOT EXISTS
        FOR (a:Assertion) REQUIRE a.assertionKey IS UNIQUE
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
        CREATE INDEX mention_text IF NOT EXISTS
        FOR (m:Mention) ON (m.text)
        """,
        """
        CREATE INDEX qualifier_canonical_text IF NOT EXISTS
        FOR (q:Qualifier) ON (q.canonicalText)
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
        sampleRequirement(
            SAMPLE_FUNCTIONAL_GOAL_ID,
            "Enable automatic recovery for eligible failed subscription payments.",
            "GOAL",
            "FUNCTIONAL",
            "sample-provenance-goal-payment-recovery-flow"
        ),
        sampleRequirement(
            SAMPLE_QUALITY_GOAL_ID,
            "Keep duplicate payment retries below one per 10,000 renewal attempts during peak billing windows.",
            "GOAL",
            "QUALITY",
            "sample-provenance-goal-payment-recovery-resilience"
        ),
        sampleRequirement(
            SAMPLE_FUNCTIONAL_NEED_ID,
            "Operations need eligible failed renewal payments to receive an automatic retry decision without manual triage.",
            "NEED",
            "FUNCTIONAL",
            "sample-provenance-need-automatic-retry-decision"
        ),
        sampleRequirement(
            SAMPLE_QUALITY_NEED_ID,
            "Finance auditors need anomaly evidence when a failed payment has missing provider data, conflicting decline categories, or late-arriving events.",
            "NEED",
            "QUALITY",
            "sample-provenance-need-payment-anomaly-evidence"
        ),
        sampleRequirement(
            SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
            "The payment recovery service must schedule one retry after every failed renewal payment is recorded.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-automatic-retry"
        ),
        sampleRequirement(
            SAMPLE_QUARANTINE_REQUIREMENT_ID,
            "The payment recovery service must quarantine failed renewal payments that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late.",
            "REQUIREMENT",
            "QUALITY",
            "sample-provenance-requirement-quarantine-anomalies"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTIONS = List.of(
        assertion(
            SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
            "payment recovery service",
            "schedule",
            "payment retries"
        ),
        assertion(
            SAMPLE_QUARANTINE_REQUIREMENT_ID,
            "payment recovery service",
            "quarantine",
            "failed renewal payments"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTION_QUALIFIERS =
        List.of(
            assertionQualifier(
                AUTOMATIC_RETRY_ASSERTION_KEY,
                "CONSTRAINT",
                "one"
            ),
            assertionQualifier(
                AUTOMATIC_RETRY_ASSERTION_KEY,
                "CONDITION",
                "after every failed renewal payment is recorded"
            ),
            assertionQualifier(
                QUARANTINE_ANOMALIES_ASSERTION_KEY,
                "CONDITION",
                "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late"
            ),
            assertionQualifier(
                QUARANTINE_ANOMALIES_ASSERTION_KEY,
                "CONSTRAINT",
                "until billing operations reviews the anomaly"
            )
        );

    private static final List<Map<String, String>> SAMPLE_CONCEPT_MENTIONS = List.of(
        conceptMention(
            "sample-mention-retry-subject",
            SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
            "SUBJECT",
            "payment recovery service",
            "payment recovery service"
        ),
        conceptMention(
            "sample-mention-retry-object",
            SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
            "OBJECT",
            "payment retries",
            "payment retries"
        ),
        conceptMention(
            "sample-mention-quarantine-subject",
            SAMPLE_QUARANTINE_REQUIREMENT_ID,
            "SUBJECT",
            "payment recovery service",
            "payment recovery service"
        ),
        conceptMention(
            "sample-mention-quarantine-object",
            SAMPLE_QUARANTINE_REQUIREMENT_ID,
            "OBJECT",
            "failed renewal payments",
            "failed renewal payments"
        )
    );

    private static final List<Map<String, String>> SAMPLE_PREDICATE_MENTIONS = List.of(
        predicateMention(
            "sample-mention-retry-predicate",
            SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
            "schedule"
        ),
        predicateMention(
            "sample-mention-quarantine-predicate",
            SAMPLE_QUARANTINE_REQUIREMENT_ID,
            "quarantine"
        )
    );

    private static final List<Map<String, String>> SAMPLE_QUALIFIER_MENTIONS =
        List.of(
            qualifierMention(
                "sample-mention-retry-count-constraint",
                SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
                "CONSTRAINT",
                "one",
                "one"
            ),
            qualifierMention(
                "sample-mention-retry-recorded-condition",
                SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
                "CONDITION",
                "after every failed renewal payment is recorded",
                "after every failed renewal payment is recorded"
            ),
            qualifierMention(
                "sample-mention-quarantine-anomaly-condition",
                SAMPLE_QUARANTINE_REQUIREMENT_ID,
                "CONDITION",
                "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late",
                "that lack a provider code, contain an unknown decline category, or arrive more than 24 hours late"
            ),
            qualifierMention(
                "sample-mention-quarantine-review-constraint",
                SAMPLE_QUARANTINE_REQUIREMENT_ID,
                "CONSTRAINT",
                "until billing operations reviews the anomaly",
                "until billing operations reviews the anomaly"
            )
        );

    private static final List<Map<String, String>> SAMPLE_SATISFIES_RELATIONS =
        List.of(
            requirementRelation(SAMPLE_FUNCTIONAL_NEED_ID, SAMPLE_FUNCTIONAL_GOAL_ID),
            requirementRelation(SAMPLE_QUALITY_NEED_ID, SAMPLE_QUALITY_GOAL_ID)
        );

    private static final List<Map<String, String>> SAMPLE_REFINES_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
                SAMPLE_FUNCTIONAL_NEED_ID
            ),
            requirementRelation(SAMPLE_QUARANTINE_REQUIREMENT_ID, SAMPLE_QUALITY_NEED_ID)
        );

    private static final List<Map<String, String>> SAMPLE_DEPENDS_ON_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID,
                SAMPLE_QUARANTINE_REQUIREMENT_ID
            )
        );

    private static final List<Map<String, String>> SAMPLE_CONFLICTS_WITH_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_QUARANTINE_REQUIREMENT_ID,
                SAMPLE_AUTOMATIC_RETRY_REQUIREMENT_ID
            )
        );

    private final Driver driver;
    private final boolean pruneOnStart;

    @Inject
    Neo4jSchemaInitializer(
        Driver driver,
        @ConfigProperty(name = "req.kg.neo4j.prune-on-start", defaultValue = "true")
        boolean pruneOnStart
    ) {
        this.driver = driver;
        this.pruneOnStart = pruneOnStart;
    }

    Neo4jSchemaInitializer(Driver driver) {
        this(driver, true);
    }

    void onStart(@Observes StartupEvent event) {
        try (Session session = driver.session()) {
            pruneGraph(session);
            createSchema(session);
            seedOntology(session);
            seedSampleData(session);
        }
    }

    private void pruneGraph(Session session) {
        if (pruneOnStart) {
            session.run(PRUNE_GRAPH_STATEMENT).consume();
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
                UNWIND $assertions AS assertion
                MATCH (r:Requirement {id: assertion.requirementId})
                MERGE (subject:Concept {canonicalName: assertion.subjectName})
                SET subject.sample = true
                MERGE (predicate:Predicate {canonicalName: assertion.predicateName})
                SET predicate.sample = true
                MERGE (object:Concept {canonicalName: assertion.objectName})
                SET object.sample = true
                MERGE (a:Assertion {assertionKey: assertion.assertionKey})
                SET a.subjectCanonicalName = assertion.subjectName,
                    a.predicateCanonicalName = assertion.predicateName,
                    a.objectCanonicalName = assertion.objectName,
                    a.sample = true
                MERGE (r)-[:ASSERTS]->(a)
                MERGE (a)-[:HAS_SUBJECT]->(subject)
                MERGE (a)-[:HAS_PREDICATE]->(predicate)
                MERGE (a)-[:HAS_OBJECT]->(object)
                """,
                parameters("assertions", SAMPLE_ASSERTIONS)
            );

            tx.run(
                """
                UNWIND $assertionQualifiers AS qualifier
                MATCH (a:Assertion {assertionKey: qualifier.assertionKey})
                MERGE (q:Qualifier {
                    qualifierKind: qualifier.qualifierKind,
                    canonicalText: qualifier.canonicalText
                })
                SET q.sample = true
                MERGE (a)-[:HAS_QUALIFIER]->(q)
                """,
                parameters("assertionQualifiers", SAMPLE_ASSERTION_QUALIFIERS)
            );

            tx.run(
                """
                UNWIND $conceptMentions AS mention
                MATCH (r:Requirement {id: mention.requirementId})
                MATCH (c:Concept {canonicalName: mention.canonicalName})
                MERGE (m:Mention {id: mention.id})
                SET m.text = mention.text,
                    m.role = mention.role,
                    m.sample = true
                MERGE (r)-[:HAS_MENTION]->(m)
                MERGE (m)-[:DENOTES]->(c)
                """,
                parameters("conceptMentions", SAMPLE_CONCEPT_MENTIONS)
            );

            tx.run(
                """
                UNWIND $predicateMentions AS mention
                MATCH (r:Requirement {id: mention.requirementId})
                MATCH (p:Predicate {canonicalName: mention.canonicalName})
                MERGE (m:Mention {id: mention.id})
                SET m.text = mention.text,
                    m.role = 'ACTION',
                    m.sample = true
                MERGE (r)-[:HAS_MENTION]->(m)
                MERGE (m)-[:DENOTES]->(p)
                """,
                parameters("predicateMentions", SAMPLE_PREDICATE_MENTIONS)
            );

            tx.run(
                """
                UNWIND $qualifierMentions AS mention
                MATCH (r:Requirement {id: mention.requirementId})
                MATCH (q:Qualifier {
                    qualifierKind: mention.qualifierKind,
                    canonicalText: mention.canonicalText
                })
                MERGE (m:Mention {id: mention.id})
                SET m.text = mention.text,
                    m.role = mention.qualifierKind,
                    m.sample = true
                MERGE (r)-[:HAS_MENTION]->(m)
                MERGE (m)-[:DENOTES]->(q)
                """,
                parameters("qualifierMentions", SAMPLE_QUALIFIER_MENTIONS)
            );

            tx.run(
                """
                UNWIND $satisfiesRelations AS relation
                MATCH (need:Requirement {id: relation.sourceRequirementId})
                MATCH (goal:Requirement {id: relation.targetRequirementId})
                MERGE (need)-[:SATISFIES]->(goal)
                """,
                parameters("satisfiesRelations", SAMPLE_SATISFIES_RELATIONS)
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

            tx.run(
                """
                UNWIND $conflictsWithRelations AS relation
                MATCH (source:Requirement {id: relation.sourceRequirementId})
                MATCH (target:Requirement {id: relation.targetRequirementId})
                MERGE (source)-[:CONFLICTS_WITH]->(target)
                """,
                parameters(
                    "conflictsWithRelations",
                    SAMPLE_CONFLICTS_WITH_RELATIONS
                )
            );

            return null;
        });
    }

    private static Map<String, String> assertion(
        String requirementId,
        String subjectName,
        String predicateName,
        String objectName
    ) {
        return Map.of(
            "requirementId",
            requirementId,
            "assertionKey",
            assertionKey(subjectName, predicateName, objectName),
            "subjectName",
            subjectName.strip(),
            "predicateName",
            predicateName.strip(),
            "objectName",
            objectName.strip()
        );
    }

    private static Map<String, String> sampleRequirement(
        String id,
        String rawText,
        String type,
        String property,
        String provenanceId
    ) {
        return Map.of(
            "id",
            id.strip(),
            "rawText",
            rawText.strip(),
            "type",
            type.strip(),
            "property",
            property.strip(),
            "provenanceId",
            provenanceId.strip()
        );
    }

    private static Map<String, String> requirementRelation(
        String sourceRequirementId,
        String targetRequirementId
    ) {
        return Map.of(
            "sourceRequirementId",
            sourceRequirementId.strip(),
            "targetRequirementId",
            targetRequirementId.strip()
        );
    }

    private static Map<String, String> assertionQualifier(
        String assertionKey,
        String qualifierKind,
        String canonicalText
    ) {
        return Map.of(
            "assertionKey",
            assertionKey,
            "qualifierKind",
            qualifierKind.strip(),
            "canonicalText",
            canonicalText.strip()
        );
    }

    private static Map<String, String> conceptMention(
        String id,
        String requirementId,
        String role,
        String text,
        String canonicalName
    ) {
        return Map.of(
            "id",
            id,
            "requirementId",
            requirementId,
            "role",
            role,
            "text",
            text.strip(),
            "canonicalName",
            canonicalName.strip()
        );
    }

    private static Map<String, String> predicateMention(
        String id,
        String requirementId,
        String canonicalName
    ) {
        return Map.of(
            "id",
            id,
            "requirementId",
            requirementId,
            "text",
            canonicalName.strip(),
            "canonicalName",
            canonicalName.strip()
        );
    }

    private static Map<String, String> qualifierMention(
        String id,
        String requirementId,
        String qualifierKind,
        String text,
        String canonicalText
    ) {
        return Map.of(
            "id",
            id,
            "requirementId",
            requirementId,
            "qualifierKind",
            qualifierKind.strip(),
            "text",
            text.strip(),
            "canonicalText",
            canonicalText.strip()
        );
    }

    private static String assertionKey(String subjectName, String predicateName, String objectName) {
        return subjectName.strip() + "|" + predicateName.strip() + "|" + objectName.strip();
    }

    private Map<String, Object> parameters(String key, Object value) {
        return Map.of(key, value);
    }
}
