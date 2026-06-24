package io.fekav.platform.llm;

import com.fasterxml.jackson.databind.JsonNode;

public interface LlmClientPort {
    String generate(Prompt prompt, JsonNode format);
}
