# Implementation Plan: Atomic Slice Receipt Contracts

## Target Interface Direction

```text
ClassifyRequirementCommand(rawText: String)
    -> RequirementClassifiedEvent(
           rawText: RawText,
           classification: Classification
       )

ExtractSyntaxCommand(rawText: String)
    -> SyntaxExtractedEvent(
           rawText: RawText,
           action: Action
       )

RetrieveCandidateConceptsCommand(selectedTerm: SelectedTerm)
    -> ConceptCandidatesRetrievedEvent(
           match: CandidateConceptMatch(
               selectedTerm: SelectedTerm,
               candidates: List<RetrievedCandidateConcept>
           )
       )

EvaluateConceptMatchCommand(match: CandidateConceptMatch)
    -> ConceptMatchEvaluatedEvent(
           match: CandidateConceptMatch,
           decision: ConceptMatchDecision
       )
```

`CandidateConceptMatchSet` and `ConceptMatchDecisionSet` are removed. `CandidateConceptMatch` remains the single-term retrieval result artifact and may contain zero, one, or many `RetrievedCandidateConcept` artifacts. Collection of multiple term receipts belongs to a future workflow orchestrator, not to these slice contracts.

## Task List

### Task 1: Refactor Classification to Return a Receipt

**Goal:** Make `classification` return and publish one named receipt artifact for one raw requirement text.

**Implementation steps:**
- Change `RequirementClassifiedEvent` to contain `rawText: RawText` and `classification: Classification`.
- Remove `requirementId: RequirementId` from `RequirementClassifiedEvent`.
- Change `RequirementClassifiedEvent.create(...)` to accept `RawText` and `Classification`.
- Change `ClassifyRequirementCommandHandler` to return `RequirementClassifiedEvent`.
- In `ClassifyRequirementCommandHandler`, build `RawText`, call `ClassificationService.classifyRequirement(rawText)`, create `RequirementClassifiedEvent`, publish that same event, and return it.
- Stop creating a transient `Requirement` in `ClassifyRequirementCommandHandler`.
- Update `Requirement.applyClassification(...)` to keep publishing `RequirementClassifiedEvent` with `rawText: RawText` and `classification: Classification`.
- Update `RequirementTest` to assert the new `RequirementClassifiedEvent` payload and no `RequirementId`.

**Named artifacts:**
- Input artifact: `ClassifyRequirementCommand`
- Input value artifact: `RawText`
- Domain result artifact: `Classification`
- Receipt output artifact: `RequirementClassifiedEvent`
- Published artifact: `RequirementClassifiedEvent`

**Acceptance criteria:**
- [ ] `ClassifyRequirementCommandHandler.handle(...)` returns `RequirementClassifiedEvent`.
- [ ] Returned `RequirementClassifiedEvent.rawText()` equals the normalized `RawText` from the command.
- [ ] Returned `RequirementClassifiedEvent.classification()` equals the `Classification` from `ClassificationService`.
- [ ] `EventPublisher.publish(...)` receives the same `RequirementClassifiedEvent` instance returned by the handler.
- [ ] `RequirementClassifiedEvent` has no `RequirementId`.

**Verification:**
- [ ] `./gradlew test --tests '*ClassifyRequirementCommandHandlerTest' --tests '*RequirementTest'`

**Files likely touched:**
- `src/main/java/io/fekav/req/classification/application/ClassifyRequirementCommandHandler.java`
- `src/main/java/io/fekav/req/shared/event/RequirementClassifiedEvent.java`
- `src/main/java/io/fekav/req/shared/model/Requirement.java`
- `src/test/java/io/fekav/req/classification/application/ClassifyRequirementCommandHandlerTest.java`
- `src/test/java/io/fekav/req/shared/model/RequirementTest.java`

### Task 2: Refactor Syntax Extraction to Return a Receipt

**Goal:** Make `syntaxextraction` return and publish one named receipt artifact for one raw requirement text.

**Implementation steps:**
- Change `SyntaxExtractedEvent` to contain `rawText: RawText` and `action: Action`.
- Remove `requirementId: RequirementId` from `SyntaxExtractedEvent`.
- Change `SyntaxExtractedEvent.create(...)` to accept `RawText` and `Action`.
- Change `ExtractSyntaxCommandHandler` to return `SyntaxExtractedEvent`.
- In `ExtractSyntaxCommandHandler`, build `RawText`, call `SyntaxExtraction.extractSyntax(rawText)`, create `SyntaxExtractedEvent`, publish that same event, and return it.
- Stop creating a transient `Requirement` in `ExtractSyntaxCommandHandler`.
- Delete `ExtractSyntaxResponse` and `RequirementElementsResponse`.
- Update `Requirement.applyExtraction(...)` to keep publishing `SyntaxExtractedEvent` with `rawText: RawText` and `action: Action`.
- Update `RequirementTest` to assert the new `SyntaxExtractedEvent` payload and no `RequirementId`.

**Named artifacts:**
- Input artifact: `ExtractSyntaxCommand`
- Input value artifact: `RawText`
- Domain result artifact: `Action`
- Receipt output artifact: `SyntaxExtractedEvent`
- Published artifact: `SyntaxExtractedEvent`

**Acceptance criteria:**
- [ ] `ExtractSyntaxCommandHandler.handle(...)` returns `SyntaxExtractedEvent`.
- [ ] Returned `SyntaxExtractedEvent.rawText()` equals the normalized `RawText` from the command.
- [ ] Returned `SyntaxExtractedEvent.action()` equals the `Action` from `SyntaxExtraction`.
- [ ] `EventPublisher.publish(...)` receives the same `SyntaxExtractedEvent` instance returned by the handler.
- [ ] `SyntaxExtractedEvent` has no `RequirementId`.

**Verification:**
- [ ] `./gradlew test --tests '*ExtractEntitiesCommandHandlerTest' --tests '*RequirementTest' --tests '*ExtractSyntaxCommandRestControllerTestIT'`

**Files likely touched:**
- `src/main/java/io/fekav/req/syntaxextraction/application/ExtractSyntaxCommandHandler.java`
- `src/main/java/io/fekav/req/syntaxextraction/application/ExtractSyntaxResponse.java`
- `src/main/java/io/fekav/req/syntaxextraction/application/RequirementElementsResponse.java`
- `src/main/java/io/fekav/req/shared/event/SyntaxExtractedEvent.java`
- `src/main/java/io/fekav/req/shared/model/Requirement.java`
- `src/test/java/io/fekav/req/syntaxextraction/application/ExtractEntitiesCommandHandlerTest.java`
- `src/test/java/io/fekav/req/shared/model/RequirementTest.java`

### Task 3: Refactor Retrieval to One Selected Term

**Goal:** Make `conceptretrieval` retrieve zero, one, or many candidates for exactly one `SelectedTerm` and delete the multi-term set artifact.

**Implementation steps:**
- Change `RetrieveCandidateConceptsCommand` to contain `selectedTerm: SelectedTerm`.
- Change `ConceptRetrievalService.retrieveCandidates(...)` to accept `SelectedTerm` and return `CandidateConceptMatch`.
- Keep `ConceptRetrievalPolicy.retrieveCandidates(selectedTerm: SelectedTerm) -> CandidateConceptMatch`.
- Keep `CandidateConceptMatch.candidates()` as `List<RetrievedCandidateConcept>`.
- Change `RetrieveCandidateConceptsCommandHandler` to return `ConceptCandidatesRetrievedEvent`.
- Rename `ConceptCandidatesReadyEvent` to `ConceptCandidatesRetrievedEvent`.
- Change `ConceptCandidatesRetrievedEvent` to contain `match: CandidateConceptMatch`.
- Publish the same `ConceptCandidatesRetrievedEvent` instance returned by the handler.
- Delete `CandidateConceptMatchSet`.
- Remove all construction, validation, tests, and REST payload usage of `CandidateConceptMatchSet`.

**Named artifacts:**
- Input artifact: `RetrieveCandidateConceptsCommand`
- Input value artifact: `SelectedTerm`
- Domain result artifact: `CandidateConceptMatch`
- Candidate item artifact: `RetrievedCandidateConcept`
- Receipt output artifact: `ConceptCandidatesRetrievedEvent`
- Deleted artifact: `CandidateConceptMatchSet`
- Published artifact: `ConceptCandidatesRetrievedEvent`

**Acceptance criteria:**
- [ ] `RetrieveCandidateConceptsCommand.selectedTerm()` returns one `SelectedTerm`.
- [ ] `ConceptRetrievalService.retrieveCandidates(selectedTerm)` returns `CandidateConceptMatch`.
- [ ] `RetrieveCandidateConceptsCommandHandler.handle(...)` returns `ConceptCandidatesRetrievedEvent`.
- [ ] Returned `ConceptCandidatesRetrievedEvent.match()` contains the input `SelectedTerm`.
- [ ] `CandidateConceptMatch.candidates()` may contain zero, one, or many `RetrievedCandidateConcept` values.
- [ ] Empty `List<RetrievedCandidateConcept>` remains valid on `CandidateConceptMatch`.
- [ ] `CandidateConceptMatchSet` no longer exists in `src/main/java`.
- [ ] No test imports or constructs `CandidateConceptMatchSet`.

**Verification:**
- [ ] `./gradlew test --tests '*RetrieveCandidateConceptsCommandHandlerTest' --tests '*ConceptNameRetrievalPolicyTest' --tests '*ConceptRetrievalServiceTest'`
- [ ] `rg "CandidateConceptMatchSet" src/main/java src/test/java` returns no matches.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommand.java`
- `src/main/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandler.java`
- `src/main/java/io/fekav/req/conceptretrieval/domain/ConceptRetrievalService.java`
- `src/main/java/io/fekav/req/shared/event/ConceptCandidatesReadyEvent.java`
- `src/main/java/io/fekav/req/shared/model/CandidateConceptMatchSet.java`
- `src/test/java/io/fekav/req/shared/model/CandidateConceptMatchSetTest.java`
- `src/test/java/io/fekav/req/conceptretrieval/application/RetrieveCandidateConceptsCommandHandlerTest.java`
- `src/test/java/io/fekav/req/conceptretrieval/domain/ConceptRetrievalServiceTest.java`

### Task 4: Refactor Matching to One Candidate Match

**Goal:** Make `conceptmatching` evaluate exactly one `CandidateConceptMatch` and delete the batch set artifact.

**Implementation steps:**
- Replace `DecideConceptMatchesCommand` with `EvaluateConceptMatchCommand`.
- Make `EvaluateConceptMatchCommand` contain `match: CandidateConceptMatch`.
- Replace `DecideConceptMatchesCommandHandler` with `EvaluateConceptMatchCommandHandler`.
- Change `ConceptMatchingService.decideMatches(...)` to `evaluateMatch(match: CandidateConceptMatch) -> ConceptMatchDecision`.
- Keep `ConceptMatchingPolicy.decide(match: CandidateConceptMatch) -> ConceptMatchDecision`.
- Add `ConceptMatchEvaluatedEvent` containing `match: CandidateConceptMatch` and `decision: ConceptMatchDecision`.
- Publish the same `ConceptMatchEvaluatedEvent` instance returned by the handler.
- Delete `ConceptMatchDecisionSet`.
- Remove all construction, validation, tests, and REST payload usage of `ConceptMatchDecisionSet`.
- Remove status-specific publication from the matching handler: `MapExistingConceptRequestedEvent`, `ExistingConceptProposedEvent`, `ConceptMatchReviewRequestedEvent`, and `CreateConceptRequestedEvent` are not emitted by this handler.

**Named artifacts:**
- Input artifact: `EvaluateConceptMatchCommand`
- Input value artifact: `CandidateConceptMatch`
- Domain result artifact: `ConceptMatchDecision`
- Receipt output artifact: `ConceptMatchEvaluatedEvent`
- Deleted artifact: `ConceptMatchDecisionSet`
- Published artifact: `ConceptMatchEvaluatedEvent`

**Acceptance criteria:**
- [ ] `EvaluateConceptMatchCommand.match()` returns one `CandidateConceptMatch`.
- [ ] `ConceptMatchingService.evaluateMatch(match)` returns `ConceptMatchDecision`.
- [ ] `EvaluateConceptMatchCommandHandler.handle(...)` returns `ConceptMatchEvaluatedEvent`.
- [ ] Returned `ConceptMatchEvaluatedEvent.match()` equals the command `CandidateConceptMatch`.
- [ ] Returned `ConceptMatchEvaluatedEvent.decision()` equals the policy-produced `ConceptMatchDecision`.
- [ ] `ConceptMatchDecision.status` behavior remains unchanged for `AUTO_MAP_EXISTING`, `PROPOSE_EXISTING`, `REVIEW_REQUIRED`, and `AUTO_CREATE_NEW`.
- [ ] `ConceptMatchDecisionSet` no longer exists in `src/main/java`.
- [ ] No test imports or constructs `ConceptMatchDecisionSet`.

**Verification:**
- [ ] `./gradlew test --tests '*EvaluateConceptMatchCommandHandlerTest' --tests '*ThresholdConceptMatchingPolicyTest' --tests '*ConceptMatchingServiceTest'`
- [ ] `rg "ConceptMatchDecisionSet" src/main/java src/test/java` returns no matches.

**Files likely touched:**
- `src/main/java/io/fekav/req/conceptmatching/application/DecideConceptMatchesCommand.java`
- `src/main/java/io/fekav/req/conceptmatching/application/DecideConceptMatchesCommandHandler.java`
- `src/main/java/io/fekav/req/conceptmatching/application/EvaluateConceptMatchCommand.java`
- `src/main/java/io/fekav/req/conceptmatching/application/EvaluateConceptMatchCommandHandler.java`
- `src/main/java/io/fekav/req/conceptmatching/domain/ConceptMatchingService.java`
- `src/main/java/io/fekav/req/conceptmatching/domain/ConceptMatchDecisionSet.java`
- `src/main/java/io/fekav/req/shared/event/ConceptMatchEvaluatedEvent.java`
- `src/test/java/io/fekav/req/conceptmatching/application/DecideConceptMatchesCommandHandlerTest.java`
- `src/test/java/io/fekav/req/conceptmatching/domain/ConceptMatchingServiceTest.java`
- `src/test/java/io/fekav/req/conceptmatching/domain/ConceptMatchDecisionSetTest.java`

### Task 5: Align REST Command Integration Tests

**Goal:** Make the generic REST command endpoint serialize the new receipt artifacts for each command.

**Implementation steps:**
- Update classification REST tests to expect serialized `RequirementClassifiedEvent`.
- Update syntax extraction REST tests to expect serialized `SyntaxExtractedEvent`.
- Update retrieval REST tests to send one `SelectedTerm` and expect serialized `ConceptCandidatesRetrievedEvent`.
- Update matching REST tests to send one `CandidateConceptMatch` and expect serialized `ConceptMatchEvaluatedEvent`.
- Replace REST test references to `DecideConceptMatchesCommand` with `EvaluateConceptMatchCommand`.
- Remove REST test expectations for `CandidateConceptMatchSet` and `ConceptMatchDecisionSet`.

**Named artifacts:**
- Input artifact: `CommandRequest`
- Classification output artifact: JSON serialization of `RequirementClassifiedEvent`
- Syntax output artifact: JSON serialization of `SyntaxExtractedEvent`
- Retrieval output artifact: JSON serialization of `ConceptCandidatesRetrievedEvent`
- Matching output artifact: JSON serialization of `ConceptMatchEvaluatedEvent`

**Acceptance criteria:**
- [ ] `CommandRequest(command = "ClassifyRequirementCommand")` returns serialized `RequirementClassifiedEvent`.
- [ ] `CommandRequest(command = "ExtractSyntaxCommand")` returns serialized `SyntaxExtractedEvent`.
- [ ] `CommandRequest(command = "RetrieveCandidateConceptsCommand")` returns serialized `ConceptCandidatesRetrievedEvent`.
- [ ] `CommandRequest(command = "EvaluateConceptMatchCommand")` returns serialized `ConceptMatchEvaluatedEvent`.
- [ ] No REST test expects `CandidateConceptMatchSet`.
- [ ] No REST test expects `ConceptMatchDecisionSet`.

**Verification:**
- [ ] `./gradlew test --tests '*RestControllerTest' --tests '*CommandRestControllerTestIT'`

**Files likely touched:**
- `src/test/java/io/fekav/platform/api/ClassifyRequirementCommandRestControllerTestIT.java`
- `src/test/java/io/fekav/platform/api/ExtractSyntaxCommandRestControllerTestIT.java`
- `src/test/java/io/fekav/platform/api/RetrieveCandidateConceptsCommandRestControllerTestIT.java`
- `src/test/java/io/fekav/platform/api/DecideConceptMatchesCommandRestControllerTestIT.java`
- `src/test/java/io/fekav/platform/api/RestControllerCommandTestSupport.java`

### Task 6: Remove Obsolete Set References and Run Full Verification

**Goal:** Finish the refactor by removing obsolete set references from production and test code, then verify the whole codebase.

**Implementation steps:**
- Run `rg "CandidateConceptMatchSet|ConceptMatchDecisionSet|DecideConceptMatchesCommand|DecideConceptMatchesCommandHandler|ConceptCandidatesReadyEvent" src`.
- Delete every remaining obsolete production reference found by that search.
- Use only the final target artifact names listed in this task for replacement code.
- Update tests so remaining names match the target artifacts.
- Run focused tests for all modified slices.
- Run the full test suite.

**Named artifacts:**
- Removed artifact: `CandidateConceptMatchSet`
- Removed artifact: `ConceptMatchDecisionSet`
- Removed artifact: `DecideConceptMatchesCommand`
- Removed artifact: `DecideConceptMatchesCommandHandler`
- Removed artifact: `ConceptCandidatesReadyEvent`
- Final command artifact: `EvaluateConceptMatchCommand`
- Final command handler artifact: `EvaluateConceptMatchCommandHandler`
- Final retrieval receipt artifact: `ConceptCandidatesRetrievedEvent`
- Final matching receipt artifact: `ConceptMatchEvaluatedEvent`

**Acceptance criteria:**
- [ ] `rg "CandidateConceptMatchSet|ConceptMatchDecisionSet" src/main/java src/test/java` returns no matches.
- [ ] `rg "DecideConceptMatchesCommand|DecideConceptMatchesCommandHandler" src/main/java src/test/java` returns no matches.
- [ ] `rg "ConceptCandidatesReadyEvent" src/main/java src/test/java` returns no matches.
- [ ] `./gradlew test` passes.
- [ ] No KG persistence, workflow orchestration, or documentation updates are included in this refactoring.

**Verification:**
- [ ] `./gradlew test`
