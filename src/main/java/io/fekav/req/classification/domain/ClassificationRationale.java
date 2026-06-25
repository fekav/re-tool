package io.fekav.req.classification.domain;

public record ClassificationRationale(String text) {

    public ClassificationRationale {
        if (text == null || text.isBlank()) {
            throw new InvalidClassificationRationaleException();
        }

        text = text.strip();
    }
}
