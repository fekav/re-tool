package io.fekav.req.queryrequirements.infrastructure;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import io.fekav.req.queryrequirements.application.FindRequirementsQuery;
import io.fekav.req.queryrequirements.application.RequirementsFinder;
import io.fekav.req.queryrequirements.domain.RequirementView;
import io.fekav.req.queryrequirements.domain.RequirementsResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Neo4jRequirementsReader implements RequirementsFinder {

    private static final String CONCEPT_REQUIREMENTS_QUERY = """
        MATCH (seed:Concept)
        WHERE seed.canonicalName = $conceptName
        MATCH (seed)<-[:HAS_SUBJECT|HAS_OBJECT]-(:Assertion)<-[:ASSERTS]-(requirement:Requirement)
        WHERE requirement.rawText IS NOT NULL
          AND requirement.type IS NOT NULL
          AND requirement.property IS NOT NULL
        RETURN DISTINCT toString(requirement.rawText) AS rawText,
               toString(requirement.type) AS type,
               toString(requirement.property) AS property
        """;

    private final Driver driver;

    @Inject
    public Neo4jRequirementsReader(Driver driver) {
        this.driver = driver;
    }

    @Override
    public RequirementsResult read(FindRequirementsQuery query) {
        try (Session session = driver.session()) {
            LinkedHashMap<String, RequirementView> deduplicatedRequirements =
                new LinkedHashMap<>();

            if (query.concept() != null && !query.concept().isBlank()) {
                readConceptRequirements(session, query.concept())
                    .forEach(view ->
                        deduplicatedRequirements.putIfAbsent(deduplicationKey(view), view)
                    );
            }

            return new RequirementsResult(new ArrayList<>(deduplicatedRequirements.values()));
        }
    }

    private List<RequirementView> readConceptRequirements(
        Session session,
        String concept
    ) {
        String normalizedConcept = normalizeText(concept);

        if (normalizedConcept.isBlank()) {
            return List.of();
        }

        return session.executeRead(transaction ->
            transaction
                .run(
                    CONCEPT_REQUIREMENTS_QUERY,
                    Map.of("conceptName", normalizedConcept)
                )
                .stream()
                .map(this::requirementView)
                .toList()
        );
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{Alnum}]+", " ")
            .strip();
        return normalized.replaceAll("\\s+", " ");
    }

    private RequirementView requirementView(Record record) {
        return new RequirementView(
            record.get("rawText").asString(),
            record.get("type").asString(),
            record.get("property").asString()
        );
    }

    private String deduplicationKey(RequirementView requirementView) {
        return String.join(
            "|",
            requirementView.rawText(),
            requirementView.type(),
            requirementView.property()
        );
    }
}
