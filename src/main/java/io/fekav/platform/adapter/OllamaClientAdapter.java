package io.fekav.platform.adapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * LLM Adapter (Driven Side) for Ollama.
 * 
 * @see https://docs.ollama.com/api/generate
 *
 */
@ApplicationScoped
public class OllamaClientAdapter implements LlmClientPort {

    private static final Logger log = Logger.getLogger(OllamaClientAdapter.class.getName());

    @ConfigProperty(name = "llm.base.url", defaultValue = "http://ollama:11434/api/")
    String llmBaseUrl;

    @ConfigProperty(name = "llm.options.temperature", defaultValue = "0.0")
    Double llmTemperature;

    @ConfigProperty(name = "llm.model", defaultValue = "granite4.1:8b")
    String llmModel;

    private final ObjectMapper objectMapper;

    // HttpClient is no cdi bean, cant be injected without producing a bean
    private final HttpClient httpClient;

    private final boolean stream;

    @Inject
    public OllamaClientAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.stream = false;
    }

    @Override
    public String generate(Prompt prompt, JsonNode format) {
        if (llmBaseUrl.isBlank()) {
            throw new IllegalStateException("config property not set for llm Url");
        }

        try {
            String payload = buildRequestPayload(prompt, format);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(llmBaseUrl + "generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            log.info("Send http request: " + request + "\n with payload: " + payload);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Received response:\n" + response + " with body:\n" + response.body());
            return response.body();

        } catch (Exception e) {
            throw new IllegalStateException("llm request failed", e);
        }
    }

    private String buildRequestPayload(Prompt prompt, JsonNode format) {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", llmTemperature);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", llmModel);
        payload.put("prompt", prompt.promptText());
        payload.put("system", prompt.systemPrompt());
        payload.put("stream", stream);
        payload.put("format", format != null ? format : "json");
        payload.put("options", options);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("llm request payload serialization failed", e);
        }
    }
}