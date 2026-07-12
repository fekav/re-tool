package io.fekav.req.search.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FindRequirementsQueryTest {

    @Test
    void stripsConceptText() {
        FindRequirementsQuery query = new FindRequirementsQuery(
            "  Zahlungsdienst  "
        );

        assertThat(query.concept()).isEqualTo("Zahlungsdienst");
    }

    @Test
    void keepsNullConceptWhenQueryDoesNotProvideOne() {
        FindRequirementsQuery query = new FindRequirementsQuery(
            null
        );

        assertThat(query.concept()).isNull();
    }
}
