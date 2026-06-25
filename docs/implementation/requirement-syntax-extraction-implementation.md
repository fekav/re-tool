# Spec: Requirement Syntax Extraction Implementation

## Objective

Implement the missing extraction capability used by `ExtractEntitiesCommandHandler`. The handler must orchestrate the use case only: create the requirement, request requirement syntax extraction, apply the extraction, publish domain events, and return `RequirementSyntax`.

## Tech Stack

Java 25, Quarkus CDI, Jackson, JUnit 5, Mockito, AssertJ, REST Assured.

## Commands

Build: `./gradlew build`

Unit tests: `./gradlew test`

Focused tests: `./gradlew test --tests '*RequirementSyntax*' --tests '*StructuredOutput*' --tests '*LlmRequirementSyntaxExtraction*' --tests '*ExtractEntitiesCommandHandler*'`

## Project Structure

`src/main/java/io/fekav/req/entityextraction/domain` contains domain values and domain rule exceptions.

`src/main/java/io/fekav/req/entityextraction/application` contains the command handler and extraction port.

`src/main/java/io/fekav/req/entityextraction/infrastructure` contains the LLM-backed extraction implementation and structured-output DTOs.

`src/main/java/io/fekav/platform/structuredoutput` contains the reusable handwritten DTO validator.

`src/main/resources/contracts/ai/v1` contains app-owned structured-output JSON contracts.

`src/test/java` mirrors source packages.

## New Files

| File | Description |
| --- | --- |
| `src/main/java/io/fekav/req/entityextraction/application/RequirementSyntaxExtraction.java` | Application port for extracting `RequirementSyntax` from raw requirement text. |
| `src/main/java/io/fekav/req/entityextraction/domain/MissingRequirementSyntaxElementException.java` | Domain exception for missing required syntax elements. |
| `src/main/java/io/fekav/req/entityextraction/infrastructure/LlmRequirementSyntaxExtraction.java` | LLM-backed adapter that calls the LLM port, parses structured output, validates it, and maps it to the domain type. |
| `src/main/java/io/fekav/req/entityextraction/infrastructure/RequirementSyntaxElementsOutput.java` | Nested DTO matching the structured-output `syntaxElements` object. |
| `src/main/java/io/fekav/req/entityextraction/infrastructure/RequirementSyntaxOutput.java` | Java binding for the requirement-syntax structured-output contract. |
| `src/main/java/io/fekav/req/shared/model/InvalidRawRequirementTextException.java` | Shared exception for blank or missing raw requirement text. |
| `src/main/java/io/fekav/req/shared/model/RawRequirementText.java` | Shared value object for user-provided requirement text. |
| `src/main/java/io/fekav/platform/structuredoutput/InvalidStructuredOutputException.java` | Runtime exception for malformed or unusable provider output. |
| `src/main/java/io/fekav/platform/structuredoutput/StructuredOutputContract.java` | Reusable description of required and optional DTO text fields. |
| `src/main/java/io/fekav/platform/structuredoutput/StructuredOutputValidationException.java` | Validation exception for DTO output that does not satisfy its contract. |
| `src/main/java/io/fekav/platform/structuredoutput/StructuredOutputValidator.java` | Small handwritten validator for structured-output DTO contracts. |
| `src/main/resources/contracts/ai/v1/requirement-syntax.schema.json` | App-owned JSON Schema sent to the LLM as the structured-output format. |
| `src/test/java/io/fekav/req/entityextraction/application/ExtractEntitiesCommandHandlerTest.java` | Unit tests for command-handler orchestration. |
| `src/test/java/io/fekav/req/entityextraction/domain/RequirementSyntaxTest.java` | Unit tests for required and optional `RequirementSyntax` elements. |
| `src/test/java/io/fekav/req/entityextraction/infrastructure/LlmRequirementSyntaxExtractionTest.java` | Unit tests for LLM response parsing, validation, and mapping. |
| `src/test/java/io/fekav/req/shared/model/RawRequirementTextTest.java` | Unit tests for raw requirement text invariants. |
| `src/test/java/io/fekav/platform/api/RestControllerTestIT.java` | REST integration test using the extraction port mock. |
| `src/test/java/io/fekav/platform/structuredoutput/StructuredOutputValidatorTest.java` | Unit tests for the reusable handwritten validator. |

## Code Style

Use records for value objects and DTOs, interfaces for ports, and constructor injection for CDI beans.

```java
public interface RequirementSyntaxExtraction {
    RequirementSyntax extractRequirementSyntax(RawRequirementText rawRequirementText);
}
```

## Testing Strategy

Small JUnit tests cover domain invariants, DTO validation, DTO mapping, and LLM response parsing. The command handler test mocks the extraction port and event publisher. The REST integration test mocks the extraction port rather than the LLM provider.

## Boundaries

Always: keep Jackson, raw JSON, and Ollama response shapes out of the domain model.

Ask first: adding a JSON Schema validation dependency or changing the external REST request shape.

Never: deserialize model output directly into domain types or put provider-specific names in the extraction port.

## Success Criteria

- `RequirementSyntax` requires subject, action, and object.
- Condition and constraint are optional.
- The handwritten validator is reusable across DTO contracts.
- `ExtractEntitiesCommandHandler` no longer injects `LlmClientPort` or `ObjectMapper`.
- The LLM extraction implementation parses the Ollama wrapper, validates DTO output, maps to domain, and passes the app-owned schema as `format`.

## Open Questions

None. The first implementation uses a small handwritten validator, not a JSON Schema validation dependency.
