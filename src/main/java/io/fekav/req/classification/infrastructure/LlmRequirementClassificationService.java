package io.fekav.req.classification.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.classification.application.ClassificationService;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.RawText;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LlmRequirementClassificationService implements ClassificationService {

    private static final String REQUIREMENT_CLASSIFICATION_FORMAT =
        "/contracts/ai/v1/requirement-classification.schema.json";

    private final LlmClientPort llmClientPort;
    private final ObjectMapper objectMapper;
    private final StructuredOutputValidator structuredOutputValidator;
    private final JsonNode requirementClassificationFormat;

    @Inject
    public LlmRequirementClassificationService(
        LlmClientPort llmClientPort,
        ObjectMapper objectMapper,
        StructuredOutputValidator structuredOutputValidator
    ) {
        this.llmClientPort = Objects.requireNonNull(llmClientPort, "llmClientPort must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.structuredOutputValidator = Objects.requireNonNull(
            structuredOutputValidator,
            "structuredOutputValidator must not be null"
        );
        this.requirementClassificationFormat = readRequirementClassificationFormat();
    }

    @Override
    public Classification classifyRequirement(RawText rawRequirementText) {
        Objects.requireNonNull(rawRequirementText, "rawRequirementText must not be null");

        String llmResponse = llmClientPort.generate(
            buildPrompt(rawRequirementText),
            requirementClassificationFormat.deepCopy()
        );
        RequirementClassificationOutput requirementClassificationOutput =
            readRequirementClassificationOutput(modelOutputFrom(llmResponse));

        structuredOutputValidator.validate(
            RequirementClassificationOutput.contract(),
            requirementClassificationOutput
        );

        return requirementClassificationOutput.toRequirementClassification();
    }

    private JsonNode readRequirementClassificationFormat() {
        try (InputStream input = LlmRequirementClassificationService.class.getResourceAsStream(
            REQUIREMENT_CLASSIFICATION_FORMAT
        )) {
            if (input == null) {
                throw new InvalidStructuredOutputException("requirement classification format contract is missing");
            }

            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new InvalidStructuredOutputException("requirement classification format contract cannot be read", e);
        }
    }

    private JsonNode modelOutputFrom(String llmResponse) {
        JsonNode responseWrapper = readJson(llmResponse, "llm response is not valid JSON");
        JsonNode response = responseWrapper.path("response");

        if (!response.isTextual() || response.asText().isBlank()) {
            throw new InvalidStructuredOutputException("llm response does not contain model output");
        }

        return readJson(response.asText(), "model output is not valid JSON");
    }

    private RequirementClassificationOutput readRequirementClassificationOutput(JsonNode modelOutput) {
        try {
            return objectMapper.treeToValue(modelOutput, RequirementClassificationOutput.class);
        } catch (JsonProcessingException e) {
            throw new InvalidStructuredOutputException(
                "model output does not match RequirementClassificationOutput",
                e
            );
        }
    }

    private JsonNode readJson(String json, String failureMessage) {
        if (json == null || json.isBlank()) {
            throw new InvalidStructuredOutputException(failureMessage);
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new InvalidStructuredOutputException(failureMessage, e);
        }
    }

    private Prompt buildPrompt(RawText rawRequirementText) {
        return PromptFactory.fromTemplate("""
        Classify the following raw requirement text.

        Choose in this order:
        1. Determine concept type from intent and commitment level.
        2. Determine property from the primary concern of the text.
        3. Assign confidence based on how explicit and unambiguous the selected concept type and property are.
        4. Provide a short rationale that names evidence for both the concept type and the property.

        Concept type records intent and commitment level.
        - GOAL: Desired outcome or business objective. Explains why change matters. Usually not directly testable as one system behavior.
        - NEED: Stakeholder need or capability gap. Explains what someone needs before it is expressed as a binding system obligation.
        - REQUIREMENT: Binding product or system obligation. Specifies what the system must do or satisfy and should be verifiable.

        Property is a separate axis from concept type.
        - FUNCTIONAL: Behavior, capability, workflow, operation, or interaction.
        - QUALITY: Quality attribute or constraint, including performance, security, availability, usability, reliability, compliance, or scalability.

        Concept-type tie-breakers:
        - Prefer GOAL when the text describes a desired business or product outcome without a concrete system obligation.
        - Prefer NEED when the text is stakeholder-centered and describes what someone needs, wants, or lacks before a binding system obligation is stated.
        - Prefer REQUIREMENT when the text assigns an obligation to the product, system, service, component, or team and can be verified.

        Property tie-breakers:
        - Prefer FUNCTIONAL when the text primarily describes behavior, capability, workflow, operation, or interaction.
        - Prefer QUALITY when the text primarily describes performance, security, availability, usability, reliability, compliance, scalability, or another quality attribute.
        - Prefer QUALITY when the quality constraint is the distinguishing obligation in text that includes both behavior and a quality constraint; otherwise prefer FUNCTIONAL.

        Keywords are evidence cues, not sufficient by themselves.
        Counterexamples:
        - "Teams need the billing service to log failed payments" can be NEED because it is stakeholder-centered, even though it mentions a service behavior.
        - "The billing service needs to log failed payments for audit" can be REQUIREMENT because it assigns a verifiable service obligation.
        - "Make onboarding secure" can be GOAL and QUALITY even without the words must, shall, performance, or security control.

        Confidence calibration:
        - 0.90 to 1.00: Explicit concept and property signals with little ambiguity.
        - 0.80 to 0.89: Likely classification with mostly clear signals but some indirect wording.
        - 0.60 to 0.79: Mixed or weak signals; rationale should name the ambiguity.
        - Below 0.60: downstream consumers should treat it as human-review material.
        Do not inflate confidence for mixed signals.

        Allowed conceptType values are exactly: GOAL, NEED, REQUIREMENT.
        Allowed property values are exactly: FUNCTIONAL, QUALITY.
        The rationale must justify both the concept type and the property.

        Respond only with valid JSON matching this shape:
        {
          "classification": {
            "conceptType": "GOAL",
            "property": "FUNCTIONAL",
            "confidenceScore": 0.0,
            "rationale": ""
          }
        }

        Requirement:
        {{requirement}}
        """)
            .systemPrompt("""
            You classify raw software requirement text into the supported requirement knowledge-graph vocabulary.
            You must respond with valid JSON only.
            Use only the supported enum values named in the prompt.
            Treat classifier signals as evidence cues, not keyword-only rules.
            Keep the rationale short and grounded in the raw text.
            """)
            .variable("requirement", rawRequirementText.text())
            .addFewShotExample(
                """
                Requirement:
                Reduce checkout abandonment.
                """,
                """
                {
                  "classification": {
                    "conceptType": "GOAL",
                    "property": "FUNCTIONAL",
                    "confidenceScore": 0.91,
                    "rationale": "The text states a desired business outcome and does not give a quality constraint."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Improve checkout response time for mobile users.
                """,
                """
                {
                  "classification": {
                    "conceptType": "GOAL",
                    "property": "QUALITY",
                    "confidenceScore": 0.9,
                    "rationale": "The text states a desired outcome focused on response time."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Customers need to complete checkout without creating an account.
                """,
                """
                {
                  "classification": {
                    "conceptType": "NEED",
                    "property": "FUNCTIONAL",
                    "confidenceScore": 0.92,
                    "rationale": "The text is stakeholder-centered and describes a capability customers need."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Customers need checkout pages to load quickly on mobile networks.
                """,
                """
                {
                  "classification": {
                    "conceptType": "NEED",
                    "property": "QUALITY",
                    "confidenceScore": 0.9,
                    "rationale": "The text is stakeholder-centered and focuses on load speed."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                The checkout service must support guest checkout.
                """,
                """
                {
                  "classification": {
                    "conceptType": "REQUIREMENT",
                    "property": "FUNCTIONAL",
                    "confidenceScore": 0.94,
                    "rationale": "The text assigns a verifiable obligation to the service."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                The checkout page must load within 2 seconds on a 4G connection.
                """,
                """
                {
                  "classification": {
                    "conceptType": "REQUIREMENT",
                    "property": "QUALITY",
                    "confidenceScore": 0.95,
                    "rationale": "The text assigns a verifiable performance constraint to the page."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Make checkout better for returning customers.
                """,
                """
                {
                  "classification": {
                    "conceptType": "GOAL",
                    "property": "FUNCTIONAL",
                    "confidenceScore": 0.58,
                    "rationale": "The wording is ambiguous and broad, so this is a forced best-fit outcome classification."
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                The reporting service must export usage metrics within 2 seconds.
                """,
                """
                {
                  "classification": {
                    "conceptType": "REQUIREMENT",
                    "property": "QUALITY",
                    "confidenceScore": 0.82,
                    "rationale": "The text mixes a behavior with a measurable performance constraint, making quality the distinguishing obligation."
                  }
                }
                """
            )
            .build();
    }
}
