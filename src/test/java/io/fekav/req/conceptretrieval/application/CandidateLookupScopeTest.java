package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.req.conceptretrieval.domain.SelectedTerm;

class CandidateLookupScopeTest {

    @Test
    void createsLookupScopeFromSelectedTerm() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm(" SUBJECT ", " billing service ");

        // When
        CandidateLookupScope scope = CandidateLookupScope.forSelectedTerm(selectedTerm);

        // Then
        assertThat(scope.syntaxRole()).isEqualTo("SUBJECT");
        assertThat(scope.allowedConceptTypes()).isEmpty();
    }

    @Test
    void trimsAndCopiesAllowedConceptTypes() {
        // Given
        Set<String> allowedConceptTypes = new HashSet<>();
        allowedConceptTypes.add(" SystemComponent ");

        // When
        CandidateLookupScope scope = new CandidateLookupScope(
            " SUBJECT ",
            allowedConceptTypes
        );
        allowedConceptTypes.clear();

        // Then
        assertThat(scope.syntaxRole()).isEqualTo("SUBJECT");
        assertThat(scope.allowedConceptTypes()).containsExactly("SystemComponent");
        assertThatThrownBy(() -> scope.allowedConceptTypes().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsLookupScope_whenSyntaxRoleIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateLookupScope(" ", Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate lookup scope syntax role must not be blank");
    }
}
