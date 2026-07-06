package io.fekav.req.graphchange.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeType;

class AssertionIdentityTest {

    @Test
    void returnsAssertionKeyFromResolvedSubjectPredicateAndObject() {
        // Arrange
        GraphNodeReference subject =
            new GraphNodeReference(NodeType.CONCEPT, "login form", "Login Form");
        GraphNodeReference predicate =
            new GraphNodeReference(NodeType.PREDICATE, "validate", "Validate");
        GraphNodeReference object =
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "Credentials");

        // Act
        AssertionIdentity identity = new AssertionIdentity(subject, predicate, object);

        // Assert
        assertThat(identity.assertionKey()).isEqualTo("login form|validate|credentials");
    }

    @Test
    void rejectsNonConceptSubject_whenAssertionIdentityIsCreated() {
        // Arrange
        GraphNodeReference subject =
            new GraphNodeReference(NodeType.PREDICATE, "validate", "Validate");
        GraphNodeReference predicate =
            new GraphNodeReference(NodeType.PREDICATE, "validate", "Validate");
        GraphNodeReference object =
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "Credentials");

        // Act / Assert
        assertThatThrownBy(() -> new AssertionIdentity(subject, predicate, object))
            .isInstanceOf(InvalidGraphChangeException.class)
            .hasMessageContaining("subject must reference a CONCEPT node");
    }

    @Test
    void rejectsNonPredicatePredicate_whenAssertionIdentityIsCreated() {
        // Arrange
        GraphNodeReference subject =
            new GraphNodeReference(NodeType.CONCEPT, "login form", "Login Form");
        GraphNodeReference predicate =
            new GraphNodeReference(NodeType.CONCEPT, "validate", "Validate");
        GraphNodeReference object =
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "Credentials");

        // Act / Assert
        assertThatThrownBy(() -> new AssertionIdentity(subject, predicate, object))
            .isInstanceOf(InvalidGraphChangeException.class)
            .hasMessageContaining("predicate must reference a PREDICATE node");
    }

    @Test
    void rejectsNonConceptObject_whenAssertionIdentityIsCreated() {
        // Arrange
        GraphNodeReference subject =
            new GraphNodeReference(NodeType.CONCEPT, "login form", "Login Form");
        GraphNodeReference predicate =
            new GraphNodeReference(NodeType.PREDICATE, "validate", "Validate");
        GraphNodeReference object =
            new GraphNodeReference(NodeType.PREDICATE, "credentials", "Credentials");

        // Act / Assert
        assertThatThrownBy(() -> new AssertionIdentity(subject, predicate, object))
            .isInstanceOf(InvalidGraphChangeException.class)
            .hasMessageContaining("object must reference a CONCEPT node");
    }
}
