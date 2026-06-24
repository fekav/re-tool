package io.fekav.platform.llm;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public interface LlmClientPort {
    String generate(String promptText, JsonNode format);
}
