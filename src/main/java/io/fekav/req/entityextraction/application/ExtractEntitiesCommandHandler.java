package io.fekav.req.entityextraction.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.entityextraction.domain.RequirementSyntaxType;
import io.fekav.req.shared.model.Requirement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Command Handler (Use Case implementation)
 * 
 */
@ApplicationScoped
public class ExtractEntitiesCommandHandler
        implements CommandHandler<RequirementSyntax, ExtractEntitiesCommand> {

    @Inject
    EventPublisher eventPublisher;

    @Inject
    LlmClientPort llmClientPort;

    @Inject
    ObjectMapper objectMapper;


    @Override
    @Transactional
    public RequirementSyntax handle(ExtractEntitiesCommand command) {
        // create Aggregate
        Requirement requirement = Requirement.create(command.rawText());

        // build prompt
        Prompt prompt = buildPrompt(command.rawText());

        RequirementSyntax requirementSyntax = extractRequirementSyntax(prompt);

        // apply extraction to aggregate
        requirement.applyExtraction(requirementSyntax);

        // 4. publish Event
        eventPublisher.publishAll(requirement.domainEvents());

        // return RequirementSyntax
        return requirementSyntax;
    }

    @Override
    public Class<ExtractEntitiesCommand> commandType() {
        return ExtractEntitiesCommand.class;
    }

    private RequirementSyntax extractRequirementSyntax(Prompt prompt) {
        String response = llmClientPort.generate(prompt, requirementSyntaxFormat());

        try {
            JsonNode responseBody = objectMapper.readTree(response);
            String requirementSyntaxJson = responseBody.path("response").asText(responseBody.toString());

            return objectMapper.readValue(requirementSyntaxJson, RequirementSyntax.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Requirement syntax extraction response could not be parsed", e);
        }
    }

    private JsonNode requirementSyntaxFormat() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode syntaxElements = properties.putObject("syntaxElements");
        syntaxElements.put("type", "object");

        ObjectNode syntaxElementProperties = syntaxElements.putObject("properties");
        for (RequirementSyntaxType requirementSyntaxType : RequirementSyntaxType.values()) {
            syntaxElementProperties
                    .putObject(requirementSyntaxType.name())
                    .put("type", "string");
        }

        syntaxElements.put("additionalProperties", false);
        syntaxElements.putArray("required")
                .add(RequirementSyntaxType.SUBJECT.name())
                .add(RequirementSyntaxType.ACTION.name())
                .add(RequirementSyntaxType.OBJECT.name())
                .add(RequirementSyntaxType.CONSTRAINT.name())
                .add(RequirementSyntaxType.CONDITION.name());

        schema.put("additionalProperties", false);
        schema.putArray("required").add("syntaxElements");

        return schema;
    }

    // TODO use ddd policies
    private Prompt buildPrompt(String requirement) {

        return PromptFactory.fromTemplate("""
        Extract the following syntax elements from the requirement:

        SUBJECT:
        ACTION:
        OBJECT:
        CONSTRAINT:
        CONDITION:

        Respond only with valid JSON matching this shape:
        {
          "syntaxElements": {
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
                Use an empty string when a syntax element is absent.
                """)
        .variable(
                "requirement",
                requirement
        )
        .addFewShotExample(
                """
                Requirement:
                If a user enters an invalid password three times, the authentication service must lock the account for 15 minutes.
                """,
                """
                {
                  "syntaxElements": {
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
                  "syntaxElements": {
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
