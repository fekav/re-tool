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

  private static final String SYNTAX_FORMAT = "/contracts/ai/v1/requirement-syntax.schema.json";

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
      StructuredOutputValidator structuredOutputValidator) {
    this.llmClientPort = Objects.requireNonNull(llmClientPort, "llmClientPort must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.structuredOutputValidator = Objects.requireNonNull(
        structuredOutputValidator,
        "structuredOutputValidator must not be null");
    this.syntaxFormat = readSyntaxFormat();
  }

  @Override
  public Action extractSyntax(RawText rawRequirementText) {
    Objects.requireNonNull(rawRequirementText, "rawRequirementText must not be null");

    String llmResponse = llmClientPort.generate(
        buildPrompt(rawRequirementText),
        syntaxFormat.deepCopy());
    JsonNode modelOutput = modelOutputFrom(llmResponse);

    if (logLlmResponse) {
      log.info(
          Observability.block(
              "llm.response.model_output",
              Observability.kv("service", "syntax-extraction"),
              Observability.section("model_output", modelOutput)));
    }

    SyntaxExtractionOutput syntaxExtractionOutput = readSyntaxExtractionOutput(modelOutput);

    structuredOutputValidator.validate(
        SyntaxExtractionOutput.contract(),
        syntaxExtractionOutput);

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
          e);
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
    return PromptFactory
        .fromTemplate(
            """
                TASK

                Extract the linguistic parts of the requirement sentence and return only valid JSON.

                Extract text spans from the requirement only.
                Do not paraphrase, summarize, translate, or invent wording.

                A requirement can contain zero, one, or several CONDITION phrases, and zero, one,
                or several CONSTRAINT phrases. Every qualifying phrase must be represented in the
                output. Never drop a phrase, and never merge two unrelated phrases together just
                to make the output fit a single value.

                DEFINITIONS

                SUBJECT
                The grammatical subject (Subjekt): the noun, or actor without its leading article or determiner, that performs, owns, or is responsible for the main predicate. 

                ACTION
                The complete predicate (Prädikat): the modal or auxiliary verb together with the main verb, ALWAYS including any negation marker that negates the predicate
                Examples:
                - shall export
                - must notify
                - soll speichern
                - darf nicht anzeigen

                OBJECT
                The grammatical object (Objekt): the noun without its leading article or determiner, that is governed by the predicate.

                CONDITION
                A trigger that determines whether or when the requirement applies.

                Typical examples:
                if
                when
                whenever
                unless
                after
                before
                during
                once
                upon
                falls
                wenn
                sobald
                nachdem
                bevor
                während
                bei
                im fall von

                CONSTRAINT
                A restriction describing how, where, or in which context the action must be fulfilled.

                Typical examples:
                within
                by
                via
                with
                without
                only
                at least
                at most
                immediately
                automatically
                in
                im
                innerhalb
                bis
                über
                per
                mit
                ohne
                nur
                mindestens
                höchstens
                sofort
                automatisch


                PRIORITY RULES

                Apply these rules in order.

                1.
                Identify SUBJECT, ACTION and OBJECT from the main predication.

                2.
                If OBJECT cannot be identified because the requirement is copular or describes availability, use the required role, recipient, beneficiary or target group as OBJECT.

                3.
                SUBJECT, ACTION and OBJECT must never be empty.

                4.
                All remaining phrases must become candidates for CONDITION or CONSTRAINT.                 

                5.
                Normalize every extracted value according to the normalization rules below.


                CONDITION VS CONSTRAINT

                Apply the following test separately to every remaining phrase identified in the audit step, one phrase at a time. 
                A requirement can contain several qualifying phrases; evaluate each one on its own and do not stop after finding the first match.

                For every remaining phrase ask:

                Question 1

                Does removing this phrase change WHEN or WHETHER the requirement applies?

                Examples:
                after login
                during maintenance
                if payment fails
                when authenticated

                YES → CONDITION

                NO → continue.


                Question 2

                Does removing this phrase change HOW or WHERE or in WHICH CONTEXT the requirement must be fulfilled?

                Examples:
                within 10 seconds
                via API
                by email
                as CSV
                only for administrators
                with AES-256 encryption
                immediately
                once per day
                im Kontaktformular 

                YES → CONSTRAINT

                NO → ignore.

                
                NORMALIZATION

                Normalize only as follows:

                - convert to lowercase
                - remove articles or determiners from SUBJECT and OBJECT, such as "the", "a", "an", "der", "die", "das", "ein", "eine"
                - keep the modal or auxiliary verb together with the main verb
                - remove leading condition markers from CONDITION
                - remove trailing commas from CONDITION
                - preserve meaningful prepositions such as:
                  before
                  after
                  during
                  on
                  within
                  via
                  by
                  per
                  über
                  innerhalb von
                  nur nach

                Always singularize nouns.

                Every extracted value must be a contiguous span from the requirement after normalization. 

                Do not merge unrelated phrases.

                Each phrase may belong to only one field.

                COMPLETENESS CHECK

                Before emitting the JSON, silently re-scan the requirement one more time.
                Recall every adverbial and prepositional phrase you found during the audit step.
                Confirm that each one appears in exactly one field of your draft output, or has deliberately been ignored under Rule 5.
                If any phrase from your audit is missing from the draft output, add it to the correct array now.
                Do not describe, list, or explain this check. Respond with the final JSON only.


                OUTPUT

                Always return:

                {
                  "requirementElements": {
                    "SUBJECT": "...",
                    "ACTION": "...",
                    "OBJECT": "...",
                    "CONSTRAINT": "...",
                    "CONDITION": "..."
                  }
                }

                Always emit every key.

                SUBJECT, ACTION and OBJECT must never be empty, and must be plain strings.

                CONSTRAINT and CONDITION are always arrays of strings. Use an empty string "" only if no corresponding phrase exists.


                Requirement:

                {{requirement}}
                        """)
        .systemPrompt("""
            You are an information extraction engine.
            Return valid JSON only.
            Never explain your reasoning.
            Never invent missing information.
            Follow the extraction specification exactly.
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
                """)
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
                """)
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
                """)
        .addFewShotExample(
            """
                Requirement:
                Die Nutzer sollen im Kontaktformular ihr Anliegen bequemer senden können.
                """,
            """
                {
                  "requirementElements": {
                    "SUBJECT": "nutzer",
                    "ACTION": "sollen senden",
                    "OBJECT": "anliegen",
                    "CONSTRAINT": "bequemer, im kontaktformular",
                    "CONDITION": ""
                  }
                }
                """)
        .addFewShotExample(
            """
                Requirement:
                Ein neuer Kunde soll keine Bestätigungsemail nach der Registrierung erhalten.
                """,
            """
                {
                  "requirementElements": {
                    "SUBJECT": "neuer kunde",
                    "ACTION": "sollen keine erhalten",
                    "OBJECT": "bestätigungsemail",
                    "CONSTRAINT": "nach registrierung",
                    "CONDITION": ""
                  }
                }
                """)
        .build();
  }
}
