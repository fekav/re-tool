package io.fekav.req.entityextraction.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
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

    

        // extract entities
        RequirementSyntax result = extractionService.extract(requirement);

        // 3. apply extraction to aggregate
        requirement.applyExtraction(result);

        // 4. publish Event
        eventPublisher.publishAll(requirement.domainEvents());

        return result;
    }

    @Override
    public Class<ExtractEntitiesCommand> commandType() {
        return ExtractEntitiesCommand.class;
    }

    // TODO use ddd policies
    private Prompt buildPrompt(String requirement) {

        return PromptFactory.fromTemplate("""
        Extract the following fields from the requirement:

        Actor:
        Action:
        Object:
        Condition:
        Constraint:

        Requirement:
        {{requirement}}
        """)
        .systemPrompt("""
                You extract structured information from software requirements.
                You must respond with valid JSON.
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
                Actor: authentication service
                Action: must lock
                Object: account
                Condition: user enters an invalid password three times
                Constraint: for 15 minutes
                """
        )
        .addFewShotExample(
                """
                Requirement:
                Das System soll die Bestellung innerhalb von 2 Sekunden bestätigen.
                """,
                """
                Actor: System
                Action: soll bestätigen
                Object: Bestellung
                Condition: null
                Constraint: innerhalb von 2 Sekunden
                """
        )
        .build();
    }

}
