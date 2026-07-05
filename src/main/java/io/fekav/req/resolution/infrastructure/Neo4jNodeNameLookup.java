package io.fekav.req.resolution.infrastructure;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.resolution.domain.CandidateLookup;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jNodeNameLookup implements CandidateLookup {

    private static final String CONCEPT_QUERY = """
        MATCH (candidate:Concept)
        WHERE candidate.canonicalName = $text
        WITH candidate,
             toString(candidate.canonicalName) AS candidateLabel,
             toString(candidate.canonicalName) AS candidateKey
        RETURN candidateKey, candidateLabel, 'CONCEPT' AS nodeType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private static final String PREDICATE_QUERY = """
        MATCH (candidate:Predicate)
        WHERE candidate.canonicalName = $text
        WITH candidate,
             toString(candidate.canonicalName) AS candidateLabel,
             toString(candidate.canonicalName) AS candidateKey
        RETURN candidateKey, candidateLabel, 'PREDICATE' AS nodeType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private static final String QUALIFIER_QUERY = """
        MATCH (candidate:Qualifier)
        WHERE candidate.canonicalText = $text
          AND candidate.qualifierKind = $qualifierKind
        WITH candidate,
             toString(candidate.canonicalText) AS candidateLabel,
             toString(candidate.qualifierKind + '::' + candidate.canonicalText) AS candidateKey
        RETURN candidateKey, candidateLabel, 'QUALIFIER' AS nodeType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private final Driver driver;

    @Inject
    public Neo4jNodeNameLookup(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<CandidateNode> findCandidates(RequirementElement requirementElement) {
        try (Session session = driver.session()) {
            return session.executeRead(transaction -> {
                var result = transaction.run(
                    queryFor(requirementElement),
                    parametersFor(requirementElement)
                );
                return result
                    .stream()
                    .map(this::candidateNode)
                    .toList();
            });
        }
    }

    private String queryFor(RequirementElement requirementElement) {
        return switch (requirementElement.type()) {
            case SUBJECT, OBJECT -> CONCEPT_QUERY;
            case ACTION -> PREDICATE_QUERY;
            case CONDITION, CONSTRAINT -> QUALIFIER_QUERY;
        };
    }

    private Map<String, Object> parametersFor(RequirementElement requirementElement) {
        return switch (requirementElement.type()) {
            case SUBJECT, ACTION, OBJECT -> Map.of("text", requirementElement.text());
            case CONDITION, CONSTRAINT -> Map.of(
                "text",
                requirementElement.text(),
                "qualifierKind",
                requirementElement.type().name()
            );
        };
    }

    private CandidateNode candidateNode(Record record) {
        return new CandidateNode(
            record.get("candidateKey").asString(),
            record.get("candidateLabel").asString(),
            NodeType.valueOf(record.get("nodeType").asString())
        );
    }
}
