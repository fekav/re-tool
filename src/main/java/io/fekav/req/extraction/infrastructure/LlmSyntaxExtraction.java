package io.fekav.req.extraction.infrastructure;

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
import io.fekav.req.extraction.application.SyntaxExtraction;
import io.fekav.req.extraction.domain.Action;
import io.fekav.req.shared.model.RawText;
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
        Extract the linguistic parts of the requirement sentence and map them to the JSON keys.

        Linguistic parts:
        - SUBJECT is the grammatical subject (Subjekt): the noun phrase that performs, owns, or is responsible for
          the predicate.
        - ACTION is the predicate (Prädikat): the full required verb phrase, including modal or auxiliary verbs such as
          "shall", "must", "soll", or "muss" plus the main verb. Do not omit the verb.
        - OBJECT is the grammatical object (Objekt): the noun phrase affected, created, read, notified, stored,
          displayed, or otherwise governed by the predicate. A core object is not a CONSTRAINT, even when it follows
          a preposition.
        - CONDITION is a conditional or temporal clause/adverbial that states when, if, or under which trigger the
          whole predication applies. Typical cues include "if", "when", "whenever", "unless", "after", "before",
          "during", "once", "as soon as", "on", "upon", "in case of", "wenn", "falls", "sobald", "nachdem",
          "bevor", "während", "bei", and "im fall von".
        - CONSTRAINT is a non-core modifier of the predicate: a limit, format, deadline, frequency, manner, quality,
          quantity, location, permission boundary, or other restriction on how the predicate must be fulfilled.
          Typical cues include "within", "by", "for", "as", "via", "with", "without", "only", "at least",
          "at most", "no more than", "immediately", "automatically", "innerhalb", "bis", "als", "per",
          "über", "mit", "ohne", "nur", "mindestens", "maximal", "höchstens", "sofort", and "automatisch".

        Extraction process:
        1. Identify SUBJECT, ACTION, and OBJECT from the main predication.
        2. Scan every remaining phrase before, inside, and after the main predication.
        3. If a remaining phrase changes when or whether the requirement applies, put it in CONDITION.
        4. If a remaining phrase restricts how, how well, how fast, how often, how long, where, in which format,
           by which channel, for whom, or under which permission boundary the action must be fulfilled, put it in
           CONSTRAINT.
        5. Do not drop a qualifier because SUBJECT, ACTION, and OBJECT already make a complete sentence. Completeness
           of the main predication is not evidence that CONDITION or CONSTRAINT is absent.
        6. If multiple condition phrases exist, combine them in reading order in CONDITION. If multiple constraint
           phrases exist, combine them in reading order in CONSTRAINT.

        CONDITION versus CONSTRAINT tie-breakers:
        - Use CONDITION for triggers, preconditions, states, events, or time windows that decide when the requirement
          is active.
        - Use CONSTRAINT for measurable limits, deadlines, durations, formats, channels, quality levels, manner words,
          access or permission boundaries, locations, target groups, and other fulfillment restrictions.
        - Phrases that name the interface or channel used to perform the action are CONSTRAINT, for example
          "via API", "by email", "per E-Mail", or "über die Chatoberfläche".
        - When a temporal phrase is a trigger such as "after login" or "during incidents", prefer CONDITION.
        - When a temporal phrase is a fulfillment limit such as "within 10 seconds" or "for 15 minutes", prefer
          CONSTRAINT.

        Normalize extracted values with these rules:
        - Use lowercase and singular form for all extracted values.
        - SUBJECT and OBJECT omit leading articles or determiners such as "a", "an", "the", "der", "die", "das",
          "ein", or "eine".
        - ACTION keeps the modal or auxiliary verb together with the main verb.
        - SUBJECT, ACTION, and OBJECT are required and must never be empty. If wording is elliptical, choose the
          closest noun phrase or verb phrase from the requirement that completes the predication.
        - For copular or availability requirements without a direct object, treat a required role, beneficiary,
          recipient, or target group as OBJECT instead of leaving OBJECT empty.
        - CONDITION omits leading condition markers such as "if", "when", "whenever", "unless", "wenn", "falls",
          "sobald", "nachdem", and "bevor"; omit trailing commas.
        - CONDITION keeps meaningful temporal prepositions when removing them would change the meaning, such as
          "before authentication", "during incidents", "after timeout", or "on request".
        - CONSTRAINT keeps meaningful prepositions and particles such as "within", "for", "as", "by",
          "via", "per", "über", "innerhalb von", and "nur nach".
        - Always emit the CONDITION and CONSTRAINT keys. Use an empty string only after the extraction process found
          no phrase in the requirement that belongs to that key.

        Respond only with valid JSON matching this shape:
        {
          "requirementElements": {
            "SUBJECT": "non-empty subject text",
            "ACTION": "non-empty predicate text",
            "OBJECT": "non-empty object text",
            "CONSTRAINT": "constraint text or empty string",
            "CONDITION": "condition text or empty string"
          }
        }

        Requirement:
        {{requirement}}
        """)
            .systemPrompt("""
            You extract linguistic parts from requirement sentences.
            You must respond with valid JSON only.
            SUBJECT, ACTION, and OBJECT are mandatory and must never be empty.
            Extract exactly as requested by the prompt.
            Before answering, audit every adverbial and prepositional phrase in the requirement.
            Use an empty string for CONDITION or CONSTRAINT only when no phrase in the raw requirement belongs there.
            Normalize extracted values exactly as requested by the prompt.
            """)
            .variable("requirement", rawRequirementText.text())
            .addFewShotExample(
                """
                Requirement:
                When a customer submits a refund request, the payment service must credit the original payment method within 3 business days.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "payment service",
                    "ACTION": "must credit",
                    "OBJECT": "original payment method",
                    "CONSTRAINT": "within 3 business days",
                    "CONDITION": "customer submits refund request"
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                The reporting dashboard shall export monthly usage metrics as a CSV file within 10 seconds.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "reporting dashboard",
                    "ACTION": "shall export",
                    "OBJECT": "monthly usage metrics",
                    "CONSTRAINT": "as a csv file within 10 seconds",
                    "CONDITION": ""
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                If sensor temperature exceeds 80 degrees Celsius, the monitoring service must notify the operator by email immediately.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "monitoring service",
                    "ACTION": "must notify",
                    "OBJECT": "operator",
                    "CONSTRAINT": "by email immediately",
                    "CONDITION": "sensor temperature exceeds 80 degrees celsius"
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Die User-management seite soll für admin nur nach einwilligung sein.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "user-management seite",
                    "ACTION": "soll sein",
                    "OBJECT": "admin",
                    "CONSTRAINT": "nur nach einwilligung",
                    "CONDITION": ""
                  }
                }
                """
            )
            .addFewShotExample(
                """
                Requirement:
                Kunden sollen leichter ihre Software liefern können.
                """,
                """
                {
                  "requirementElements": {
                    "SUBJECT": "kunden",
                    "ACTION": "sollen liefern",
                    "OBJECT": "software",
                    "CONSTRAINT": "leichter",
                    "CONDITION": ""
                  }
                }
                """
            )
            .build();
    }
}
