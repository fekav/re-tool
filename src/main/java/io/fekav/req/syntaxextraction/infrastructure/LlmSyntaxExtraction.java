package io.fekav.req.syntaxextraction.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.platform.observability.Observability;
import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.application.SyntaxExtraction;
import io.fekav.req.syntaxextraction.domain.Action;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LlmSyntaxExtraction implements SyntaxExtraction {

    private static final Logger log = Logger.getLogger(LlmSyntaxExtraction.class);

    private static final String SYNTAX_FORMAT =
        "/contracts/ai/v1/requirement-syntax.schema.json";

    private final LlmClientPort llmClientPort;
    private final ObjectMapper objectMapper;
    private final StructuredOutputValidator structuredOutputValidator;
    private final JsonNode syntaxFormat;

    @ConfigProperty(name = "observability.log.llm-response", defaultValue = "true")
    boolean logLlmResponse;

    @Inject
    public LlmSyntaxExtraction(
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
        this.syntaxFormat = readSyntaxFormat();
    }

    @Override
    public Action extractSyntax(RawText rawRequirementText) {
        Objects.requireNonNull(rawRequirementText, "rawRequirementText must not be null");

        String llmResponse = llmClientPort.generate(
            buildPrompt(rawRequirementText),
            syntaxFormat.deepCopy()
        );
        JsonNode modelOutput = modelOutputFrom(llmResponse);

        if (logLlmResponse) {
            log.info(
                Observability.block(
                    "llm.response.model_output",
                    Observability.kv("service", "syntax-extraction"),
                    Observability.section("model_output", modelOutput)
                )
            );
        }

        SyntaxExtractionOutput syntaxExtractionOutput = readSyntaxExtractionOutput(modelOutput);

        structuredOutputValidator.validate(
            SyntaxExtractionOutput.contract(),
            syntaxExtractionOutput
        );

        return syntaxExtractionOutput.toAction();
    }

    private JsonNode readSyntaxFormat() {
        try (InputStream input = LlmSyntaxExtraction.class.getResourceAsStream(SYNTAX_FORMAT)) {
            if (input == null) {
                throw new InvalidStructuredOutputException("requirement syntax format contract is missing");
            }

            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new InvalidStructuredOutputException("requirement syntax format contract cannot be read", e);
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

    private SyntaxExtractionOutput readSyntaxExtractionOutput(JsonNode modelOutput) {
        try {
            return objectMapper.treeToValue(modelOutput, SyntaxExtractionOutput.class);
        } catch (JsonProcessingException e) {
            throw new InvalidStructuredOutputException(
                "model output does not match SyntaxExtractionOutput",
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
        Extract the following requirement elements from the requirement:

        SUBJECT:
        ACTION:
        OBJECT:
        CONSTRAINT:
        CONDITION:

        Normalize extracted values with these rules:
        - SUBJECT and OBJECT omit leading articles or determiners or prepositional complements such as "a", "an", "the", "for".
        - CONSTRAINT keep temporal adjuncts such as "for"
        - CONDITION omits leading condition markers such as "if", "when", and "whenever".
        - CONDITION omits trailing commas.

        Respond only with valid JSON matching this shape:
        {
          "requirementElements": {
            "SUBJECT": "",
            "ACTION": "",
            "OBJECT": "",
            "CONSTRAINT": "",
            "CONDITION": ""
          }
        }

        Requirement:
        {{requirement}}
        """)
            .systemPrompt("""
            You extract structured information from software requirements.
            You must respond with valid JSON only.
            Use an empty string when a requirement element is absent.
            Normalize extracted values exactly as requested by the prompt.
            """)
            .variable("requirement", rawRequirementText.text())
            .addFewShotExample(
                """
                Requirement:
                If a user enters an invalid password three times, the authentication service must lock the account for 15 minutes.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "authentication service",
                    "ACTION": "must lock",
                    "OBJECT": "account",
                    "CONSTRAINT": "for 15 minutes",
                    "CONDITION": "user enters an invalid password three times"
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Das System soll die Bestellung innerhalb von 2 Sekunden bestätigen.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "System",
                    "ACTION": "soll bestätigen",
                    "OBJECT": "Bestellung",
                    "CONSTRAINT": "innerhalb von 2 Sekunden",
                    "CONDITION": ""
                  }
                }
                """
            )
            .build();
    }
}
