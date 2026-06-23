package io.fekav.platform.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PromptValues {

    private PromptValues() {
    }

    static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    static String labelOrDefault(String value) {
        return value == null || value.isBlank() ? "Input" : value.strip();
    }

    static List<PromptExample> copyExamples(List<PromptExample> source) {
        return source == null || source.isEmpty() ? List.of() : List.copyOf(source);
    }

    static Map<String, Object> copyObjectMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, copyValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> {
                if (!(key instanceof String textKey)) {
                    throw new IllegalArgumentException("prompt maps must use string keys");
                }
                copy.put(textKey, copyValue(nestedValue));
            });
            return Collections.unmodifiableMap(copy);
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(copyValue(item)));
            return Collections.unmodifiableList(copy);
        }

        if (value instanceof Object[] array) {
            List<Object> copy = new ArrayList<>(array.length);
            for (Object item : array) {
                copy.add(copyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }

        return value;
    }
}
