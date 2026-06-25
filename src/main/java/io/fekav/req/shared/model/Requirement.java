package io.fekav.req.shared.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.shared.event.EntitiesExtractedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;

// Domain Model / Aggregat-Root
public class Requirement {
    private final RequirementId id;
    private final String rawText;
    private RequirementStatus status;
    private RequirementSyntax extractionResult;
    private RequirementClassification classificationResult;
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

    public void applyClassification(RequirementClassification classification) {
        this.classificationResult = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        this.status = RequirementStatus.CLASSIFIED;

        domainEvents.add(
            RequirementClassifiedEvent.create(this.id, classification)
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
