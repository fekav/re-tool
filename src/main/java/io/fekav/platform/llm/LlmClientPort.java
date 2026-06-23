package io.fekav.platform.llm;

public interface LlmClientPort {
    String generate(LlmRequest request);
}
