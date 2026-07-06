package io.fekav.req.graphchange.domain;

import java.util.Objects;
import java.util.Set;

import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElementType;

public record GraphQualifier(
    RequirementElementType kind,
    GraphNodeReference node
) {

    private static final Set<RequirementElementType> QUALIFIER_KINDS =
        Set.of(RequirementElementType.CONDITION, RequirementElementType.CONSTRAINT);

    public GraphQualifier {
        Objects.requireNonNull(kind, "qualifier kind must not be null");
        Objects.requireNonNull(node, "qualifier node must not be null");
        assertValidKind(kind);
        assertValidNode(node);
    }

    public String qualifierKind() {
        return kind.name();
    }

    public String canonicalText() {
        return node.label();
    }

    private static void assertValidKind(RequirementElementType kind) {
        if (!QUALIFIER_KINDS.contains(kind)) {
            throw new InvalidGraphChangeException(
                "qualifier kind must be CONDITION or CONSTRAINT"
            );
        }
    }

    private static void assertValidNode(GraphNodeReference node) {
        if (node.nodeType() != NodeType.QUALIFIER) {
            throw new InvalidGraphChangeException(
                "qualifier must reference a QUALIFIER node"
            );
        }
    }
}
