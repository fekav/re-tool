package io.fekav.platform.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OllamaResponseParser {

    private final ObjectMapper mapper;

    @Inject
    public OllamaResponseParser(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    public OllamaResponse parse(String responseJson) {
        try {
            JsonNode response = mapper.readTree(responseJson);
            return new OllamaResponse(response.path("response").asText());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Invalid Ollama response JSON",
                e
            );
        }
    }
}
