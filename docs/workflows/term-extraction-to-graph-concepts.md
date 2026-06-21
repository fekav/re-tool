# Term Extraction to Graph Concepts Workflow Contract

## Purpose
Enable agents to implement a Java command-bus feature that accepts one text payload, extracts typed terms through an existing LLM port, maps each term to existing graph concepts through an existing matching policy, and emits a minimal workflow-state event. This contract is intended for no-human implementation: unresolved prerequisites cause aborts, not clarification requests.

## Intended Consuming Agents
- Feature implementation agent
- Test-writing or review agent

## Scope

### In Scope
- Discover existing project structure, domain vocabulary, command-bus patterns, graph concept model, LLM port, matching policy, event publisher, and test conventions.
- Design the command, handler, domain flow, typed term extraction through the existing LLM port, policy-driven concept matching, and workflow-state event.
- Implement Java application/domain code required for command handling, validation, term extraction orchestration, graph concept mapping, and event emission.
- Add or update JUnit tests and run `./gradlew test`.

### Non-goals
- Asking a human for implementation decisions during execution.
- Production deployment, release management, or runtime operations.
- Creating a new command bus, graph concept read port, graph concept repository, matching policy, LLM provider integration, model configuration, credentials, prompt infrastructure, graph schema, persistence contract, table, index, migration, or graph write.
- Building UI screens unless an existing required interface already depends on this feature.

## Global Execution Rules
- Follow `/workspace/AGENTS.md`: use `domain-driven-design-coder` for coding, use `java-junit-testing` for tests, and execute tests with `./gradlew test`.
- Prefer existing project patterns, packages, repositories, services, command handlers, ports, events, and tests over new abstractions.
- Preserve user or unrelated work in the repository; do not revert changes outside the feature scope.
- Do not ask humans for decisions while executing this workflow; abort with a specific reason when a required existing surface or rule is missing.
- Use the existing primary application command bus. If multiple command buses exist, choose the one nearest to the relevant bounded context or nearest existing command/handler package. Do not create a new command-bus surface.
- Text enters through a corresponding command processed by a command handler registered with the selected command bus.
- Reject blank text and text longer than `10_000` characters through command validation.
- Extracted terms are categorized as `subject`, `object`, `action`, `condition`, or `constraint`.
- Term extraction uses an existing LLM port only. Provider configuration, credentials, model choice, prompt infrastructure, direct LLM integration, network setup, and model downloads are out of scope.
- Graph concepts are read only through an existing graph/concept repository or read port. Do not create a new graph read port or write graph data.
- Matching, unmatched handling, and ambiguity handling use an existing matching policy only. Do not create matching rules, thresholds, or a new matching policy.
- The command handler emits a new domain/application workflow-state event after policy evaluation.
- The workflow-state event payload contains only `commandId` and the original `text`.
- If multiple existing graph read paths, matching policies, LLM ports, or event publishers exist, choose the one nearest by bounded context, package proximity, or existing feature naming. Abort if proximity does not identify one clear choice.

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Transition | Abort If |
|---|---|---|---|---|---|---|
| `INTAKE` | Capture the requested feature and execution constraints. | User request; `/workspace/AGENTS.md`; repository access. | Read instructions; name the feature; create or update this workflow contract. | Workflow contract at `docs/workflows/term-extraction-to-graph-concepts.md`. | If the feature objective and repo root are known, go to `DISCOVER_PROJECT_CONTEXT`. | Abort if the requested feature cannot be identified. |
| `DISCOVER_PROJECT_CONTEXT` | Find existing code and tests required for no-human implementation. | Workflow contract; workspace files. | Run `rg --files`; search for command, handler, bus, term, concept, graph, node, extraction, LLM port, repository, read port, matching policy, event, publisher, service, and test names; inspect build files and tests. | Context notes listing relevant files, modules, APIs, command-bus registration, LLM port, graph concept read path, matching policy, event publisher, and test locations. | If discovery records each required surface or records it missing, go to `VALIDATE_FIXED_RULES`. | Abort if repository files needed for discovery cannot be read. |
| `VALIDATE_FIXED_RULES` | Verify that all no-human prerequisites exist and are uniquely selectable. | Context notes; existing domain docs or glossary if present; Global Execution Rules. | Verify selected command bus and handler package; verify existing LLM port; verify existing graph concept read path; verify existing matching policy; verify event publication pattern; verify Gradle wrapper; classify any missing or ambiguous prerequisite. | Validation notes naming selected existing surfaces and any abort reason. | If all required existing surfaces are present and uniquely selectable, go to `DESIGN_DOMAIN_FLOW`; otherwise go to `ABORTED`. | Abort if command bus, LLM port, graph read path, matching policy, event publisher, or test/build entry point is absent or ambiguous. |
| `DESIGN_DOMAIN_FLOW` | Specify feature behavior before changing code. | Validation notes; selected code paths; no abort reason. | Use `domain-driven-design-coder`; choose names and package locations from local conventions; identify command, handler, command-bus registration, LLM-port invocation, graph/concept read call, matching-policy call, minimal event, value objects, errors, and validation behavior. | Design notes describing command handling, `10_000` character validation, typed term extraction through LLM port, concept matching through existing policy, minimal event semantics, affected code paths, and test scenarios. | If design names affected code paths and test scenarios without adding out-of-scope infrastructure, go to `IMPLEMENT_TERM_EXTRACTION`. | Abort if design requires creating a new command bus, LLM integration, graph read port, matching policy, graph schema, persistence change, or event infrastructure. |
| `IMPLEMENT_TERM_EXTRACTION` | Add or update code that validates input and extracts typed candidate terms through the existing LLM port. | Design notes; source code paths; selected LLM port; command text contract; term categories: `subject`, `object`, `action`, `condition`, `constraint`. | Modify Java code; add value objects/services following existing patterns; reject blank text and text longer than `10_000` characters; invoke the existing LLM port; map LLM-port output to typed terms; handle empty, repeated, punctuation-heavy, and case-varied text according to design. | Source changes for validation and typed term extraction orchestration. | If valid payloads can produce typed terms through the existing LLM port and invalid payloads are rejected, go to `IMPLEMENT_CONCEPT_MAPPING`. | Abort if implementation requires a new LLM port, provider configuration, credentials, model selection, prompt infrastructure, direct network call, or model download. |
| `IMPLEMENT_CONCEPT_MAPPING` | Map typed extracted terms to graph concepts using existing graph read path and matching policy. | Extraction code; selected graph concept read path; selected matching policy; matching policy contract. | Modify Java code; load graph concepts through the selected existing read path; invoke the selected matching policy; persist no graph concepts; represent policy outcomes internally as needed for control flow. | Source changes for policy-driven term-to-concept mapping behavior. | If the selected matching policy produces a mapping outcome for every typed term, go to `INTEGRATE_FEATURE_SURFACE`. | Abort if graph node identity, graph concept source, or matching policy contract is missing, ambiguous, or requires new rules. |
| `INTEGRATE_FEATURE_SURFACE` | Connect the feature to the command bus and emit the minimal workflow-state event. | Mapping implementation; selected command bus; command handler pattern; selected event publication pattern; design notes. | Define or update the command; implement and register the command handler; invoke validation, extraction, mapping, and policy evaluation; emit the new domain/application event with only `commandId` and original `text`; add minimal configuration if local command-bus registration requires it. | Integrated command handler; command-bus registration; minimal workflow-state event public contract. | If command-bus dispatch invokes the handler and the handler emits the designed event after policy evaluation, go to `WRITE_TESTS`. | Abort if integration requires a new UI, public API, command bus, database migration, credential, graph write, or event infrastructure change. |
| `WRITE_TESTS` | Cover command handling, validation, LLM-port extraction orchestration, policy-driven mapping, event emission, and command-bus integration behavior. | Implemented code; test paths; design notes; AGENTS.md. | Use `java-junit-testing`; add focused JUnit 5 tests; prefer existing fixtures/builders; stub or fake existing ports/policies; cover valid text, blank text rejection, `10_000` character boundary, no terms, repeated terms, typed terms, unmatched policy outcome, ambiguous policy outcome, event timing, minimal event payload, and integration path. | Unit and integration tests under the existing test tree. | If tests are written for all designed behavior, go to `VERIFY_RESULT`. | Abort if required fixtures or graph test data cannot be created without production data access or out-of-scope infrastructure. |
| `VERIFY_RESULT` | Prove the implementation works in the repository. | Code changes; tests; Gradle wrapper. | Run `./gradlew test`; inspect failures; fix feature-scoped failures; rerun tests after fixes; summarize command result. | Passing test result or failure summary with exact blocker. | If `./gradlew test` exits successfully, go to `DONE`; if failures are unrelated or require out-of-scope changes, go to `ABORTED`. | Abort if tests cannot run due to missing dependencies, environment restrictions, or unrelated broken build after one focused retry. |
| `DONE` | Deliver the implemented and verified feature status. | Passing `./gradlew test`; implementation notes; changed files list. | Summarize changes; identify any non-blocking assumptions; cite verification command. | Final implementation summary. | Terminal state. | Abort if final summary would omit failed verification, abort reasons, or unresolved assumptions. |
| `ABORTED` | Stop execution when no-human implementation is unsafe or impossible. | Abort reason from any state. | Record the blocking condition; preserve partial work; report the exact missing or ambiguous prerequisite. | Abort summary with blocked state and required external fix. | Terminal state. | Abort if new work is requested before the blocking condition is resolved. |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| `INTAKE` | Feature objective is "extract typed terms from text and map terms to graph concepts" and repo root is `/workspace`. | `DISCOVER_PROJECT_CONTEXT` | No | This contract is the initial durable artifact. |
| `DISCOVER_PROJECT_CONTEXT` | Context notes record command-bus, LLM-port, graph-read, matching-policy, event-publisher, source, and test findings, including missing findings. | `VALIDATE_FIXED_RULES` | No | Discovery must produce enough detail for validation or abort. |
| `VALIDATE_FIXED_RULES` | Existing command bus, LLM port, graph read path, matching policy, event publisher, and build/test entry point are present and uniquely selectable. | `DESIGN_DOMAIN_FLOW` | No | Selection uses bounded-context, package proximity, and local naming. |
| `VALIDATE_FIXED_RULES` | Any required existing surface is missing or ambiguous. | `ABORTED` | No | No human question is asked during this workflow. |
| `DESIGN_DOMAIN_FLOW` | Design identifies command, handler, command-bus registration, LLM-port call, repository/read path, matching-policy call, minimal workflow-state event, validation behavior, affected code paths, and test scenarios. | `IMPLEMENT_TERM_EXTRACTION` | No | Use DDD skill before Java changes. |
| `DESIGN_DOMAIN_FLOW` | Design would require out-of-scope infrastructure or semantic rules. | `ABORTED` | No | Examples: new graph port, new matching policy, new LLM integration, or event infrastructure. |
| `IMPLEMENT_TERM_EXTRACTION` | Validation and typed candidate term extraction through the existing LLM port are implemented behind the designed API. | `IMPLEMENT_CONCEPT_MAPPING` | No | Extraction does not access graph data. |
| `IMPLEMENT_TERM_EXTRACTION` | Extraction requires new LLM/provider/prompt infrastructure. | `ABORTED` | No | Existing LLM port only. |
| `IMPLEMENT_CONCEPT_MAPPING` | Existing matching policy produces mapping outcomes for all typed terms. | `INTEGRATE_FEATURE_SURFACE` | No | Mapping must not silently drop terms or bypass the policy. |
| `IMPLEMENT_CONCEPT_MAPPING` | Mapping requires new graph read path, graph writes, new matching policy, or invented matching rules. | `ABORTED` | No | Existing graph read path and existing policy only. |
| `INTEGRATE_FEATURE_SURFACE` | Command-bus dispatch invokes the handler and the handler emits the minimal event after policy evaluation. | `WRITE_TESTS` | No | Event payload is only `commandId` and original `text`. |
| `WRITE_TESTS` | Tests cover validation, command handling, LLM-port extraction, policy-driven mapping outcomes, event emission, and relevant edge cases. | `VERIFY_RESULT` | No | Use JUnit 5 conventions in the project. |
| `VERIFY_RESULT` | `./gradlew test` exits successfully. | `DONE` | No | Include command result in final summary. |
| `VERIFY_RESULT` | Build or test failure is unrelated, environmental, or requires out-of-scope changes. | `ABORTED` | No | Preserve partial work and report exact blocker. |
| Any non-terminal state | State-specific abort condition occurs. | `ABORTED` | No | Stop and report the exact blocker. |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| Workflow contract | `INTAKE` | All states | `docs/workflows/term-extraction-to-graph-concepts.md` | File exists and contains required sections. |
| Context notes | `DISCOVER_PROJECT_CONTEXT` | `VALIDATE_FIXED_RULES`, `DESIGN_DOMAIN_FLOW` | Chat summary, issue comment, or `docs/workflows/term-extraction-to-graph-concepts.context.md` | Lists relevant source files, tests, command-bus entry points, LLM port, graph concept model/read path, matching policy, event publisher, or records missing/ambiguous findings. |
| Validation notes | `VALIDATE_FIXED_RULES` | `DESIGN_DOMAIN_FLOW`, `ABORTED` | Chat summary, issue comment, spec update, or implementation notes | Names selected existing surfaces or exact abort reason. |
| Design notes | `DESIGN_DOMAIN_FLOW` | Implementation and tests | Existing spec location or implementation notes | Defines command contract, validation, typed terms, LLM-port call, concept read path, matching-policy call, minimal workflow-state event, affected code paths, and test scenarios. |
| Command and handler | `INTEGRATE_FEATURE_SURFACE` | `WRITE_TESTS`, `VERIFY_RESULT` | Existing application command/handler package | Command bus can dispatch the command to the handler in tests or existing integration path. |
| LLM port usage | `IMPLEMENT_TERM_EXTRACTION` | `WRITE_TESTS`, `VERIFY_RESULT` | Existing application/domain port package | Extraction code calls an existing port and tests use a fake/stub without provider credentials or network access. |
| Graph concept read usage | `IMPLEMENT_CONCEPT_MAPPING` | `WRITE_TESTS`, `VERIFY_RESULT` | Existing graph/concept repository or read-port package | Mapping code reads concepts through the existing path and performs no graph writes or migrations. |
| Matching policy usage | `IMPLEMENT_CONCEPT_MAPPING` | `WRITE_TESTS`, `VERIFY_RESULT` | Existing domain/application policy package | Handler delegates matching, unmatched handling, and ambiguity handling to the existing policy. |
| Workflow-state event | `INTEGRATE_FEATURE_SURFACE` | `WRITE_TESTS`, `VERIFY_RESULT`, downstream consumers | Existing event package following project conventions | Event is emitted after policy evaluation and contains only `commandId` and original `text`. |
| Feature code changes | Implementation states | `WRITE_TESTS`, `VERIFY_RESULT` | Existing Java source tree | Code compiles under project build and follows local patterns. |
| Test changes | `WRITE_TESTS` | `VERIFY_RESULT` | Existing Java test tree | Tests cover command handling, validation, extraction via LLM port, policy-driven mapping, unmatched, ambiguous, event timing, minimal event payload, and integration behavior. |
| Verification result | `VERIFY_RESULT` | `DONE` | Final response or implementation notes | Includes `./gradlew test` exit status and relevant failure summary if not passing. |
| Abort summary | `ABORTED` | Requesting user or future implementer | Final response or implementation notes | Names blocked state, missing/ambiguous prerequisite, and external fix required before retry. |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| None during no-human implementation | Any missing or ambiguous command bus, LLM port, graph read path, matching policy, event publisher, input rule, or event payload field. | Not applicable during this workflow | Abort only | No response is requested; the agent records the abort reason and stops. |

## Abort Conditions
- The feature objective is no longer typed term extraction plus graph concept mapping.
- No existing command bus is found, or multiple command buses cannot be resolved by bounded-context/package proximity.
- No existing LLM port is found for term extraction, or using it would require provider configuration, credentials, model selection, prompt infrastructure, direct network integration, or model downloads.
- No existing graph/concept read path is found, or multiple read paths cannot be resolved by bounded-context/package proximity.
- No existing matching policy is found, or multiple policies cannot be resolved by bounded-context/package proximity.
- Event publishing cannot be implemented through an existing event pattern.
- A state requires creating a new command bus, graph read port, matching policy, LLM integration, graph schema, persistence change, credential, network setup, or event infrastructure.
- A state requires the command handler to hard-code matching, unmatched handling, or ambiguity handling instead of delegating to the existing matching policy.
- The agent cannot read required repository files or locate the build/test entry points.
- `./gradlew test` cannot run or fails because of unrelated project issues after one focused retry.

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| None. | All previously blocking implementation choices are fixed by this contract. | The agent proceeds only when required existing surfaces are present and uniquely selectable; otherwise it aborts. | None. |

## Assumption Log

| Assumption | Source | Risk | How To Validate |
|---|---|---|---|
| The repository is a Java Gradle project. | `/workspace/AGENTS.md` says to run `./gradlew test` and use Java testing skills. | Workflow may point to wrong build and test commands if the project is not Java/Gradle. | Verify `./gradlew`, `build.gradle`, or `settings.gradle` exist during discovery. |
| Domain-driven design conventions apply to feature code. | `/workspace/AGENTS.md` says to use `domain-driven-design-coder` for coding. | Agent may place logic in the wrong layer if the project is not organized around DDD. | Inspect package structure, domain docs, and existing services before editing. |
| The feature entry point is a command handler invoked through an existing command bus. | User confirmed the command-bus rule. | Implementation aborts if no existing command bus can be selected. | Discovery must identify the command-bus pattern and handler registration mechanism. |
| Extracted terms are typed as `subject`, `object`, `action`, `condition`, and `constraint`. | User clarification: "<subject, object, action, condition, constraint>". | Tests and event payloads may encode the wrong categories if category meanings differ from project vocabulary. | Confirm category semantics from existing domain docs or code during discovery; abort only if implementation cannot proceed safely. |
| Term extraction uses an existing LLM port. | User clarification: "it uses a LLM via port. but this is out of scope for this workflow". | Implementation aborts if no existing LLM port exists or if provider integration work is required. | Discovery must identify the port and tests must fake/stub it. |
| Graph concepts are read only from an existing graph/concept repository or read port. | User confirmed "use only existing one". | Implementation aborts if no existing graph read path exists. | Discovery must identify the concept read path and node identity model. |
| Matching, unmatched handling, and ambiguity handling belong to an existing matching policy. | User confirmed only existing matching policy should be used. | Implementation aborts if no existing matching policy exists or if policy choice is ambiguous. | Discovery/design must locate and call the policy following project patterns. |
| The output is a new domain/application workflow-state event emitted after policy evaluation. | User confirmed event timing. | Downstream consumers receive only minimal event data. | Tests must assert emission happens after policy evaluation. |
| Workflow-state event payload contains only `commandId` and original `text`. | User clarified "only commandid and original text". | Event may not expose terms or mapping outcomes for downstream consumers. | Tests must assert no additional payload fields are required by this workflow. |
| Command handling is synchronous for one moderate text payload, with a fixed validation limit. | User accepted the validation rule. | Inputs above `10_000` characters are rejected even if the system could handle more. | Tests must cover blank input and the `10_000` character boundary. |

## Validation Checklist
- [x] Every state has a clear goal.
- [x] Every state defines required inputs.
- [x] Every state defines allowed actions.
- [x] Every state defines outputs.
- [x] Every transition has an explicit condition.
- [x] Every ambiguous or risky step has an abort condition.
- [x] Human decision points are named explicitly.
- [x] Open questions are separated from assumptions.
- [x] Assumptions are visible and never silently applied.
