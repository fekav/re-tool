# Architecture

## Shape

The project follows a small hexagonal and vertical-slice structure. The `platform`
package owns technical capabilities shared by the application. The `req` package
owns the requirement domain and its slices.

```text
src/main/java/io/fekav
├── platform
│   ├── adapter                     // CDI, CQRS, and external-provider adapters
│   ├── api                         // REST entrypoints and exception mapping
│   ├── cqrs                        // command and query, bus
│   ├── llm                         // provider-neutral LLM port and prompt model
│   ├── messaging                   // domain-event contracts
│   └── structuredoutput            // reusable structured-output validation
│
└── req
    ├── shared                      // domain-wide, cross-slice
    │   ├── event
    │   └── model
    │
    ├── classification
    │   ├── application             // command handler and classification port
    │   ├── domain                  // classification values and evidence
    │   └── infrastructure          // LLM-backed adapter and output DTOs
    │
    └── entityextraction
        ├── application             // command handler and application ports
        ├── domain                  // domain values and domain rule exceptions
        └── infrastructure          // LLM-backed adapter and output DTOs
```

## Dependency Direction

Dependencies point inward:

```text
platform api/adapter -> req application -> req domain
req infrastructure   -> req application -> req domain
req slices           -> req shared model/event, when concepts are shared
```

The domain model does not depend on REST, CDI, Jackson, Ollama, HTTP response
shapes, or raw JSON types.

## Requirement Syntax Extraction

`ExtractEntitiesCommandHandler` is the use-case orchestrator. It creates the
requirement, calls the `RequirementSyntaxExtraction` application port, applies
the returned `RequirementSyntax`, publishes domain events, and returns the
domain result.

The current port implementation is `LlmRequirementSyntaxExtraction` in the
`entityextraction.infrastructure` package. It talks to the provider-neutral
`LlmClientPort`, sends the app-owned JSON Schema as the structured-output
format, parses the provider wrapper, validates the model output DTO, and maps
the DTO to the domain type.

```text
REST/API
  -> ExtractEntitiesCommandHandler
  -> RequirementSyntaxExtraction port
  -> LlmRequirementSyntaxExtraction
  -> LlmClientPort
  -> structured-output DTO validation
  -> RequirementSyntax domain type
  -> Requirement aggregate
  -> domain events
```

## Requirement Classification

`ClassifyRequirementCommandHandler` is the use-case orchestrator for
`CLASSIFY_REQUIREMENT`. It creates the requirement, calls the
`RequirementClassificationService` application port, applies the returned
`RequirementClassification`, publishes domain events, and returns the domain
result. Low confidence remains part of the returned result so consumers can
decide whether review or triage is needed.

The current port implementation is `LlmRequirementClassificationService` in the
`classification.infrastructure` package. It follows the same provider-boundary
pattern as syntax extraction: send the app-owned JSON Schema as the
structured-output format, parse the provider wrapper, validate the model output
DTO, reject unsupported enum values or invalid evidence, and map only validated
output into the domain model.

```text
REST/API
  -> ClassifyRequirementCommandHandler
  -> RequirementClassificationService port
  -> LlmRequirementClassificationService
  -> LlmClientPort
  -> structured-output DTO validation
  -> RequirementClassification domain type
  -> Requirement aggregate
  -> domain events
```

## Structured-Output Contracts

The structured-output contract is split across artifacts on purpose:

```text
JSON Schema resource
    src/main/resources/contracts/ai/v1/requirement-syntax.schema.json
    src/main/resources/contracts/ai/v1/requirement-classification.schema.json

Java DTO binding
    RequirementSyntaxOutput
    RequirementSyntaxElementsOutput
    RequirementClassificationOutput
    RequirementClassificationFieldsOutput

Executable validation
    StructuredOutputContract
    StructuredOutputValidator

Domain result
    RequirementSyntax
    RequirementClassification
```

The DTOs describe the external model-output shape. The domain type describes the
validated business concept. New LLM-backed features should follow the same
boundary: schema and DTOs at the infrastructure edge, reusable validation in
`platform.structuredoutput`, and domain types inside the owning slice.

See also `docs/json-contracts.md` for general concept overview.
