package io.fekav.req.shared.model;

import java.util.ArrayList;
import java.util.List;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.shared.event.EntitiesExtractedEvent;

// Domain Model / Aggregat-Root
public class Requirement {
    private final RequirementId id;
    private final String rawText;
    private RequirementStatus status;
    private RequirementSyntax extractionResult;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    private Requirement(RequirementId id, String rawText) {
        this.id = id;
        this.rawText = rawText;
        this.status = RequirementStatus.PENDING;
    }    

    public static Requirement create(String rawText) {
        return create(new RawRequirementText(rawText));
    }

    public static Requirement create(RawRequirementText rawRequirementText) {
        return new Requirement(RequirementId.create(), rawRequirementText.text());
    }

    public String getRawText() {
        return rawText;
    }

    public void applyExtraction(RequirementSyntax result) {
        if (result.syntaxElements().isEmpty()) {
            throw new IllegalArgumentException("requirement entity extraction object is empty");
        }
        this.extractionResult = result;
        this.status = RequirementStatus.EXTRACTED;
        
        domainEvents.add(
            EntitiesExtractedEvent.create(this.id, result)
        );
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public RequirementId getId() {
        return id;
    }       

    public List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearEvents() {
        domainEvents.clear();
    }
    
}
