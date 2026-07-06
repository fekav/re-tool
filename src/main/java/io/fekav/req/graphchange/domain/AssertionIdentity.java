package io.fekav.req.graphchange.domain;

import java.util.Objects;

import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeType;

public record AssertionIdentity(
    GraphNodeReference subject,
    GraphNodeReference predicate,
    GraphNodeReference object
) {

    public AssertionIdentity {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");
        Objects.requireNonNull(object, "object must not be null");
        assertValidSubject(subject);
        assertValidPredicate(predicate);
        assertValidObject(object);
    }

    public String assertionKey() {
        return subject.key() + "|" + predicate.key() + "|" + object.key();
    }

    private static void assertValidSubject(GraphNodeReference subject) {
        if (subject.nodeType() != NodeType.CONCEPT) {
            throw new InvalidGraphChangeException(
                "assertion subject must reference a CONCEPT node"
            );
        }
    }

    private static void assertValidPredicate(GraphNodeReference predicate) {
        if (predicate.nodeType() != NodeType.PREDICATE) {
            throw new InvalidGraphChangeException(
                "assertion predicate must reference a PREDICATE node"
            );
        }
    }

    private static void assertValidObject(GraphNodeReference object) {
        if (object.nodeType() != NodeType.CONCEPT) {
            throw new InvalidGraphChangeException(
                "assertion object must reference a CONCEPT node"
            );
        }
    }
}
