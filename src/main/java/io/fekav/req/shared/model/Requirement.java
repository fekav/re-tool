package io.fekav.req.shared.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.event.EntitiesExtractedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.syntaxextraction.domain.Action;

// Domain Model / Aggregat-Root
public class Requirement {
    private final ElementId id;
    private final String rawText;
    private RequirementStatus status;
    private Action action;
    private Classification classificationResult;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    private Requirement(ElementId id, String rawText) {
        this.id = id;
        this.rawText = rawText;
        this.status = RequirementStatus.PENDING;
    }    

    public static Requirement create(String rawText) {
        return create(new RawText(rawText));
    }

    public static Requirement create(RawText rawRequirementText) {
        return new Requirement(ElementId.create(), rawRequirementText.text());
    }

    public String getRawText() {
        return rawText;
    }

    public void applyExtraction(Action action) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.status = RequirementStatus.EXTRACTED;
        
        domainEvents.add(
            EntitiesExtractedEvent.create(this.id, action)
        );
    }

    public void applyClassification(Classification classification) {
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

    public ElementId getId() {
        return id;
    }       

    public List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearEvents() {
        domainEvents.clear();
    }
    
}
