package io.fekav.platform.llm;

import java.util.Map;

public record LlmRequest(
    String prompt,
    Map<String, Object> jsonSchema
) {}
