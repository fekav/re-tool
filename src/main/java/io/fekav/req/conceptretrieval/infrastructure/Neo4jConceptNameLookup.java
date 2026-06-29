package io.fekav.req.conceptretrieval.infrastructure;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import io.fekav.req.conceptretrieval.domain.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.ConceptNameLookup;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jConceptNameLookup implements ConceptNameLookup {

    private static final String QUERY = """
        MATCH (candidate)
        WHERE (
            candidate.label = $text
            OR candidate.text = $text
            OR candidate.alias = $text
            OR $text IN coalesce(candidate.aliases, [])
        )
          AND ($syntaxRole IS NULL OR candidate.role IS NULL OR candidate.role = $syntaxRole)
        WITH candidate,
             toString(coalesce(candidate.label, candidate.text)) AS candidateLabel,
             toString(coalesce(candidate.id, candidate.code, candidate.label, candidate.text)) AS candidateKey,
             coalesce(candidate.type, candidate.role, head(labels(candidate))) AS conceptType
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
                        "syntaxRole",
                        selectedTerm.syntaxRole()
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
