package io.fekav.req.classification.infrastructure;

import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputContract;
import io.fekav.req.classification.domain.ClassificationRationale;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.InvalidClassificationRationaleException;
import io.fekav.req.classification.domain.InvalidConfidenceScoreException;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.classification.domain.RequirementConceptType;
import io.fekav.req.classification.domain.RequirementProperty;

public record RequirementClassificationOutput(
    RequirementClassificationFieldsOutput classification
) {

    static StructuredOutputContract<RequirementClassificationOutput> contract() {
        return StructuredOutputContract.<RequirementClassificationOutput>named("RequirementClassificationOutput")
            .requiredText("classification.conceptType", RequirementClassificationOutput::conceptType)
            .requiredText("classification.property", RequirementClassificationOutput::property)
            .requiredText("classification.confidenceScore", RequirementClassificationOutput::confidenceScoreText)
            .requiredText("classification.rationale", RequirementClassificationOutput::rationale)
            .build();
    }

    RequirementClassification toRequirementClassification() {
        return new RequirementClassification(
            requirementConceptType(),
            requirementProperty(),
            confidenceScore(),
            classificationRationale()
        );
    }

    private RequirementConceptType requirementConceptType() {
        try {
            return RequirementConceptType.valueOf(conceptType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidStructuredOutputException(
                "unsupported requirement concept type: " + conceptType(),
                e
            );
        }
    }

    private RequirementProperty requirementProperty() {
        try {
            return RequirementProperty.valueOf(property());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidStructuredOutputException(
                "unsupported requirement property: " + property(),
                e
            );
        }
    }

    private ConfidenceScore confidenceScore() {
        try {
            return new ConfidenceScore(classification.confidenceScore());
        } catch (InvalidConfidenceScoreException | NullPointerException e) {
            throw new InvalidStructuredOutputException("classification confidence score is invalid", e);
        }
    }

    private ClassificationRationale classificationRationale() {
        try {
            return new ClassificationRationale(rationale());
        } catch (InvalidClassificationRationaleException e) {
            throw new InvalidStructuredOutputException("classification rationale is invalid", e);
        }
    }

    private String conceptType() {
        return classification == null ? null : classification.conceptType();
    }

    private String property() {
        return classification == null ? null : classification.property();
    }

    private String confidenceScoreText() {
        Double confidenceScore = classification == null ? null : classification.confidenceScore();
        return confidenceScore == null ? null : confidenceScore.toString();
    }

    private String rationale() {
        return classification == null ? null : classification.rationale();
    }
}
