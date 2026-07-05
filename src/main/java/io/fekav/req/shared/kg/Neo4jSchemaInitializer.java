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

    private static final String SAMPLE_GOAL_ID = "sample-goal-payment-support";
    private static final String SAMPLE_SUPPORT_NEED_ID =
        "sample-need-support-visibility";
    private static final String SAMPLE_AUDIT_NEED_ID = "sample-need-audit-evidence";
    private static final String SAMPLE_RECORD_FAILURE_REQUIREMENT_ID =
        "sample-requirement-record-failure";
    private static final String SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID =
        "sample-requirement-normalize-codes";
    private static final String SAMPLE_DASHBOARD_REQUIREMENT_ID =
        "sample-requirement-support-dashboard";
    private static final String SAMPLE_NOTIFICATION_REQUIREMENT_ID =
        "sample-requirement-support-alert";
    private static final String SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID =
        "sample-requirement-audit-retention";
    private static final String SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID =
        "sample-requirement-audit-export";
    private static final String SAMPLE_AUDIT_RECORD_REQUIREMENT_ID =
        "sample-requirement-audit-record";
    private static final String SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID =
        "sample-requirement-retry-suppression";
    private static final String SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID =
        "sample-requirement-dashboard-masking";

    private static final String RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY =
        assertionKey("billing service", "record", "failed payment attempts");

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
            SAMPLE_GOAL_ID,
            "Reduce payment-support escalations caused by failed payments by 25% in Q3.",
            "GOAL",
            "QUALITY",
            "sample-provenance-goal-payment-support"
        ),
        sampleRequirement(
            SAMPLE_SUPPORT_NEED_ID,
            "Support agents need near-real-time visibility into failed payment attempts and decline reasons.",
            "NEED",
            "FUNCTIONAL",
            "sample-provenance-need-support-visibility"
        ),
        sampleRequirement(
            SAMPLE_AUDIT_NEED_ID,
            "Finance auditors need complete evidence for failed payment attempts that affect customer invoices.",
            "NEED",
            "QUALITY",
            "sample-provenance-need-audit-evidence"
        ),
        sampleRequirement(
            SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
            "The billing service must record failed payment attempts with provider code, decline reason, payment method, and customer account.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-record-failure"
        ),
        sampleRequirement(
            SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
            "The payment adapter must normalize provider decline codes into standard reason categories before failed payment attempts are recorded.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-normalize-codes"
        ),
        sampleRequirement(
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "The support dashboard must display failed payment attempts for a customer account within five seconds.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-support-dashboard"
        ),
        sampleRequirement(
            SAMPLE_NOTIFICATION_REQUIREMENT_ID,
            "The notification service must alert support agents when three failed payment attempts occur for the same account within ten minutes.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-support-alert"
        ),
        sampleRequirement(
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "The billing audit store must retain failed payment attempt records for 90 days in encrypted storage.",
            "REQUIREMENT",
            "QUALITY",
            "sample-provenance-requirement-audit-retention"
        ),
        sampleRequirement(
            SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
            "The audit exporter must produce a daily exception report for failed payment attempts with missing reason categories.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-audit-export"
        ),
        sampleRequirement(
            SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
            "The billing service must record failed payment attempts for audit review.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-audit-record"
        ),
        sampleRequirement(
            SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
            "The retry scheduler must suppress automatic payment retries when the decline category is hard decline.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-retry-suppression"
        ),
        sampleRequirement(
            SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
            "The support dashboard must mask payment instrument identifiers unless the agent has billing-admin permission.",
            "REQUIREMENT",
            "QUALITY",
            "sample-provenance-requirement-dashboard-masking"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTIONS = List.of(
        assertion(
            SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
            "billing service",
            "record",
            "failed payment attempts"
        ),
        assertion(
            SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
            "payment adapter",
            "normalize",
            "provider decline codes"
        ),
        assertion(
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "support dashboard",
            "display",
            "failed payment attempts"
        ),
        assertion(
            SAMPLE_NOTIFICATION_REQUIREMENT_ID,
            "notification service",
            "alert",
            "support agents"
        ),
        assertion(
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "billing audit store",
            "retain",
            "failed payment attempt records"
        ),
        assertion(
            SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
            "audit exporter",
            "produce",
            "exception report"
        ),
        assertion(
            SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
            "billing service",
            "record",
            "failed payment attempts"
        ),
        assertion(
            SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
            "retry scheduler",
            "suppress",
            "automatic payment retries"
        ),
        assertion(
            SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
            "support dashboard",
            "mask",
            "payment instrument identifiers"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTION_QUALIFIERS =
        List.of(
            assertionQualifier(
                RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY,
                "CONSTRAINT",
                "with provider code, decline reason, payment method, and customer account"
            ),
            assertionQualifier(
                assertionKey("payment adapter", "normalize", "provider decline codes"),
                "CONDITION",
                "before failed payment attempts are recorded"
            ),
            assertionQualifier(
                assertionKey("payment adapter", "normalize", "provider decline codes"),
                "CONSTRAINT",
                "into standard reason categories"
            ),
            assertionQualifier(
                assertionKey("support dashboard", "display", "failed payment attempts"),
                "CONSTRAINT",
                "for a customer account within five seconds"
            ),
            assertionQualifier(
                assertionKey("notification service", "alert", "support agents"),
                "CONDITION",
                "when three failed payment attempts occur for the same account within ten minutes"
            ),
            assertionQualifier(
                assertionKey(
                    "billing audit store",
                    "retain",
                    "failed payment attempt records"
                ),
                "CONSTRAINT",
                "for 90 days in encrypted storage"
            ),
            assertionQualifier(
                assertionKey("audit exporter", "produce", "exception report"),
                "CONSTRAINT",
                "daily"
            ),
            assertionQualifier(
                assertionKey("audit exporter", "produce", "exception report"),
                "CONSTRAINT",
                "for failed payment attempts with missing reason categories"
            ),
            assertionQualifier(
                RECORD_FAILED_PAYMENT_ATTEMPTS_ASSERTION_KEY,
                "CONSTRAINT",
                "for audit review"
            ),
            assertionQualifier(
                assertionKey("retry scheduler", "suppress", "automatic payment retries"),
                "CONDITION",
                "when the decline category is hard decline"
            ),
            assertionQualifier(
                assertionKey(
                    "support dashboard",
                    "mask",
                    "payment instrument identifiers"
                ),
                "CONDITION",
                "unless the agent has billing-admin permission"
            )
        );

    private static final List<Map<String, String>> SAMPLE_CONCEPT_MENTIONS = List.of(
        conceptMention(
            "sample-mention-record-subject",
            SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
            "SUBJECT",
            "billing service",
            "billing service"
        ),
        conceptMention(
            "sample-mention-record-object",
            SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
            "OBJECT",
            "failed payment attempts",
            "failed payment attempts"
        ),
        conceptMention(
            "sample-mention-normalize-subject",
            SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
            "SUBJECT",
            "payment adapter",
            "payment adapter"
        ),
        conceptMention(
            "sample-mention-normalize-object",
            SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
            "OBJECT",
            "provider decline codes",
            "provider decline codes"
        ),
        conceptMention(
            "sample-mention-dashboard-subject",
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "SUBJECT",
            "support dashboard",
            "support dashboard"
        ),
        conceptMention(
            "sample-mention-dashboard-object",
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "OBJECT",
            "failed payment attempts",
            "failed payment attempts"
        ),
        conceptMention(
            "sample-mention-alert-subject",
            SAMPLE_NOTIFICATION_REQUIREMENT_ID,
            "SUBJECT",
            "notification service",
            "notification service"
        ),
        conceptMention(
            "sample-mention-alert-object",
            SAMPLE_NOTIFICATION_REQUIREMENT_ID,
            "OBJECT",
            "support agents",
            "support agents"
        ),
        conceptMention(
            "sample-mention-retention-subject",
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "SUBJECT",
            "billing audit store",
            "billing audit store"
        ),
        conceptMention(
            "sample-mention-retention-object",
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "OBJECT",
            "failed payment attempt records",
            "failed payment attempt records"
        ),
        conceptMention(
            "sample-mention-export-subject",
            SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
            "SUBJECT",
            "audit exporter",
            "audit exporter"
        ),
        conceptMention(
            "sample-mention-export-object",
            SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
            "OBJECT",
            "exception report",
            "exception report"
        ),
        conceptMention(
            "sample-mention-audit-record-subject",
            SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
            "SUBJECT",
            "billing service",
            "billing service"
        ),
        conceptMention(
            "sample-mention-audit-record-object",
            SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
            "OBJECT",
            "failed payment attempts",
            "failed payment attempts"
        ),
        conceptMention(
            "sample-mention-retry-subject",
            SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
            "SUBJECT",
            "retry scheduler",
            "retry scheduler"
        ),
        conceptMention(
            "sample-mention-retry-object",
            SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
            "OBJECT",
            "automatic payment retries",
            "automatic payment retries"
        ),
        conceptMention(
            "sample-mention-masking-subject",
            SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
            "SUBJECT",
            "support dashboard",
            "support dashboard"
        ),
        conceptMention(
            "sample-mention-masking-object",
            SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
            "OBJECT",
            "payment instrument identifiers",
            "payment instrument identifiers"
        )
    );

    private static final List<Map<String, String>> SAMPLE_PREDICATE_MENTIONS = List.of(
        predicateMention(
            "sample-mention-record-predicate",
            SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
            "record"
        ),
        predicateMention(
            "sample-mention-normalize-predicate",
            SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
            "normalize"
        ),
        predicateMention(
            "sample-mention-dashboard-predicate",
            SAMPLE_DASHBOARD_REQUIREMENT_ID,
            "display"
        ),
        predicateMention(
            "sample-mention-alert-predicate",
            SAMPLE_NOTIFICATION_REQUIREMENT_ID,
            "alert"
        ),
        predicateMention(
            "sample-mention-retention-predicate",
            SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
            "retain"
        ),
        predicateMention(
            "sample-mention-export-predicate",
            SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
            "produce"
        ),
        predicateMention(
            "sample-mention-audit-record-predicate",
            SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
            "record"
        ),
        predicateMention(
            "sample-mention-retry-predicate",
            SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
            "suppress"
        ),
        predicateMention(
            "sample-mention-masking-predicate",
            SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
            "mask"
        )
    );

    private static final List<Map<String, String>> SAMPLE_QUALIFIER_MENTIONS =
        List.of(
            qualifierMention(
                "sample-mention-record-constraint",
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
                "CONSTRAINT",
                "with provider code, decline reason, payment method, and customer account",
                "with provider code, decline reason, payment method, and customer account"
            ),
            qualifierMention(
                "sample-mention-normalize-condition",
                SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
                "CONDITION",
                "before failed payment attempts are recorded",
                "before failed payment attempts are recorded"
            ),
            qualifierMention(
                "sample-mention-normalize-constraint",
                SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
                "CONSTRAINT",
                "into standard reason categories",
                "into standard reason categories"
            ),
            qualifierMention(
                "sample-mention-dashboard-constraint",
                SAMPLE_DASHBOARD_REQUIREMENT_ID,
                "CONSTRAINT",
                "for a customer account within five seconds",
                "for a customer account within five seconds"
            ),
            qualifierMention(
                "sample-mention-alert-condition",
                SAMPLE_NOTIFICATION_REQUIREMENT_ID,
                "CONDITION",
                "when three failed payment attempts occur for the same account within ten minutes",
                "when three failed payment attempts occur for the same account within ten minutes"
            ),
            qualifierMention(
                "sample-mention-retention-constraint",
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
                "CONSTRAINT",
                "for 90 days in encrypted storage",
                "for 90 days in encrypted storage"
            ),
            qualifierMention(
                "sample-mention-export-daily-constraint",
                SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
                "CONSTRAINT",
                "daily",
                "daily"
            ),
            qualifierMention(
                "sample-mention-export-scope-constraint",
                SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
                "CONSTRAINT",
                "for failed payment attempts with missing reason categories",
                "for failed payment attempts with missing reason categories"
            ),
            qualifierMention(
                "sample-mention-audit-record-constraint",
                SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
                "CONSTRAINT",
                "for audit review",
                "for audit review"
            ),
            qualifierMention(
                "sample-mention-retry-condition",
                SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
                "CONDITION",
                "when the decline category is hard decline",
                "when the decline category is hard decline"
            ),
            qualifierMention(
                "sample-mention-masking-condition",
                SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
                "CONDITION",
                "unless the agent has billing-admin permission",
                "unless the agent has billing-admin permission"
            )
        );

    private static final List<Map<String, String>> SAMPLE_SATISFIES_RELATIONS =
        List.of(
            requirementRelation(SAMPLE_SUPPORT_NEED_ID, SAMPLE_GOAL_ID),
            requirementRelation(SAMPLE_AUDIT_NEED_ID, SAMPLE_GOAL_ID)
        );

    private static final List<Map<String, String>> SAMPLE_REFINES_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
                SAMPLE_SUPPORT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
                SAMPLE_AUDIT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID,
                SAMPLE_SUPPORT_NEED_ID
            ),
            requirementRelation(SAMPLE_DASHBOARD_REQUIREMENT_ID, SAMPLE_SUPPORT_NEED_ID),
            requirementRelation(
                SAMPLE_NOTIFICATION_REQUIREMENT_ID,
                SAMPLE_SUPPORT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
                SAMPLE_SUPPORT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
                SAMPLE_SUPPORT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
                SAMPLE_AUDIT_NEED_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
                SAMPLE_AUDIT_NEED_ID
            ),
            requirementRelation(SAMPLE_AUDIT_RECORD_REQUIREMENT_ID, SAMPLE_AUDIT_NEED_ID)
        );

    private static final List<Map<String, String>> SAMPLE_DEPENDS_ON_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID,
                SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_DASHBOARD_REQUIREMENT_ID,
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_NOTIFICATION_REQUIREMENT_ID,
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID,
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
                SAMPLE_AUDIT_RETENTION_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_EXPORT_REQUIREMENT_ID,
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_AUDIT_RECORD_REQUIREMENT_ID,
                SAMPLE_RECORD_FAILURE_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_RETRY_SUPPRESSION_REQUIREMENT_ID,
                SAMPLE_NORMALIZE_CODES_REQUIREMENT_ID
            ),
            requirementRelation(
                SAMPLE_DASHBOARD_MASKING_REQUIREMENT_ID,
                SAMPLE_DASHBOARD_REQUIREMENT_ID
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
