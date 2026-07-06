package io.fekav.req.graphchange.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.graphchange.domain.GraphQualifier;
import io.fekav.req.graphchange.domain.InvalidGraphChangeException;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompletedAnalysisGraphChangeFactory {

    private static final Set<RequirementElementType> QUALIFIER_TYPES =
        Set.of(RequirementElementType.CONDITION, RequirementElementType.CONSTRAINT);

    private static final Map<RequirementElementType, NodeType> EXPECTED_NODE_TYPES =
        Map.of(
            RequirementElementType.SUBJECT,
            NodeType.CONCEPT,
            RequirementElementType.ACTION,
            NodeType.PREDICATE,
            RequirementElementType.OBJECT,
            NodeType.CONCEPT,
            RequirementElementType.CONDITION,
            NodeType.QUALIFIER,
            RequirementElementType.CONSTRAINT,
            NodeType.QUALIFIER
        );

    public PersistRequirementGraphChange from(
        RequirementAnalysisCompletedEvent event
    ) {
        Objects.requireNonNull(event, "event must not be null");

        GraphNodeReference subject =
            graphNodeReference(requiredDecision(event, RequirementElementType.SUBJECT));
        GraphNodeReference predicate =
            graphNodeReference(requiredDecision(event, RequirementElementType.ACTION));
        GraphNodeReference object =
            graphNodeReference(requiredDecision(event, RequirementElementType.OBJECT));
        AssertionIdentity assertionIdentity =
            new AssertionIdentity(subject, predicate, object);

        return new PersistRequirementGraphChange(
            event.correlationId(),
            event.correlationId().value().toString(),
            event.provenance(),
            event.classification(),
            assertionIdentity,
            qualifiers(event)
        );
    }

    private NodeMatchDecision requiredDecision(
        RequirementAnalysisCompletedEvent event,
        RequirementElementType type
    ) {
        List<NodeMatchDecision> matchingDecisions = event.nodeMatchDecisions()
            .stream()
            .filter(decision -> decision.requirementElement().type() == type)
            .toList();
        if (matchingDecisions.isEmpty()) {
            throw new InvalidGraphChangeException("missing " + type.name() + " decision");
        }
        if (matchingDecisions.size() > 1) {
            throw new InvalidGraphChangeException("multiple " + type.name() + " decisions");
        }
        return matchingDecisions.getFirst();
    }

    private List<GraphQualifier> qualifiers(RequirementAnalysisCompletedEvent event) {
        return event.nodeMatchDecisions()
            .stream()
            .filter(decision ->
                QUALIFIER_TYPES.contains(decision.requirementElement().type())
            )
            .map(decision ->
                new GraphQualifier(
                    decision.requirementElement().type(),
                    graphNodeReference(decision)
                )
            )
            .toList();
    }

    private GraphNodeReference graphNodeReference(NodeMatchDecision decision) {
        if (decision.status() == NodeMatchDecisionStatus.AUTO_MAP_EXISTING) {
            return existingGraphNodeReference(decision);
        }

        return newGraphNodeReference(decision.requirementElement());
    }

    private GraphNodeReference existingGraphNodeReference(NodeMatchDecision decision) {
        CandidateNode candidate = decision.candidates().getFirst().candidate();
        NodeType expectedNodeType = expectedNodeType(decision.requirementElement().type());
        if (candidate.nodeType() != expectedNodeType) {
            throw new InvalidGraphChangeException(
                decision.requirementElement().type().name() +
                    " decision must reference a " +
                    expectedNodeType.name() +
                    " node"
            );
        }
        return new GraphNodeReference(
            candidate.nodeType(),
            candidate.candidateKey(),
            candidate.label()
        );
    }

    private GraphNodeReference newGraphNodeReference(RequirementElement element) {
        return new GraphNodeReference(
            expectedNodeType(element.type()),
            newNodeKey(element),
            element.text()
        );
    }

    private String newNodeKey(RequirementElement element) {
        if (QUALIFIER_TYPES.contains(element.type())) {
            return element.type().name() + "::" + element.text();
        }
        return element.text();
    }

    private NodeType expectedNodeType(RequirementElementType type) {
        return EXPECTED_NODE_TYPES.get(type);
    }
}
