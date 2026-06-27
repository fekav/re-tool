package io.fekav.platform.adapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.observability.Observability;
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

    @ConfigProperty(name = "observability.log.llm-prompt", defaultValue = "true")
    boolean logLlmPrompt;

    @ConfigProperty(name = "observability.log.llm-response", defaultValue = "true")
    boolean logLlmResponse;

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
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(format, "format must not be null");
        long startNanos = System.nanoTime();
        String endpoint = llmBaseUrl + "generate";

        try {
            String payload = buildRequestPayload(prompt, format);

            log.info(
                Observability.event("llm.generate.start") + " " +
                    Observability.kv("provider", "ollama") + " " +
                    Observability.kv("model", llmModel) + " " +
                    Observability.kv("endpoint", endpoint) + " " +
                    Observability.kv("stream", stream) + " " +
                    Observability.kv("prompt_chars", Observability.lengthOf(prompt.promptText())) + " " +
                    Observability.kv("system_prompt_chars", Observability.lengthOf(prompt.systemPrompt()))
            );

            if (logLlmPrompt) {
                log.info(
                    Observability.block(
                        "llm.prompt",
                        Observability.kv("provider", "ollama") + " " +
                            Observability.kv("model", llmModel),
                        Observability.section("system_prompt", prompt.systemPrompt()),
                        Observability.section("prompt", prompt.promptText()),
                        Observability.section("format", format)
                    )
                );
                log.info(
                    Observability.block(
                        "llm.request.raw",
                        Observability.kv("provider", "ollama") + " " +
                            Observability.kv("model", llmModel),
                        Observability.section("payload", prettyJson(payload))
                    )
                );
            }

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (logLlmResponse) {
                log.info(
                    Observability.block(
                        "llm.response.raw",
                        Observability.kv("provider", "ollama") + " " +
                            Observability.kv("model", llmModel) + " " +
                            Observability.kv("http_status", response.statusCode()),
                        Observability.section("body", prettyJson(response.body()))
                    )
                );
            }

            log.info(
                Observability.event("llm.generate.end") + " " +
                    Observability.kv("provider", "ollama") + " " +
                    Observability.kv("model", llmModel) + " " +
                    Observability.kv("http_status", response.statusCode()) + " " +
                    Observability.kv("response_chars", Observability.lengthOf(response.body())) + " " +
                    Observability.kv("status", "ok") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos))
            );

            return response.body();

        } catch (Exception e) {
            log.error(
                Observability.event("llm.generate.end") + " " +
                    Observability.kv("provider", "ollama") + " " +
                    Observability.kv("model", llmModel) + " " +
                    Observability.kv("endpoint", endpoint) + " " +
                    Observability.kv("status", "error") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos)) + " " +
                    Observability.kv("error_type", e.getClass().getSimpleName()) + " " +
                    Observability.kv("error_message", e.getMessage()),
                e
            );
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

    private String prettyJson(String json) {
        try {
            return objectMapper.readTree(json).toPrettyString();
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
