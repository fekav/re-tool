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

    private static final String SAMPLE_SOURCE = "API-Request";
    private static final String SAMPLE_FUNCTIONAL_GOAL_ID =
        "sample-goal-subscriber-incident-updates";
    private static final String SAMPLE_QUALITY_GOAL_ID =
        "sample-goal-incident-update-context";
    private static final String SAMPLE_FUNCTIONAL_NEED_ID =
        "sample-need-subscriber-outage-updates";
    private static final String SAMPLE_QUALITY_NEED_ID =
        "sample-need-incident-update-context";
    private static final String SAMPLE_SEND_UPDATES_REQUIREMENT_ID =
        "sample-requirement-send-outage-updates";
    private static final String SAMPLE_STORE_CONTEXT_REQUIREMENT_ID =
        "sample-requirement-store-incident-context";

    private static final String FUNCTIONAL_GOAL_ASSERTION_KEY = assertionKey(
        "subscribers",
        "receive",
        "outage updates"
    );
    private static final String QUALITY_GOAL_ASSERTION_KEY = assertionKey(
        "outage updates",
        "include",
        "affected services and detection time"
    );
    private static final String FUNCTIONAL_NEED_ASSERTION_KEY = assertionKey(
        "subscribers",
        "need",
        "outage updates"
    );
    private static final String QUALITY_NEED_ASSERTION_KEY = assertionKey(
        "support agents",
        "need",
        "incident updates"
    );
    private static final String SEND_UPDATES_ASSERTION_KEY = assertionKey(
        "notification service",
        "must send",
        "outage updates"
    );
    private static final String STORE_CONTEXT_ASSERTION_KEY = assertionKey(
        "monitoring service",
        "must store",
        "affected service and detection time"
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
            "Subscribers receive outage updates automatically during service incidents.",
            "GOAL",
            "FUNCTIONAL",
            "sample-provenance-goal-subscriber-incident-updates",
            "2026-07-02T10:00:00Z"
        ),
        sampleRequirement(
            SAMPLE_QUALITY_GOAL_ID,
            "Outage updates include affected services and detection time during incidents.",
            "GOAL",
            "QUALITY",
            "sample-provenance-goal-incident-update-context",
            "2026-07-02T10:01:00Z"
        ),
        sampleRequirement(
            SAMPLE_FUNCTIONAL_NEED_ID,
            "Subscribers need outage updates after the platform detects an incident.",
            "NEED",
            "FUNCTIONAL",
            "sample-provenance-need-subscriber-outage-updates",
            "2026-07-02T10:02:00Z"
        ),
        sampleRequirement(
            SAMPLE_QUALITY_NEED_ID,
            "Support agents need incident updates with affected service and detection time.",
            "NEED",
            "QUALITY",
            "sample-provenance-need-incident-update-context",
            "2026-07-02T10:03:00Z"
        ),
        sampleRequirement(
            SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
            "The notification service must send outage updates after the monitoring service stores an incident record.",
            "REQUIREMENT",
            "FUNCTIONAL",
            "sample-provenance-requirement-send-outage-updates",
            "2026-07-02T10:04:00Z"
        ),
        sampleRequirement(
            SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
            "The monitoring service must store the affected service and detection time in each incident record.",
            "REQUIREMENT",
            "QUALITY",
            "sample-provenance-requirement-store-incident-context",
            "2026-07-02T10:05:00Z"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTIONS = List.of(
        assertion(
            SAMPLE_FUNCTIONAL_GOAL_ID,
            "subscribers",
            "receive",
            "outage updates"
        ),
        assertion(
            SAMPLE_QUALITY_GOAL_ID,
            "outage updates",
            "include",
            "affected services and detection time"
        ),
        assertion(
            SAMPLE_FUNCTIONAL_NEED_ID,
            "subscribers",
            "need",
            "outage updates"
        ),
        assertion(
            SAMPLE_QUALITY_NEED_ID,
            "support agents",
            "need",
            "incident updates"
        ),
        assertion(
            SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
            "notification service",
            "must send",
            "outage updates"
        ),
        assertion(
            SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
            "monitoring service",
            "must store",
            "affected service and detection time"
        )
    );

    private static final List<Map<String, String>> SAMPLE_ASSERTION_QUALIFIERS =
        List.of(
            assertionQualifier(
                FUNCTIONAL_GOAL_ASSERTION_KEY,
                "CONSTRAINT",
                "automatically"
            ),
            assertionQualifier(
                FUNCTIONAL_GOAL_ASSERTION_KEY,
                "CONDITION",
                "during service incidents"
            ),
            assertionQualifier(
                QUALITY_GOAL_ASSERTION_KEY,
                "CONDITION",
                "during incidents"
            ),
            assertionQualifier(
                FUNCTIONAL_NEED_ASSERTION_KEY,
                "CONDITION",
                "platform detects an incident"
            ),
            assertionQualifier(
                QUALITY_NEED_ASSERTION_KEY,
                "CONSTRAINT",
                "with affected service and detection time"
            ),
            assertionQualifier(
                SEND_UPDATES_ASSERTION_KEY,
                "CONDITION",
                "monitoring service stores an incident record"
            ),
            assertionQualifier(
                STORE_CONTEXT_ASSERTION_KEY,
                "CONSTRAINT",
                "in each incident record"
            )
        );

    private static final List<Map<String, String>> SAMPLE_CONCEPT_MENTIONS = List.of(
        conceptMention(
            mentionId(SAMPLE_FUNCTIONAL_GOAL_ID, "SUBJECT", "subscribers"),
            SAMPLE_FUNCTIONAL_GOAL_ID,
            "SUBJECT",
            "subscribers",
            "subscribers"
        ),
        conceptMention(
            mentionId(SAMPLE_FUNCTIONAL_GOAL_ID, "OBJECT", "outage updates"),
            SAMPLE_FUNCTIONAL_GOAL_ID,
            "OBJECT",
            "outage updates",
            "outage updates"
        ),
        conceptMention(
            mentionId(SAMPLE_QUALITY_GOAL_ID, "SUBJECT", "outage updates"),
            SAMPLE_QUALITY_GOAL_ID,
            "SUBJECT",
            "outage updates",
            "outage updates"
        ),
        conceptMention(
            mentionId(
                SAMPLE_QUALITY_GOAL_ID,
                "OBJECT",
                "affected services and detection time"
            ),
            SAMPLE_QUALITY_GOAL_ID,
            "OBJECT",
            "affected services and detection time",
            "affected services and detection time"
        ),
        conceptMention(
            mentionId(SAMPLE_FUNCTIONAL_NEED_ID, "SUBJECT", "subscribers"),
            SAMPLE_FUNCTIONAL_NEED_ID,
            "SUBJECT",
            "subscribers",
            "subscribers"
        ),
        conceptMention(
            mentionId(SAMPLE_FUNCTIONAL_NEED_ID, "OBJECT", "outage updates"),
            SAMPLE_FUNCTIONAL_NEED_ID,
            "OBJECT",
            "outage updates",
            "outage updates"
        ),
        conceptMention(
            mentionId(SAMPLE_QUALITY_NEED_ID, "SUBJECT", "support agents"),
            SAMPLE_QUALITY_NEED_ID,
            "SUBJECT",
            "support agents",
            "support agents"
        ),
        conceptMention(
            mentionId(SAMPLE_QUALITY_NEED_ID, "OBJECT", "incident updates"),
            SAMPLE_QUALITY_NEED_ID,
            "OBJECT",
            "incident updates",
            "incident updates"
        ),
        conceptMention(
            mentionId(
                SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
                "SUBJECT",
                "notification service"
            ),
            SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
            "SUBJECT",
            "notification service",
            "notification service"
        ),
        conceptMention(
            mentionId(SAMPLE_SEND_UPDATES_REQUIREMENT_ID, "OBJECT", "outage updates"),
            SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
            "OBJECT",
            "outage updates",
            "outage updates"
        ),
        conceptMention(
            mentionId(
                SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
                "SUBJECT",
                "monitoring service"
            ),
            SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
            "SUBJECT",
            "monitoring service",
            "monitoring service"
        ),
        conceptMention(
            mentionId(
                SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
                "OBJECT",
                "affected service and detection time"
            ),
            SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
            "OBJECT",
            "affected service and detection time",
            "affected service and detection time"
        )
    );

    private static final List<Map<String, String>> SAMPLE_PREDICATE_MENTIONS = List.of(
        predicateMention(
            mentionId(SAMPLE_FUNCTIONAL_GOAL_ID, "ACTION", "receive"),
            SAMPLE_FUNCTIONAL_GOAL_ID,
            "receive"
        ),
        predicateMention(
            mentionId(SAMPLE_QUALITY_GOAL_ID, "ACTION", "include"),
            SAMPLE_QUALITY_GOAL_ID,
            "include"
        ),
        predicateMention(
            mentionId(SAMPLE_FUNCTIONAL_NEED_ID, "ACTION", "need"),
            SAMPLE_FUNCTIONAL_NEED_ID,
            "need"
        ),
        predicateMention(
            mentionId(SAMPLE_QUALITY_NEED_ID, "ACTION", "need"),
            SAMPLE_QUALITY_NEED_ID,
            "need"
        ),
        predicateMention(
            mentionId(SAMPLE_SEND_UPDATES_REQUIREMENT_ID, "ACTION", "must send"),
            SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
            "must send"
        ),
        predicateMention(
            mentionId(SAMPLE_STORE_CONTEXT_REQUIREMENT_ID, "ACTION", "must store"),
            SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
            "must store"
        )
    );

    private static final List<Map<String, String>> SAMPLE_QUALIFIER_MENTIONS =
        List.of(
            qualifierMention(
                mentionId(SAMPLE_FUNCTIONAL_GOAL_ID, "CONSTRAINT", "automatically"),
                SAMPLE_FUNCTIONAL_GOAL_ID,
                "CONSTRAINT",
                "automatically",
                "automatically"
            ),
            qualifierMention(
                mentionId(
                    SAMPLE_FUNCTIONAL_GOAL_ID,
                    "CONDITION",
                    "during service incidents"
                ),
                SAMPLE_FUNCTIONAL_GOAL_ID,
                "CONDITION",
                "during service incidents",
                "during service incidents"
            ),
            qualifierMention(
                mentionId(SAMPLE_QUALITY_GOAL_ID, "CONDITION", "during incidents"),
                SAMPLE_QUALITY_GOAL_ID,
                "CONDITION",
                "during incidents",
                "during incidents"
            ),
            qualifierMention(
                mentionId(
                    SAMPLE_FUNCTIONAL_NEED_ID,
                    "CONDITION",
                    "platform detects an incident"
                ),
                SAMPLE_FUNCTIONAL_NEED_ID,
                "CONDITION",
                "platform detects an incident",
                "platform detects an incident"
            ),
            qualifierMention(
                mentionId(
                    SAMPLE_QUALITY_NEED_ID,
                    "CONSTRAINT",
                    "with affected service and detection time"
                ),
                SAMPLE_QUALITY_NEED_ID,
                "CONSTRAINT",
                "with affected service and detection time",
                "with affected service and detection time"
            ),
            qualifierMention(
                mentionId(
                    SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
                    "CONDITION",
                    "monitoring service stores an incident record"
                ),
                SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
                "CONDITION",
                "monitoring service stores an incident record",
                "monitoring service stores an incident record"
            ),
            qualifierMention(
                mentionId(
                    SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
                    "CONSTRAINT",
                    "in each incident record"
                ),
                SAMPLE_STORE_CONTEXT_REQUIREMENT_ID,
                "CONSTRAINT",
                "in each incident record",
                "in each incident record"
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
                SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
                SAMPLE_FUNCTIONAL_NEED_ID
            ),
            requirementRelation(SAMPLE_STORE_CONTEXT_REQUIREMENT_ID, SAMPLE_QUALITY_NEED_ID)
        );

    private static final List<Map<String, String>> SAMPLE_DEPENDS_ON_RELATIONS =
        List.of(
            requirementRelation(
                SAMPLE_SEND_UPDATES_REQUIREMENT_ID,
                SAMPLE_STORE_CONTEXT_REQUIREMENT_ID
            )
        );

    private static final List<Map<String, String>> SAMPLE_CONFLICTS_WITH_RELATIONS =
        List.of();

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
                SET p.source = requirement.source,
                    p.rawText = requirement.rawText,
                    p.ingestedAt = requirement.ingestedAt
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
        String provenanceId,
        String ingestedAt
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
            provenanceId.strip(),
            "source",
            SAMPLE_SOURCE,
            "ingestedAt",
            ingestedAt.strip()
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

    private static String mentionId(String requirementId, String role, String text) {
        return requirementId.strip() + "::" + role.strip() + "::" + text.strip();
    }

    private Map<String, Object> parameters(String key, Object value) {
        return Map.of(key, value);
    }
}
