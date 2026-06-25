package io.fekav.platform.structuredoutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record StructuredOutputContract<T>(
    String name,
    List<TextField<T>> textFields
) {

    public StructuredOutputContract {
        name = requireNonBlank(name, "name");
        textFields = List.copyOf(Objects.requireNonNull(textFields, "textFields must not be null"));
    }

    public static <T> Builder<T> named(String name) {
        return new Builder<>(name);
    }

    public static final class Builder<T> {

        private final String name;
        private final List<TextField<T>> textFields = new ArrayList<>();

        private Builder(String name) {
            this.name = requireNonBlank(name, "name");
        }

        public Builder<T> requiredText(String path, Function<T, String> read) {
            textFields.add(new TextField<>(path, read, true));
            return this;
        }

        public Builder<T> optionalText(String path, Function<T, String> read) {
            textFields.add(new TextField<>(path, read, false));
            return this;
        }

        public StructuredOutputContract<T> build() {
            return new StructuredOutputContract<>(name, textFields);
        }
    }

    public record TextField<T>(
        String path,
        Function<T, String> read,
        boolean required
    ) {

        public TextField {
            path = requireNonBlank(path, "path");
            read = Objects.requireNonNull(read, "read must not be null");
        }

        boolean isMissingFrom(T output) {
            String extractedText = read.apply(output);
            return required && (extractedText == null || extractedText.isBlank());
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value;
    }
}
