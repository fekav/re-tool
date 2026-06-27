package io.fekav.req.syntaxextraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.shared.model.ElementId;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class ExtractSyntaxResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesCompatibleSyntaxElementsShape() throws Exception {
        ExtractSyntaxResponse response = ExtractSyntaxResponse.from(action(
            Set.of(new Condition("on request")),
            Set.of(new Constraint("as CSV"))
        ));

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(json.path("syntaxElements").path("SUBJECT").asText())
            .isEqualTo("reporting dashboard");
        assertThat(json.path("syntaxElements").path("ACTION").asText())
            .isEqualTo("shall export");
        assertThat(json.path("syntaxElements").path("OBJECT").asText())
            .isEqualTo("monthly usage metrics");
        assertThat(json.path("syntaxElements").path("CONDITION").asText())
            .isEqualTo("on request");
        assertThat(json.path("syntaxElements").path("CONSTRAINT").asText())
            .isEqualTo("as CSV");
    }

    @Test
    void serializesMissingOptionalValuesAsEmptyStrings() {
        ExtractSyntaxResponse response = ExtractSyntaxResponse.from(action(Set.of(), Set.of()));

        assertThat(response.syntaxElements().CONDITION()).isEqualTo("");
        assertThat(response.syntaxElements().CONSTRAINT()).isEqualTo("");
    }

    @Test
    void rejectsMultipleConditionsForSingleFieldResponse() {
        assertThatThrownBy(() -> ExtractSyntaxResponse.from(action(
            Set.of(new Condition("on request"), new Condition("after approval")),
            Set.of()
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("syntaxElements.CONDITION cannot represent multiple values");
    }

    @Test
    void rejectsMultipleConstraintsForSingleFieldResponse() {
        assertThatThrownBy(() -> ExtractSyntaxResponse.from(action(
            Set.of(),
            Set.of(new Constraint("as CSV"), new Constraint("within 24 hours"))
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("syntaxElements.CONSTRAINT cannot represent multiple values");
    }

    private Action action(Set<Condition> conditions, Set<Constraint> constraints) {
        return new Action(
            ElementId.create(),
            "shall export",
            new Subject(ElementId.create(), "reporting dashboard"),
            new TargetObject(ElementId.create(), "monthly usage metrics"),
            conditions,
            constraints
        );
    }
}
