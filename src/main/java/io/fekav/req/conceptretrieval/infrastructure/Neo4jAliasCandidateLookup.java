package io.fekav.req.conceptretrieval.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import io.fekav.req.conceptretrieval.application.CandidateLookupScope;
import io.fekav.req.conceptretrieval.application.ConceptCandidateLookup;
import io.fekav.req.conceptretrieval.domain.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateLookupHit;
import io.fekav.req.conceptretrieval.domain.OrderedWeightedConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jAliasCandidateLookup implements ConceptCandidateLookup {

    private static final String QUERY = """
        MATCH (candidate)
        WHERE (candidate.alias = $text OR $text IN coalesce(candidate.aliases, []))
          AND ($syntaxRole IS NULL OR candidate.role IS NULL OR candidate.role = $syntaxRole)
          AND (
            size($allowedConceptTypes) = 0 OR
            any(conceptType IN labels(candidate) WHERE conceptType IN $allowedConceptTypes)
          )
        WITH candidate,
             toString(coalesce(candidate.label, candidate.text)) AS candidateLabel,
             toString(coalesce(candidate.id, candidate.code, candidate.label, candidate.text)) AS candidateKey,
             coalesce(candidate.type, candidate.role, head(labels(candidate))) AS conceptType
        RETURN candidateKey, candidateLabel, conceptType
        ORDER BY candidateLabel ASC, candidateKey ASC
        """;

    private final Driver driver;

    @Inject
    public Neo4jAliasCandidateLookup(Driver driver) {
        this.driver = driver;
    }

    @Override
    public String lookupMethodName() {
        return OrderedWeightedConceptRetrievalPolicy.ALIAS_LOOKUP;
    }

    @Override
    public double lookupWeight() {
        return OrderedWeightedConceptRetrievalPolicy.ALIAS_WEIGHT;
    }

    @Override
    public List<CandidateLookupHit> findCandidates(
        SelectedTerm selectedTerm,
        CandidateLookupScope scope
    ) {
        try (Session session = driver.session()) {
            return session.executeRead(transaction -> {
                var result = transaction.run(
                    QUERY,
                    Map.of(
                        "text",
                        selectedTerm.text(),
                        "syntaxRole",
                        scope.syntaxRole(),
                        "allowedConceptTypes",
                        scope.allowedConceptTypes()
                    )
                );
                List<CandidateConcept> candidates = result
                    .stream()
                    .map(this::candidateConcept)
                    .toList();
                return lookupHits(selectedTerm, candidates);
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

    private List<CandidateLookupHit> lookupHits(
        SelectedTerm selectedTerm,
        List<CandidateConcept> candidates
    ) {
        List<CandidateLookupHit> hits = new ArrayList<>();
        for (CandidateConcept candidate : candidates) {
            hits.add(new CandidateLookupHit(
                candidate,
                lookupMethodName(),
                "Matched alias '" + selectedTerm.text() + "' to graph candidate '" +
                    candidate.label() + "'"
            ));
        }
        return List.copyOf(hits);
    }

    private String nullableString(Value value) {
        return value.isNull() ? null : value.asString();
    }
}
