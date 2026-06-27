# Spec: Requirement Syntax Domain Model Change

## Objective

Change the internal requirement syntax domain model from a map of syntax roles to
text into a semantic composition. `Requirement` remains the aggregate root and
owns one `Action`. `Action` owns one `Subject`, one `TargetObject`, its own
action text, and zero or more `Condition` and `Constraint` value objects.

The extraction prompt, structured-output schema, and external API response shape
remain compatible with the current five extracted fields:

- `SUBJECT`
- `ACTION`
- `OBJECT`
- `CONDITION`
- `CONSTRAINT`

## Motivation

The current `RequirementSyntax` model is a map. This makes the five extracted
fields look independent even though their meaning depends on the complete
requirement context. An action text without its subject and target object is not
a useful standalone domain concept.

The new model treats the extraction as a triple-like semantic structure:

```text
Subject -> Action -> TargetObject
```

Conditions and constraints qualify the action.

## Decisions

- Keep the current prompt and extraction contract field names.
- Preserve the public API response shape with an application response DTO.
- Do not keep the old `RequirementSyntax` map as the domain model just to
  preserve JSON compatibility.
- Introduce `ElementId` for domain entities only.
- Do not replace `EventId` with `ElementId`.
- Do not add IDs to `Condition` or `Constraint`.

## Target Model

```text
Requirement
  id: ElementId
  rawText: String
  status: RequirementStatus
  action: Action

Action
  id: ElementId
  text: String
  subject: Subject
  targetObject: TargetObject
  conditions: Set<Condition>
  constraints: Set<Constraint>

Subject
  id: ElementId
  text: String

TargetObject
  id: ElementId
  text: String

Condition
  text: String

Constraint
  text: String
```

## Boundary Model

The infrastructure boundary still receives the current structured-output DTO:

```json
{
  "syntaxElements": {
    "SUBJECT": "reporting dashboard",
    "ACTION": "shall export",
    "OBJECT": "monthly usage metrics",
    "CONSTRAINT": "as CSV",
    "CONDITION": ""
  }
}
```

After validation, the mapper builds the domain model:

```text
SUBJECT    -> Subject
ACTION     -> Action.actionText
OBJECT     -> TargetObject
CONDITION  -> Set<Condition>
CONSTRAINT -> Set<Constraint>
```

The command/API response remains compatible with the current shape:

```json
{
  "syntaxElements": {
    "SUBJECT": "reporting dashboard",
    "ACTION": "shall export",
    "OBJECT": "monthly usage metrics",
    "CONSTRAINT": "as CSV",
    "CONDITION": ""
  }
}
```

## Non-Goals

- Do not change the LLM provider adapter.
- Do not change the JSON schema field names.
- Do not change the REST command request shape.
- Do not introduce persistence or database migrations.
- Do not model multiple extracted conditions or constraints in the prompt yet.
- Do not replace platform messaging IDs.

## Implementation Tasks

### Task 1: Add `ElementId`

Add a UUID-backed `ElementId` implemented like the existing ID records.

Acceptance criteria:

- `ElementId.create()` creates a UUID-backed ID.
- `EventId` remains unchanged.
- `Condition` and `Constraint` do not receive IDs.

Verification:

```bash
./gradlew test --tests '*ElementId*'
```

### Task 2: Add Domain Element Types

Add `Subject`, `TargetObject`, `Condition`, and `Constraint`.

Acceptance criteria:

- `Subject` has an `ElementId` and non-blank text.
- `TargetObject` has an `ElementId` and non-blank text.
- `Condition` is a value object with non-blank text.
- `Constraint` is a value object with non-blank text.
- Text is stripped consistently with existing domain value-object style.

Verification:

```bash
./gradlew test --tests '*Subject*' --tests '*TargetObject*' --tests '*Condition*' --tests '*Constraint*'
```

### Task 3: Add `Action`

Add `Action` as the semantic owner of subject, target object, action text,
conditions, and constraints.

Acceptance criteria:

- `Action` has an `ElementId`.
- `Action` requires non-blank `actionText`.
- `Action` requires one `Subject`.
- `Action` requires one `TargetObject`.
- `conditions` and `constraints` are sets.

Verification:

```bash
./gradlew test --tests '*Action*'
```

### Task 4: Update `Requirement` Extraction Ownership

Change `Requirement` so extracted syntax is stored as an `Action`.

Acceptance criteria:

- `Requirement.applyExtraction(Action)` rejects null.
- Applying extraction sets status to `EXTRACTED`.
- Applying extraction publishes the existing extraction domain event.
- Classification behavior is unchanged.

Verification:

```bash
./gradlew test --tests '*Requirement*' --tests '*ExtractEntitiesCommandHandler*'
```

### Task 5: Add API-Compatible Extraction Response

Add an application response DTO that preserves the current serialized response
shape while the internal model uses `Action`.

Acceptance criteria:

- Command result serializes with `syntaxElements.SUBJECT`.
- Command result serializes with `syntaxElements.ACTION`.
- Command result serializes with `syntaxElements.OBJECT`.
- Command result serializes with `syntaxElements.CONDITION`.
- Command result serializes with `syntaxElements.CONSTRAINT`.
- Missing optional values serialize consistently with current API expectations.

Verification:

```bash
./gradlew test --tests '*ExtractSyntaxCommandHandler*' --tests '*RestControllerTestIT*'
```

### Task 6: Update Infrastructure Mapping

Keep the current LLM output DTO and schema, but map validated DTO output into
the new domain model.

Acceptance criteria:

- `SUBJECT` maps to `Subject`.
- `ACTION` maps to `Action.actionText`.
- `OBJECT` maps to `TargetObject`.
- Blank `CONDITION` maps to an empty condition set.
- Non-blank `CONDITION` maps to a one-element condition set.
- Blank `CONSTRAINT` maps to an empty constraint set.
- Non-blank `CONSTRAINT` maps to a one-element constraint set.
- Prompt and schema field names are unchanged.

Verification:

```bash
./gradlew test --tests '*LlmRequirementSyntaxExtraction*'
```

### Task 7: Remove Old Map Domain Usage

Remove production dependency on the old syntax map and enum after all callers
use `Action` or the API-compatible response DTO.

Acceptance criteria:

- Production domain/application code no longer depends on `RequirementSyntaxType`.
- `RequirementSyntax` is removed.
- Documentation no longer describes the domain as a map and has no references to old `RequirementSyntax`

Verification:

```bash
rg "RequirementSyntax|RequirementSyntaxType|syntaxElements\\(" src/main/java
./gradlew test
```

## Checkpoints

After Tasks 1-3:

- Domain model tests pass.
- No prompt, schema, or infrastructure provider behavior has changed.

After Tasks 4-6:

- Extraction use case works with the new domain model.
- API response remains compatible.
- Structured-output schema remains compatible.

After Task 7:

- The old map model is gone from production domain/application code.
- Full test suite passes, except for any explicitly documented environmental
  blockers such as missing Docker/Testcontainers support.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| API response changes accidentally because commands return domain objects directly. | High | Return an application response DTO instead of the domain `Action`. |
| Command-name drift hides real integration failures. | Medium | Resolve `ExtractEntitiesCommand` vs. `ExtractSyntax` before final verification. |
| Value-object set deduplication behaves unexpectedly. | Medium | Use record-based value objects and add duplicate tests. |
| Infrastructure changes expand beyond mapping. | Medium | Keep prompt/schema fields unchanged and limit infrastructure edits to DTO-to-domain conversion. |

## Final Verification

```bash
./gradlew test
```

Also verify manually:

- The prompt still asks for `SUBJECT`, `ACTION`, `OBJECT`, `CONDITION`, and
  `CONSTRAINT`.
- The JSON schema still exposes those fields.
- The REST response still exposes `syntaxElements` with those fields.
- `EventId` is unchanged.
- `Condition` and `Constraint` have no IDs.
