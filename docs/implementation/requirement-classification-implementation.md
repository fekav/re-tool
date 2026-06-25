# Spec: Requirement Classification Implementation

## Objective

Implement the `CLASSIFY_REQUIREMENT` workflow state as an LLM-backed capability in a new `classification` slice. The classifier accepts `RawRequirementText` and returns a `RequirementClassification` that identifies the initial KG concept type, property, confidence score, and rationale.

## Classification Semantics

### Concept Type

Concept type records intent and commitment level.

| Concept Type | Intent / Commitment Level | Classifier Signal | Example |
|---|---|---|---|
| `GOAL` | Desired outcome or business objective. Explains why change matters. | Outcome language, benefit, target state, strategic result. Usually not directly testable as one system behavior. | "Reduce failed customer onboarding by 30%." |
| `NEED` | Stakeholder need or capability gap. Explains what someone needs before it is expressed as a binding system obligation. | Stakeholder-centered language such as "needs", "wants", "must be able to", or problem statements. Often generates multiple requirements. | "Support agents need visibility into failed payment attempts." |
| `REQUIREMENT` | Binding product or system obligation. Specifies what the system must do or satisfy. | "shall", "must", "is required to", concrete behavior, or measurable constraint. Should be verifiable. | "The billing service must log failed payment attempts with reason codes." |

### Property

Property is a separate axis from concept type.

| Property | Meaning | Classifier Signal | Example |
|---|---|---|---|
| `FUNCTIONAL` | Behavior, capability, workflow, operation, or interaction. | Action or capability language describing something the system, user, or stakeholder can do. | "The dashboard shall export monthly usage metrics." |
| `QUALITY` | Quality attribute or constraint. | Performance, security, availability, usability, reliability, compliance, scalability, or measurable constraint language. | "The dashboard export must complete within 2 seconds." |

### Classification Matrix

| Raw Text | Concept Type | Property |
|---|---|---|
| "Reduce checkout abandonment." | `GOAL` | `FUNCTIONAL` |
| "Improve checkout response time for mobile users." | `GOAL` | `QUALITY` |
| "Customers need to complete checkout without creating an account." | `NEED` | `FUNCTIONAL` |
| "Customers need checkout pages to load quickly on mobile networks." | `NEED` | `QUALITY` |
| "The checkout service must support guest checkout." | `REQUIREMENT` | `FUNCTIONAL` |
| "The checkout page must load within 2 seconds on a 4G connection." | `REQUIREMENT` | `QUALITY` |

### Classification Evidence

V1 uses one overall confidence score for the complete classification result rather than separate scores for concept type and property.

| Field | Meaning | Validation |
|---|---|---|
| `confidenceScore` | Classifier certainty that the selected concept type and property are correct for the raw text. | Required number from `0.0` to `1.0`. |
| `rationale` | Short explanation grounded in the raw text that justifies the concept type and property. | Required non-blank text. |

Example:

```json
{
  "classification": {
    "conceptType": "REQUIREMENT",
    "property": "QUALITY",
    "confidenceScore": 0.93,
    "rationale": "The sentence uses must and gives a measurable response-time constraint."
  }
}
```

### Prompt Classifier Signals

Classifier signals should help the model choose consistently, but they are cues rather than hard keyword rules. The prompt should ask the model to choose in this order:

1. Determine concept type from intent and commitment level.
2. Determine property from the primary concern of the text.
3. Assign confidence based on how explicit and unambiguous the selected concept type and property are.
4. Provide a short rationale that names evidence for both the concept type and the property.

Concept-type tie-breakers:

| Situation | Classification Guidance |
|---|---|
| Text describes a desired business or product outcome without a concrete system obligation. | Prefer `GOAL`. |
| Text is stakeholder-centered and describes what someone needs, wants, or lacks before a binding system obligation is stated. | Prefer `NEED`. |
| Text assigns an obligation to the product, system, service, component, or team and can be verified. | Prefer `REQUIREMENT`. |
| Text is scenario-shaped, such as a user-story sentence, but `SCENARIO` is not a v1 output. | Classify by intent and commitment level using `GOAL`, `NEED`, or `REQUIREMENT`. |

Property tie-breakers:

| Situation | Classification Guidance |
|---|---|
| Text primarily describes behavior, capability, workflow, operation, or interaction. | Prefer `FUNCTIONAL`. |
| Text primarily describes performance, security, availability, usability, reliability, compliance, scalability, or another quality attribute. | Prefer `QUALITY`. |
| Text includes both a behavior and a quality constraint. | Prefer `QUALITY` when the quality constraint is the distinguishing obligation; otherwise prefer `FUNCTIONAL`. |

Confidence calibration:

| Confidence Range | Meaning |
|---|---|
| `0.90` to `1.00` | Explicit concept and property signals with little ambiguity. |
| `0.70` to `0.89` | Likely classification with mostly clear signals but some indirect wording. |
| `0.50` to `0.69` | Mixed or weak signals; rationale should name the ambiguity. |
| Below `0.50` | Forced best-fit classification because v1 has no `UNKNOWN`; downstream consumers should treat it as human-review material. |

## Architecture Decisions

- Add a new application port for classification so the domain and use case do not depend on LLM, Jackson, Ollama, or JSON details.
- Use an app-owned JSON Schema in `src/main/resources/contracts/ai/v1`, matching the syntax extraction pattern.
- Keep structured-output DTOs in `classification.infrastructure`; map DTOs to domain values only after validation.
- Require concept type, property, confidence score, and rationale. Invalid or unsupported model output is an adapter failure, not a domain value.
- Use one overall confidence score from `0.0` to `1.0`.
- Include classifier signals, tie-breakers, minimal-pair examples, and confidence calibration in the prompt. Signals are evidence cues, not keyword-only rules.

## Dependency Graph

```text
Glossary and workflow docs
    -> Domain classification values and evidence values
        -> Structured-output schema and DTOs
            -> LLM-backed classification adapter
                -> ClassifyRequirementCommandHandler
                    -> REST command dispatch integration test
```

## Task List

### Task 1: Add Domain Classification Values

**Description:** Add the domain model for `RequirementClassification`, the closed enum sets for concept type and property, and value objects for confidence score and rationale.

**Acceptance criteria:**
- [ ] `RequirementClassification` requires a non-null `RequirementConceptType`.
- [ ] `RequirementClassification` requires a non-null `RequirementProperty`.
- [ ] `RequirementClassification` requires a valid `ConfidenceScore`.
- [ ] `RequirementClassification` requires a non-blank `ClassificationRationale`.
- [ ] V1 concept types are exactly `GOAL`, `NEED`, and `REQUIREMENT`.
- [ ] V1 properties are exactly `FUNCTIONAL` and `QUALITY`.
- [ ] `ConfidenceScore` accepts values from `0.0` to `1.0` inclusive and rejects values outside that range.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RequirementClassification*'`

**Dependencies:** Documentation terms recorded.

**Files likely touched:**
- `src/main/java/io/fekav/req/classification/domain/RequirementClassification.java`
- `src/main/java/io/fekav/req/classification/domain/RequirementConceptType.java`
- `src/main/java/io/fekav/req/classification/domain/RequirementProperty.java`
- `src/main/java/io/fekav/req/classification/domain/ConfidenceScore.java`
- `src/main/java/io/fekav/req/classification/domain/ClassificationRationale.java`
- `src/test/java/io/fekav/req/classification/domain/RequirementClassificationTest.java`

**Estimated scope:** Small.

### Task 2: Add Structured-Output Contract and DTO Mapping

**Description:** Add the app-owned model-output schema and DTO binding for classification.

**Acceptance criteria:**
- [ ] JSON Schema requires `classification.conceptType`, `classification.property`, `classification.confidenceScore`, and `classification.rationale`.
- [ ] Schema enum values match the domain enum names exactly.
- [ ] Schema defines `confidenceScore` as a number with minimum `0.0` and maximum `1.0`.
- [ ] Schema defines `rationale` as a string.
- [ ] DTO validation rejects missing required fields.
- [ ] DTO mapping rejects unsupported enum values, invalid confidence scores, and blank rationale as invalid structured output.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RequirementClassificationOutput*'`

**Dependencies:** Task 1.

**Files likely touched:**
- `src/main/resources/contracts/ai/v1/requirement-classification.schema.json`
- `src/main/java/io/fekav/req/classification/infrastructure/RequirementClassificationOutput.java`
- `src/main/java/io/fekav/req/classification/infrastructure/RequirementClassificationFieldsOutput.java`
- `src/test/java/io/fekav/req/classification/infrastructure/RequirementClassificationOutputTest.java`

**Estimated scope:** Medium.

### Task 3: Add LLM Classification Port Adapter

**Description:** Implement the LLM-backed classifier using the same provider-boundary pattern as `LlmRequirementSyntaxExtraction`.

**Acceptance criteria:**
- [ ] Adapter reads `requirement-classification.schema.json` and passes a deep copy as the LLM format.
- [ ] Prompt includes concept-type, property, confidence score, and rationale definitions from this plan.
- [ ] Prompt includes classifier signals and tie-breakers for `GOAL` vs `NEED` vs `REQUIREMENT`.
- [ ] Prompt includes property tie-breakers for `FUNCTIONAL` vs `QUALITY`.
- [ ] Prompt includes minimal-pair examples covering all six concept/property combinations.
- [ ] Prompt explicitly states that `SCENARIO` is not a v1 output.
- [ ] Prompt includes the confidence calibration rubric and instructs the model not to inflate confidence for mixed signals.
- [ ] Prompt requires the rationale to justify both the concept type and property.
- [ ] Adapter parses the Ollama response wrapper, validates DTO output, and returns `RequirementClassification`.
- [ ] Adapter throws existing structured-output exceptions for invalid wrapper JSON, missing model output, malformed model JSON, missing required fields, unsupported enum values, invalid confidence score, and blank rationale.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*LlmRequirementClassification*'`
- [ ] Prompt contract test confirms the generated prompt contains concept definitions, property definitions, tie-breakers, confidence calibration, and the no-`SCENARIO` instruction.

**Dependencies:** Task 2.

**Files likely touched:**
- `src/main/java/io/fekav/req/classification/application/RequirementClassificationService.java`
- `src/main/java/io/fekav/req/classification/infrastructure/LlmRequirementClassificationService.java`
- `src/test/java/io/fekav/req/classification/infrastructure/LlmRequirementClassificationServiceTest.java`
- `src/test/resources/fixtures/requirement-classification-cases.json`

**Estimated scope:** Medium.

### Task 4: Add Classify Requirement Use Case

**Description:** Add a command handler that classifies raw requirement text and returns the domain classification.

**Acceptance criteria:**
- [ ] `ClassifyRequirementCommand` validates non-blank raw text.
- [ ] Handler creates `RawRequirementText`, delegates to `RequirementClassificationService`, applies classification to `Requirement`, publishes a classification event, and returns `RequirementClassification` including confidence score and rationale.
- [ ] Existing `ExtractEntitiesCommand` behavior remains unchanged.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*ClassifyRequirementCommandHandler*'`

**Dependencies:** Task 3.

**Files likely touched:**
- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommand.java`
- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommandHandler.java`
- `src/main/java/io/fekav/req/shared/model/Requirement.java`
- `src/main/java/io/fekav/req/shared/event/RequirementClassifiedEvent.java`
- `src/test/java/io/fekav/req/classification/application/ClassifyRequirementCommandHandlerTest.java`

**Estimated scope:** Medium.

### Checkpoint: Core Classification

- [ ] Domain tests pass.
- [ ] Infrastructure adapter tests pass.
- [ ] Command-handler tests pass.
- [ ] Existing syntax extraction tests still pass.

### Task 5: Add REST Command Dispatch Coverage

**Description:** Add integration coverage proving the existing `/app/c` command endpoint can dispatch the new classification command through the command registry.

**Acceptance criteria:**
- [ ] Posting `ClassifyRequirementCommand` returns a serialized `RequirementClassification`.
- [ ] Response includes concept type, property, confidence score, and rationale.
- [ ] Integration test mocks the classification application port, not the LLM provider.
- [ ] Existing extract-entities REST tests remain unchanged.

**Verification:**
- [ ] Tests pass: `./gradlew test --tests '*RestControllerTestIT*'`

**Dependencies:** Task 4.

**Files likely touched:**
- `src/test/java/io/fekav/platform/api/RestControllerTestIT.java`

**Estimated scope:** Small.

### Task 6: Update Project Documentation

**Description:** Keep implementation docs aligned with the new classifier contract and omit `Scenario` from v1 classification language.

**Acceptance criteria:**
- [ ] `docs/glossary.md` defines `Need`, `RequirementConceptType`, `RequirementProperty`, `ConfidenceScore`, `ClassificationRationale`, `Functional`, and `Quality`.
- [ ] `docs/workflows/requirement-kg-persistence.md` names `Goal`, `Need`, and `Requirement` as v1 classification concept types.
- [ ] `docs/architecture.md` references the classification slice pattern after implementation.

**Verification:**
- [ ] Manual doc review confirms `Scenario` is not listed as a v1 classification output.

**Dependencies:** Tasks 1-5.

**Files likely touched:**
- `docs/glossary.md`
- `docs/workflows/requirement-kg-persistence.md`
- `docs/architecture.md`
- `docs/implementation/requirement-classification-implementation.md`

**Estimated scope:** Small.

### Checkpoint: Complete

- [ ] Focused tests pass: `./gradlew test --tests '*RequirementClassification*' --tests '*LlmRequirementClassification*' --tests '*ClassifyRequirementCommandHandler*'`
- [ ] REST integration tests pass: `./gradlew test --tests '*RestControllerTestIT*'`
- [ ] Full build succeeds: `./gradlew build`
- [ ] Documentation matches the implemented domain vocabulary.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| LLM confuses `NEED` and `REQUIREMENT`. | Medium | Prompt with commitment-level definitions and few-shot examples for both. |
| LLM confuses `FUNCTIONAL` and `QUALITY` for high-level goals. | Medium | Define property as a separate axis and include goal examples for both properties. |
| Classifier signals cause keyword overfitting. | Medium | Present signals as evidence cues, add tie-breakers and counterexamples, and require confidence to drop when signals conflict. |
| Scenario-shaped inputs reintroduce a `SCENARIO` category. | Low | Prompt explicitly states that `SCENARIO` is not a v1 output and scenario-shaped text must be classified by intent and commitment level. |
| Model gives a rationale for only one axis. | Medium | Prompt requires the rationale to explain both concept type and property; prompt contract tests keep that instruction present. |
| Model is forced to classify ambiguous text because v1 has no `UNKNOWN`. | Medium | Keep the best-fit classification, lower confidence for ambiguity, and let downstream consumers route low-confidence results to human review. |
| Model returns unsupported values despite schema guidance. | Medium | DTO mapping rejects unsupported enum values before domain construction. |
| Model returns overconfident or vague evidence. | Medium | Prompt requires a grounded rationale; tests verify score bounds and non-blank rationale, while human review can use low confidence for triage. |
| Prompt examples drift from domain documentation. | Low | Keep the six classification matrix examples as fixture cases and assert the prompt still includes the current definitions and no-`SCENARIO` constraint. |
| Existing text-only validator does not validate enum membership. | Low | Keep required-field validation in `StructuredOutputValidator`; perform enum membership validation in infrastructure mapping. |
| Existing text-only validator does not validate numeric confidence. | Low | Keep text validation in `StructuredOutputValidator`; perform confidence range validation in DTO mapping or extend the validator in a focused task if repetition appears. |
| Adding classification to `Requirement` status changes extraction flow. | Medium | Add a separate command and event; do not change `ExtractEntitiesCommand` result or behavior. |

## Open Questions

None for v1. Reopen only if classification needs an `UNKNOWN` output, separate confidence scores per axis, or multi-label property.

## Commands

```bash
./gradlew test --tests '*RequirementClassification*' --tests '*LlmRequirementClassification*' --tests '*ClassifyRequirementCommandHandler*'
./gradlew test --tests '*RestControllerTestIT*'
./gradlew build
```
