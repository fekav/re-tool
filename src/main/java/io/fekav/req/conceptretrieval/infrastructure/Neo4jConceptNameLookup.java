package io.fekav.req.conceptretrieval.infrastructure;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateLookup;
import io.fekav.req.shared.model.SelectedTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jConceptNameLookup implements CandidateLookup {

    private static final String QUERY = """
        MATCH (candidate)
        WHERE (
            candidate.label = $text
            OR candidate.text = $text
            OR candidate.alias = $text
            OR $text IN coalesce(candidate.aliases, [])
        )
          AND (
              $requirementElement IS NULL
              OR candidate.requirementElement IS NULL
              OR candidate.requirementElement = $requirementElement
          )
        WITH candidate,
             toString(coalesce(candidate.label, candidate.text)) AS candidateLabel,
             toString(coalesce(candidate.id, candidate.code, candidate.label, candidate.text)) AS candidateKey,
             coalesce(candidate.type, head(labels(candidate))) AS conceptType
        RETURN candidateKey, candidateLabel, conceptType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private final Driver driver;

    @Inject
    public Neo4jConceptNameLookup(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<CandidateConcept> findCandidates(SelectedTerm selectedTerm) {
        try (Session session = driver.session()) {
            return session.executeRead(transaction -> {
                var result = transaction.run(
                    QUERY,
                    Map.of(
                        "text",
                        selectedTerm.text(),
                        "requirementElement",
                        selectedTerm.requirementElement().name()
                    )
                );
                return result
                    .stream()
                    .map(this::candidateConcept)
                    .toList();
            });
        }
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
