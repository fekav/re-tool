package io.fekav.req.conceptretrieval.infrastructure;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateLookup;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jConceptNameLookup implements CandidateLookup {

    private static final String REQUIREMENT_ELEMENT_QUERY = """
        MATCH (candidate:RequirementElement)
        WHERE candidate.text = $text
          AND candidate.type = $requirementElement
        WITH candidate,
             toString(candidate.text) AS candidateLabel,
             toString(coalesce(candidate.id, candidate.text)) AS candidateKey,
             toString(coalesce(candidate.type, head(labels(candidate)))) AS conceptType
        RETURN candidateKey, candidateLabel, conceptType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private static final String ACTION_QUERY = """
        MATCH (candidate:Action)
        WHERE candidate.actionText = $text
        WITH candidate,
             toString(candidate.actionText) AS candidateLabel,
             toString(coalesce(candidate.id, candidate.actionText)) AS candidateKey
        RETURN candidateKey, candidateLabel, 'ACTION' AS conceptType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private final Driver driver;

    @Inject
    public Neo4jConceptNameLookup(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<CandidateConcept> findCandidates(RequirementElement requirementElement) {
        try (Session session = driver.session()) {
            return session.executeRead(transaction -> {
                var result = transaction.run(
                    queryFor(requirementElement),
                    parametersFor(requirementElement)
                );
                return result
                    .stream()
                    .map(this::candidateConcept)
                    .toList();
            });
        }
    }

    private String queryFor(RequirementElement requirementElement) {
        return requirementElement.type() == RequirementElementType.ACTION
            ? ACTION_QUERY
            : REQUIREMENT_ELEMENT_QUERY;
    }

    private Map<String, Object> parametersFor(RequirementElement requirementElement) {
        if (requirementElement.type() == RequirementElementType.ACTION) {
            return Map.of("text", requirementElement.text());
        }

        return Map.of(
            "text",
            requirementElement.text(),
            "requirementElement",
            requirementElement.type().name()
        );
    }

    private CandidateConcept candidateConcept(Record record) {
        return new CandidateConcept(
            record.get("candidateKey").asString(),
            record.get("candidateLabel").asString(),
            nullableString(record.get("conceptType"))
        );
    }

    private String nullableString(Value value) {
        return value.isNull() ? null : value.asString();
    }
}
