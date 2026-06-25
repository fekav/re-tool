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

## Structured-Output Contracts

The structured-output contract is split across artifacts on purpose:

```text
JSON Schema resource
    src/main/resources/contracts/ai/v1/requirement-syntax.schema.json

Java DTO binding
    RequirementSyntaxOutput
    RequirementSyntaxElementsOutput

Executable validation
    StructuredOutputContract
    StructuredOutputValidator

Domain result
    RequirementSyntax
```

The DTOs describe the external model-output shape. The domain type describes the
validated business concept. New LLM-backed features should follow the same
boundary: schema and DTOs at the infrastructure edge, reusable validation in
`platform.structuredoutput`, and domain types inside the owning slice.

See also:

- `docs/json-contracts.md`
- `docs/requirement-syntax-extraction-implementation.md`
