package io.fekav.platform.observability;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.MDC;

/**
 * Minimal structured logging helper.
 *
 * Keep this deliberately small so OpenTelemetry spans can replace these
 * operation boundaries later without changing application code everywhere.
 */
public final class Observability {

    public static final String TRACE_ID_KEY = "trace_id";

    private Observability() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    public static String currentTraceId() {
        Object traceId = MDC.get(TRACE_ID_KEY);
        return traceId == null ? "-" : traceId.toString();
    }

    public static String event(String name) {
        return kv("event", name) + " " + kv(TRACE_ID_KEY, currentTraceId());
    }

    public static String block(String name, String metadata, Section... sections) {
        StringBuilder message = new StringBuilder(event(name));

        if (metadata != null && !metadata.isBlank()) {
            message.append(" ").append(metadata);
        }

        for (Section section : sections) {
            message
                .append(System.lineSeparator())
                .append("  --- ")
                .append(section.name())
                .append(" ---")
                .append(System.lineSeparator())
                .append(indent(sectionValue(section.value())));
        }

        return message.toString();
    }

    public static Section section(String name, Object value) {
        return new Section(name, value);
    }

    public static String kv(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");

        if (value == null) {
            return key + "=null";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return key + "=" + value;
        }

        return key + "=\"" + escape(String.valueOf(value)) + "\"";
    }

    public static long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public static int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    public record Section(String name, Object value) {

        public Section {
            name = Objects.requireNonNull(name, "name must not be null");
        }
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }

        return escaped.toString();
    }

    private static String sectionValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof JsonNode jsonNode) {
            return jsonNode.toPrettyString();
        }

        return String.valueOf(value);
    }

    private static String indent(String value) {
        String[] lines = value.split("\\R", -1);
        StringBuilder indented = new StringBuilder(value.length() + lines.length * 2);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                indented.append(System.lineSeparator());
            }
            indented.append("  ").append(lines[i]);
        }

        return indented.toString();
    }
}
