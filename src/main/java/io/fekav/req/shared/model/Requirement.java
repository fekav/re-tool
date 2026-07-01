package io.fekav.req.shared.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.syntaxextraction.domain.Action;

// Domain Model / Aggregat-Root
public class Requirement {
    private final RequirementId id;
    private final RawText rawText;
    private RequirementStatus status;
    private Action action;
    private Classification classificationResult;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    private Requirement(RequirementId id, RawText rawText) {
        this.id = id;
        this.rawText = rawText;
        this.status = RequirementStatus.PENDING;
    }    

    public static Requirement create(String rawText) {
        return create(new RawText(rawText));
    }

    public static Requirement create(RawText rawRequirementText) {
        return new Requirement(RequirementId.create(), rawRequirementText);
    }

    public RawText getRawText() {
        return rawText;
    }

    public void applyExtraction(Action action) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.status = RequirementStatus.EXTRACTED;
        
        domainEvents.add(
            RequirementElementsExtractedEvent.create(this.rawText, action)
        );
    }

    public void applyClassification(Classification classification) {
        this.classificationResult = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        this.status = RequirementStatus.CLASSIFIED;

        domainEvents.add(
            RequirementClassifiedEvent.create(this.rawText, classification)
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
