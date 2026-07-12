package io.fekav.req.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.search.domain.RequirementView;
import io.fekav.req.search.domain.RequirementsResult;

class FindRequirementsQueryHandlerTest {

    @Test
    void rejectsQueryWithoutConcept() {
        // Given
        RequirementsFinder reader = mock(RequirementsFinder.class);
        FindRequirementsQueryHandler handler = new FindRequirementsQueryHandler(reader);

        // When / Then
        assertThatThrownBy(() ->
            handler.handle(new FindRequirementsQuery("   "))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("concept must not be blank");
        verifyNoInteractions(reader);
    }

    @Test
    void delegatesToReaderWhenQueryIsValid() {
        // Given
        RequirementsFinder reader = mock(RequirementsFinder.class);
        FindRequirementsQueryHandler handler = new FindRequirementsQueryHandler(reader);
        FindRequirementsQuery query = new FindRequirementsQuery("payment service");
        RequirementsResult expected = new RequirementsResult(List.of(
            new RequirementView(
                "The payment service must support retries.",
                "REQUIREMENT",
                "FUNCTIONAL"
            )
        ));
        when(reader.read(query)).thenReturn(expected);

        // When
        RequirementsResult result = handler.handle(query);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(reader).read(query);
    }
}
