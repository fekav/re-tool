package io.fekav.req.graphchange.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.graphchange.domain.InvalidGraphChangeException;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.SourceMetadata;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class CompletedAnalysisGraphChangeFactoryTest {

    private final CompletedAnalysisGraphChangeFactory factory =
        new CompletedAnalysisGraphChangeFactory();

    @Test
    void mapsExistingSubjectPredicateAndObjectNodes_whenAnalysisCompleted() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        RequirementAnalysisCompletedEvent event = event(
            correlationId,
            actionWithoutQualifiers(),
            List.of(
                autoMapDecision(
                    RequirementElementType.SUBJECT,
                    "login form",
                    "concept-login-form",
                    "Login Form",
                    NodeType.CONCEPT
                ),
                autoMapDecision(
                    RequirementElementType.ACTION,
                    "must validate",
                    "predicate-validate",
                    "Validate",
                    NodeType.PREDICATE
                ),
                autoMapDecision(
                    RequirementElementType.OBJECT,
                    "credentials",
                    "concept-credentials",
                    "Credentials",
                    NodeType.CONCEPT
                )
            )
        );

        // Act
        PersistRequirementGraphChange graphChange = factory.from(event);

        // Assert
        assertThat(graphChange.correlationId()).isEqualTo(correlationId);
        assertThat(graphChange.requirementId())
            .isEqualTo(correlationId.value().toString());
        assertThat(graphChange.provenance()).isEqualTo(event.provenance());
        assertThat(graphChange.classification()).isEqualTo(event.classification());
        assertThat(graphChange.assertionIdentity().assertionKey())
            .isEqualTo("concept-login-form|predicate-validate|concept-credentials");
        assertThat(graphChange.assertionIdentity().subject().label())
            .isEqualTo("Login Form");
        assertThat(graphChange.qualifiers()).isEmpty();
    }

    @Test
    void mapsNewSubjectPredicateAndObjectNodesFromRequirementElementText() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        RequirementAnalysisCompletedEvent event = event(
            correlationId,
            actionWithoutQualifiers(),
            List.of(
                autoCreateDecision(RequirementElementType.SUBJECT, "login form"),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials")
            )
        );

        // Act
        PersistRequirementGraphChange graphChange = factory.from(event);

        // Assert
        assertThat(graphChange.assertionIdentity().subject().key())
            .isEqualTo("login form");
        assertThat(graphChange.assertionIdentity().predicate().key())
            .isEqualTo("must validate");
        assertThat(graphChange.assertionIdentity().object().key())
            .isEqualTo("credentials");
        assertThat(graphChange.assertionIdentity().assertionKey())
            .isEqualTo("login form|must validate|credentials");
    }

    @Test
    void mapsReviewExistingDecisionsLikeAutoExistingDecisions() {
        // Arrange
        RequirementAnalysisCompletedEvent event = event(
            CorrelationId.create(),
            actionWithoutQualifiers(),
            List.of(
                reviewMapDecision(
                    RequirementElementType.SUBJECT,
                    "login form",
                    "concept-login-form",
                    "Login Form",
                    NodeType.CONCEPT
                ),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials")
            )
        );

        // Act
        PersistRequirementGraphChange graphChange = factory.from(event);

        // Assert
        assertThat(graphChange.assertionIdentity().subject().key())
            .isEqualTo("concept-login-form");
        assertThat(graphChange.assertionIdentity().subject().label())
            .isEqualTo("Login Form");
    }

    @Test
    void mapsReviewCreateDecisionsLikeAutoCreateDecisions() {
        // Arrange
        RequirementAnalysisCompletedEvent event = event(
            CorrelationId.create(),
            actionWithoutQualifiers(),
            List.of(
                reviewCreateDecision(RequirementElementType.SUBJECT, "login form"),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials")
            )
        );

        // Act
        PersistRequirementGraphChange graphChange = factory.from(event);

        // Assert
        assertThat(graphChange.assertionIdentity().subject().key())
            .isEqualTo("login form");
        assertThat(graphChange.assertionIdentity().subject().label())
            .isEqualTo("login form");
    }

    @Test
    void mapsConditionsAndConstraintsToQualifiersOutsideAssertionIdentity() {
        // Arrange
        RequirementAnalysisCompletedEvent event = event(
            CorrelationId.create(),
            actionWithQualifiers(),
            List.of(
                autoCreateDecision(RequirementElementType.SUBJECT, "login form"),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials"),
                autoCreateDecision(
                    RequirementElementType.CONDITION,
                    "before authentication"
                ),
                autoCreateDecision(
                    RequirementElementType.CONSTRAINT,
                    "within 200 milliseconds"
                )
            )
        );

        // Act
        PersistRequirementGraphChange graphChange = factory.from(event);

        // Assert
        assertThat(graphChange.assertionIdentity().assertionKey())
            .isEqualTo("login form|must validate|credentials");
        assertThat(graphChange.qualifiers())
            .extracting(
                qualifier -> qualifier.kind(),
                qualifier -> qualifier.canonicalText()
            )
            .containsExactly(
                tuple(RequirementElementType.CONDITION, "before authentication"),
                tuple(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
            );
    }

    @Test
    void failsWithNamedException_whenRequiredObjectDecisionIsMissing() {
        // Arrange
        RequirementAnalysisCompletedEvent event = event(
            CorrelationId.create(),
            actionWithoutQualifiers(),
            List.of(
                autoCreateDecision(RequirementElementType.SUBJECT, "login form"),
                autoCreateDecision(RequirementElementType.ACTION, "must validate")
            )
        );

        // Act / Assert
        assertThatThrownBy(() -> factory.from(event))
            .isInstanceOf(InvalidGraphChangeException.class)
            .hasMessageContaining("missing OBJECT decision");
    }

    @Test
    void failsWithNamedException_whenExistingCandidateHasUnexpectedNodeType() {
        // Arrange
        RequirementAnalysisCompletedEvent event = event(
            CorrelationId.create(),
            actionWithoutQualifiers(),
            List.of(
                autoMapDecision(
                    RequirementElementType.SUBJECT,
                    "login form",
                    "predicate-login-form",
                    "Login Form",
                    NodeType.PREDICATE
                ),
                autoCreateDecision(RequirementElementType.ACTION, "must validate"),
                autoCreateDecision(RequirementElementType.OBJECT, "credentials")
            )
        );

        // Act / Assert
        assertThatThrownBy(() -> factory.from(event))
            .isInstanceOf(InvalidGraphChangeException.class)
            .hasMessageContaining("SUBJECT decision must reference a CONCEPT node");
    }

    private RequirementAnalysisCompletedEvent event(
        CorrelationId correlationId,
        Action action,
        List<NodeMatchDecision> decisions
    ) {
        return RequirementAnalysisCompletedEvent.create(
            correlationId,
            provenance(),
            classification(),
            action,
            decisions
        );
    }

    private Provenance provenance() {
        return Provenance.create(
            ElementId.create(),
            new OriginalText("The login form must validate credentials."),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        );
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The sentence expresses a verifiable obligation.")
        );
    }

    private Action actionWithoutQualifiers() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(),
            Set.of()
        );
    }

    private Action actionWithQualifiers() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(new Condition("before authentication")),
            Set.of(new Constraint("within 200 milliseconds"))
        );
    }

    private NodeMatchDecision autoCreateDecision(
        RequirementElementType type,
        String text
    ) {
        return new NodeMatchDecision(
            new RequirementElement(type, text),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private NodeMatchDecision autoMapDecision(
        RequirementElementType type,
        String text,
        String candidateKey,
        String label,
        NodeType nodeType
    ) {
        return new NodeMatchDecision(
            new RequirementElement(type, text),
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(new RetrievedCandidateNode(
                new CandidateNode(candidateKey, label, nodeType),
                List.of(new RetrievalEvidence("nodeName", "matched node name", 1.0))
            )),
            "Existing candidate reached the automatic mapping threshold"
        );
    }

    private NodeMatchDecision reviewCreateDecision(
        RequirementElementType type,
        String text
    ) {
        return new NodeMatchDecision(
            new RequirementElement(type, text),
            NodeMatchDecisionStatus.REVIEW_CREATE_NEW,
            List.of(),
            "Domain reviewer requested a new concept."
        );
    }

    private NodeMatchDecision reviewMapDecision(
        RequirementElementType type,
        String text,
        String candidateKey,
        String label,
        NodeType nodeType
    ) {
        return new NodeMatchDecision(
            new RequirementElement(type, text),
            NodeMatchDecisionStatus.REVIEW_MAP_EXISTING,
            List.of(new RetrievedCandidateNode(
                new CandidateNode(candidateKey, label, nodeType),
                List.of(new RetrievalEvidence("nodeName", "matched node name", 1.0))
            )),
            "Domain reviewer selected this candidate."
        );
    }
}
