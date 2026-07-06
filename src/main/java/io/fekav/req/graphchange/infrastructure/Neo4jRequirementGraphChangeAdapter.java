package io.fekav.req.graphchange.infrastructure;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import io.fekav.req.graphchange.application.PersistRequirementGraphChange;
import io.fekav.req.graphchange.application.RequirementGraphChangePort;
import io.fekav.req.graphchange.application.RequirementGraphChangeResult;
import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.graphchange.domain.GraphQualifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jRequirementGraphChangeAdapter implements RequirementGraphChangePort {

    private static final String REQUIREMENT_EXISTS_QUERY = """
        MATCH (r:Requirement {id: $requirementId})
        RETURN count(r) > 0 AS exists
        """;

    private static final String ASSERTION_EXISTS_QUERY = """
        MATCH (a:Assertion {assertionKey: $assertionKey})
        RETURN count(a) > 0 AS exists
        """;

    private static final String WRITE_GRAPH_CHANGE_QUERY = """
        MERGE (r:Requirement {id: $requirementId})
        SET r.rawText = $rawText,
            r.type = $type,
            r.property = $property
        MERGE (p:Provenance {id: $provenanceId})
        SET p.source = $source,
            p.rawText = $rawText,
            p.ingestedAt = $ingestedAt
        MERGE (r)-[:HAS_PROVENANCE]->(p)
        MERGE (subject:Concept {canonicalName: $subjectKey})
        MERGE (predicate:Predicate {canonicalName: $predicateKey})
        MERGE (object:Concept {canonicalName: $objectKey})
        MERGE (a:Assertion {assertionKey: $assertionKey})
        SET a.subjectCanonicalName = $subjectKey,
            a.predicateCanonicalName = $predicateKey,
            a.objectCanonicalName = $objectKey
        MERGE (r)-[:ASSERTS]->(a)
        MERGE (a)-[:HAS_SUBJECT]->(subject)
        MERGE (a)-[:HAS_PREDICATE]->(predicate)
        MERGE (a)-[:HAS_OBJECT]->(object)
        WITH a
        UNWIND $qualifiers AS qualifier
        MERGE (q:Qualifier {
            qualifierKind: qualifier.qualifierKind,
            canonicalText: qualifier.canonicalText
        })
        MERGE (a)-[:HAS_QUALIFIER]->(q)
        """;

    private final Driver driver;

    @Inject
    public Neo4jRequirementGraphChangeAdapter(Driver driver) {
        this.driver = driver;
    }

    @Override
    public RequirementGraphChangeResult persist(
        PersistRequirementGraphChange graphChange
    ) {
        try (Session session = driver.session()) {
            if (requirementExists(session, graphChange)) {
                return RequirementGraphChangeResult.unchanged();
            }
            if (assertionExists(session, graphChange.assertionIdentity())) {
                return RequirementGraphChangeResult.known(graphChange.assertionIdentity());
            }

            session.executeWrite(transaction -> {
                transaction.run(
                    WRITE_GRAPH_CHANGE_QUERY,
                    graphChangeParameters(graphChange)
                );
                return null;
            });
            return RequirementGraphChangeResult.created();
        }
    }

    private boolean requirementExists(
        Session session,
        PersistRequirementGraphChange graphChange
    ) {
        return exists(
            session,
            REQUIREMENT_EXISTS_QUERY,
            Map.of("requirementId", graphChange.requirementId())
        );
    }

    private boolean assertionExists(
        Session session,
        AssertionIdentity assertionIdentity
    ) {
        return exists(
            session,
            ASSERTION_EXISTS_QUERY,
            Map.of("assertionKey", assertionIdentity.assertionKey())
        );
    }

    private boolean exists(
        Session session,
        String query,
        Map<String, Object> parameters
    ) {
        return session.executeRead(transaction ->
            transaction.run(query, parameters).single().get("exists").asBoolean()
        );
    }

    private Map<String, Object> graphChangeParameters(
        PersistRequirementGraphChange graphChange
    ) {
        AssertionIdentity assertionIdentity = graphChange.assertionIdentity();
        return Map.ofEntries(
            entry("requirementId", graphChange.requirementId()),
            entry("rawText", graphChange.provenance().getOriginalText().text()),
            entry("provenanceId", graphChange.provenance().getId().value().toString()),
            entry("source", graphChange.provenance().getSourceMetadata().sourceName().value()),
            entry("ingestedAt", graphChange.provenance().getIngestedAt().toString()),
            entry("type", graphChange.classification().conceptType().name()),
            entry("property", graphChange.classification().property().name()),
            entry("assertionKey", assertionIdentity.assertionKey()),
            entry("subjectKey", assertionIdentity.subject().key()),
            entry("predicateKey", assertionIdentity.predicate().key()),
            entry("objectKey", assertionIdentity.object().key()),
            entry("qualifiers", qualifierParameters(graphChange.qualifiers()))
        );
    }

    private List<Map<String, String>> qualifierParameters(
        List<GraphQualifier> qualifiers
    ) {
        return qualifiers.stream()
            .map(qualifier -> Map.of(
                "qualifierKind",
                qualifier.qualifierKind(),
                "canonicalText",
                qualifier.canonicalText()
            ))
            .toList();
    }
}
