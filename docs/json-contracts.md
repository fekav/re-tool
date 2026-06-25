# JSON Contracts for Ollama Structured Output and Domain Core

## Purpose

The Java platform core may call Ollama over HTTP and receive structured JSON output. That output must not leak directly into the domain core. Treat Ollama as an external provider. The stable boundary is the app-owned JSON Schema and the mapped domain type.

## Core Rule

```text
Ollama API JSON       = provider transport format
Structured output JSON = model-produced payload
Domain contract       = app-owned schema and semantics
Domain core input     = validated domain type, not JsonNode or raw JSON
```

The domain core must not depend on Ollama classes, Jackson classes, HTTP response shapes, `JsonNode`, or `Map<String, Object>`.

## Recommended Flow

```text
Domain JSON Schema
        ↓
sent to Ollama as `format`
        ↓
Ollama returns HTTP wrapper JSON
        ↓
extract model-output JSON string
        ↓
parse with ObjectMapper into JsonNode
        ↓
validate JsonNode against JSON Schema
        ↓
map JsonNode to Java DTO
        ↓
map DTO to domain type
        ↓
pass domain type into domain/application core
```

## Ownership

In this project, a contract is an owned agreement at a boundary. It is not one
class, one DTO, or one schema file by itself. For LLM structured output, the
contract is the combination of the app-owned JSON shape, the Java DTO that binds
that shape, the validation rules that make the shape executable, and the mapper
that turns validated output into a domain type.

```text
Ollama HTTP request/response schema:
    owned by Ollama

Structured-output schema:
    owned by this application

Structured-output DTO:
    owned by this application; Java binding for the structured-output schema

Structured-output validation:
    owned by this application; executable checks before mapping to the domain

Domain model:
    owned by the domain core; semantic invariants after translation
```

Example: `RequirementSyntaxOutput` is the Java binding for the
requirement-syntax structured-output contract. Its `StructuredOutputContract`
describes required and optional fields for the handwritten validator. After
validation, it maps to `RequirementSyntax`, which is the domain type and not the
LLM output contract.

## Module Placement

```text
platform
  llm/
    OllamaClient
    OllamaChatRequest
    OllamaChatResponse
    OllamaStructuredOutputParser

contracts
  ai/v1/intent-result.schema.json

application
  IntentInterpreter
  AiInterpretationService
  CreateTaskCommand

domain
  Intent
  Confidence
  ExtractedEntities
  Task
  DueDate
```

Dependency direction:

```text
platform → application → domain
contracts are shared artifacts
```

## Example Contract

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://contracts.example.com/ai/v1/intent-result.schema.json",
  "type": "object",
  "required": ["intent", "confidence", "entities"],
  "additionalProperties": false,
  "properties": {
    "intent": {
      "type": "string",
      "enum": ["create_task", "cancel_task", "unknown"]
    },
    "confidence": {
      "type": "number",
      "minimum": 0,
      "maximum": 1
    },
    "entities": {
      "type": "object",
      "additionalProperties": {
        "type": ["string", "number", "boolean", "null"]
      }
    }
  }
}
```

This schema is reused for:

```text
Ollama structured-output `format`
Java-side validation
contract tests
documentation
future Python/C#/Go consumers
```

## Java DTO Example

```java
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IntentResultDto(
    @NotBlank
    String intent,

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    BigDecimal confidence,

    @NotNull
    JsonNode entities
) {}
```

Use `JsonNode` only for dynamic or pass-through fields such as `entities`. If the shape is known, replace it with a typed DTO.

## Ollama Adapter Example

```java
public final class OllamaIntentInterpreter implements IntentInterpreter {
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final JsonSchemaValidator schemaValidator;
    private final JsonNode intentResultSchema;

    public AiIntentResult interpret(String userMessage) {
        OllamaChatRequest request = new OllamaChatRequest(
            "llama3.2",
            List.of(new OllamaMessage(
                "user",
                """
                Classify the following user message.
                Return only JSON matching the provided schema.

                Message:
                %s
                """.formatted(userMessage)
            )),
            false,
            intentResultSchema
        );

        OllamaChatResponse response = ollamaClient.chat(request);

        JsonNode output = parseModelContent(response);
        schemaValidator.validate(output);

        IntentResultDto dto = toDto(output);

        return toDomain(dto);
    }

    private JsonNode parseModelContent(OllamaChatResponse response) {
        try {
            return objectMapper.readTree(response.message().content());
        } catch (JsonProcessingException e) {
            throw new InvalidModelOutputException("Invalid JSON from Ollama", e);
        }
    }

    private IntentResultDto toDto(JsonNode output) {
        try {
            return objectMapper.treeToValue(output, IntentResultDto.class);
        } catch (JsonProcessingException e) {
            throw new InvalidModelOutputException("JSON did not match IntentResultDto", e);
        }
    }

    private AiIntentResult toDomain(IntentResultDto dto) {
        return new AiIntentResult(
            Intent.fromExternalName(dto.intent()),
            Confidence.of(dto.confidence()),
            ExtractedEntities.fromJson(dto.entities())
        );
    }
}
```

## Where to Use `ObjectMapper`

Use `ObjectMapper` in the platform adapter layer for:

```text
serializing Ollama requests
deserializing Ollama response wrappers
parsing model-output JSON strings
converting JsonNode to DTOs
writing schemas or diagnostics
```

Do not use `ObjectMapper` in the domain core.

## Where to Use `JsonNode`

Good uses:

```text
Ollama `format` schema
raw parsed model output
dynamic entity bags
extension fields
audit/replay storage
diagnostics on validation failure
contract tests
```

Bad uses:

```text
domain model fields
domain service parameters
application commands when schema is known
replacement for DTOs
Map<String, Object> alternative everywhere
```

## Boundary Rules

Allowed at platform edge:

```java
JsonNode rawOutput = objectMapper.readTree(modelContent);
```

Allowed at DTO level for dynamic fields:

```java
public record IntentResultDto(String intent, BigDecimal confidence, JsonNode entities) {}
```

Not allowed in domain core:

```java
domainCore.handle(JsonNode ollamaOutput);
domainCore.handle(OllamaChatResponse response);
domainCore.handle(Map<String, Object> payload);
```

Preferred domain input:

```java
domainCore.handle(new AiIntentResult(intent, confidence, entities));
```

## Validation Layers

```text
JSON Schema:
    validates payload structure

DTO:
    represents the external JSON shape in Java

Domain value objects:
    enforce business invariants
```

Example:

```java
public record Confidence(BigDecimal value) {
    public Confidence {
        if (value == null) {
            throw new IllegalArgumentException("confidence is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
    }
}
```

## Agent / Skill Instruction

When implementing Ollama structured-output integrations:

1. Define or reuse a JSON Schema contract first.
2. Pass that schema to Ollama as the structured-output format.
3. Parse Ollama model content as JSON using `ObjectMapper.readTree`.
4. Validate the parsed `JsonNode` against the JSON Schema.
5. Convert the validated tree to a DTO.
6. Map the DTO to domain-specific types.
7. Keep Ollama types, Jackson types, HTTP types, and raw JSON out of the domain core.

## Final Principle

```text
The contract is the schema.
Jackson is the parser/mapper.
JsonNode is an edge representation.
DTOs represent external JSON.
Domain types represent business meaning.
```
