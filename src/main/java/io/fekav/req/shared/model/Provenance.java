package io.fekav.req.shared.model;

import java.time.Instant;
import java.util.Objects;

public class Provenance {

    private final ElementId id;
    private final OriginalText originalText;
    private final SourceMetadata sourceMetadata;
    private final Instant ingestedAt;

    private Provenance(
        ElementId id,
        OriginalText originalText,
        SourceMetadata sourceMetadata,
        Instant ingestedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.originalText = Objects.requireNonNull(
            originalText,
            "originalText must not be null"
        );
        this.sourceMetadata = Objects.requireNonNull(
            sourceMetadata,
            "sourceMetadata must not be null"
        );
        this.ingestedAt = Objects.requireNonNull(
            ingestedAt,
            "ingestedAt must not be null"
        );
    }

    public static Provenance create(
        ElementId id,
        OriginalText originalText,
        SourceMetadata sourceMetadata,
        Instant ingestedAt
    ) {
        return new Provenance(id, originalText, sourceMetadata, ingestedAt);
    }

    public ElementId getId() {
        return id;
    }

    public OriginalText getOriginalText() {
        return originalText;
    }

    public SourceMetadata getSourceMetadata() {
        return sourceMetadata;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
