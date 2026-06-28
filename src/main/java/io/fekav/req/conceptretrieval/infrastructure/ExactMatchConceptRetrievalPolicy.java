package io.fekav.req.conceptretrieval.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fekav.req.conceptretrieval.application.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.application.RetrieveCandidateConceptsCommand;
import io.fekav.req.conceptretrieval.domain.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatch;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import io.fekav.req.conceptretrieval.domain.RetrievalEvidence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

@ApplicationScoped
public class ExactMatchConceptRetrievalPolicy implements ConceptRetrievalPolicy {

    private static final String POLICY_NAME = "exactMatch";
    private static final String FIND_CANDIDATES_BY_EXACT_TEXT = """
        CALL {
            MATCH (e:SyntaxElement)
            WHERE e.text = $label
            RETURN e.id AS candidateId,
                   e.text AS candidateLabel,
                   e.role AS candidateType
            UNION
            MATCH (r:Requirement)
            WHERE r.rawText = $label
            RETURN r.id AS candidateId,
                   r.rawText AS candidateLabel,
                   r.type AS candidateType
            UNION
            MATCH (t:RequirementType)
            WHERE t.code = $label OR t.label = $label
            RETURN t.code AS candidateId,
                   t.label AS candidateLabel,
                   'RequirementType' AS candidateType
            UNION
            MATCH (p:RequirementProperty)
            WHERE p.code = $label OR p.label = $label
            RETURN p.code AS candidateId,
                   p.label AS candidateLabel,
                   'RequirementProperty' AS candidateType
            UNION
            MATCH (r:SyntaxRole)
            WHERE r.code = $label OR r.label = $label
            RETURN r.code AS candidateId,
                   r.label AS candidateLabel,
                   'SyntaxRole' AS candidateType
            UNION
            MATCH (t:RequirementRelationType)
            WHERE t.code = $label OR t.label = $label
            RETURN t.code AS candidateId,
                   t.label AS candidateLabel,
                   'RequirementRelationType' AS candidateType
        }
        RETURN candidateId AS conceptId,
               candidateLabel AS label,
               candidateType AS conceptType
        ORDER BY label ASC, conceptId ASC
        """;

    private final Driver driver;

    @Inject
    public ExactMatchConceptRetrievalPolicy(Driver driver) {
        this.driver = driver;
    }

    @Override
    public CandidateConceptMatchSet retrieveCandidates(
        Collection<RetrieveCandidateConceptsCommand.SelectedTermInput> terms
    ) {
        Objects.requireNonNull(terms, "selected terms must not be null");

        List<CandidateConceptMatch> matches = new ArrayList<>();
        try (Session session = driver.session()) {
            for (RetrieveCandidateConceptsCommand.SelectedTermInput term : terms) {
                Objects.requireNonNull(term, "selected term must not be null");
                List<CandidateConcept> candidates = session
                    .run(
                        FIND_CANDIDATES_BY_EXACT_TEXT,
                        Map.of("label", term.text())
                    )
                    .stream()
                    .map(this::toCandidateConcept)
                    .toList();

                matches.add(new CandidateConceptMatch(
                    term.syntaxRole(),
                    term.text(),
                    candidates,
                    evidenceFor(term.text(), candidates)
                ));
            }
        }

        return new CandidateConceptMatchSet(matches);
    }

    private CandidateConcept toCandidateConcept(Record record) {
        return new CandidateConcept(
            record.get("conceptId").asString(),
            record.get("label").asString(),
            record.get("conceptType").asString(null)
        );
    }

    private List<RetrievalEvidence> evidenceFor(
        String termText,
        List<CandidateConcept> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of(new RetrievalEvidence(
                POLICY_NAME,
                "No graph candidate matched '" + termText + "'",
                null
            ));
        }

        return candidates
            .stream()
            .map(candidate -> new RetrievalEvidence(
                POLICY_NAME,
                "Matched graph candidate '" + candidate.label() + "'",
                1.0
            ))
            .toList();
    }
}
