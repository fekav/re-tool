---
name: agent-workflow-contract
description: Creates or completes agent-consumable workflow contracts in docs/workflows/*.md using strict Markdown state-machine tables. Use this skill whenever the user mentions workflows, SOPs, runbooks, playbooks, process contracts, agent handoffs, autonomous agent execution, or asks to turn an incomplete process into something agents can reliably execute, especially when states, transitions, artifacts, human decisions, assumptions, or abort conditions matter.
---

# Agent Workflow Contract

Create compact Markdown workflow contracts that agents can execute without relying on hidden assumptions.

This skill does not produce attractive process documentation, narrative SOPs, or diagrams. It produces an execution contract: what state the agent is in, what inputs are required, what actions are allowed, what artifacts are produced, how transitions happen, when a human must decide, and when the agent must stop.

## Core Rule

Do not silently fill gaps.

When required information is missing, make the missing information visible in the generated workflow contract. Add a targeted human question, list plausible assumptions the human can confirm or reject, and add an abort condition wherever an agent would otherwise be forced to guess.

## Modes

Use one of two modes based on the user's request.

### Create Mode

Use create mode when the user gives a raw workflow idea, process description, goal, team practice, or agent handoff that does not already exist as a workflow contract.

Your job is to turn that idea into a complete contract under:

```text
docs/workflows/[workflow-name].md
```

If the workflow name is not provided, derive a short kebab-case name from the process. If the process itself is too vague to identify, ask one targeted question before writing.

### Completion Mode

Use completion mode when the user gives an existing workflow file or partial process and asks to complete, harden, review, make agent-ready, or turn it into a contract.

Read the existing material first. Preserve useful domain language and existing decisions, but restructure it into the contract format below. Identify missing states, ambiguous transitions, undefined actors, unsafe assumptions, missing artifacts, missing human decisions, and missing abort conditions.

## Workflow

1. Determine whether the task is create mode or completion mode.
2. Read any files the user names. If completing an existing workflow, inspect the current content before editing.
3. Identify the workflow's purpose, consuming agents, boundaries, states, transitions, artifacts, human decisions, abort conditions, open questions, and assumptions.
4. Write or update the Markdown contract
5. Finish with a short summary naming the file and calling out any blocking open questions or high-risk assumptions.

## Contract Design Rules

- Prefer state-machine structure over prose. Agents need explicit states and transitions more than narrative explanation.
- Keep the contract compact and human-readable. Markdown tables are the artifact; do not introduce YAML, JSON, a formal DSL, or generated diagrams unless the user asks.
- Write allowed actions as specific verbs an agent may perform. If an action is not listed, the consuming agent should treat it as out of scope.
- Make all required inputs concrete. Name documents, code paths, commands, approvals, credentials, data sources, or human decisions where possible.
- Name human decision points explicitly. Avoid vague phrases like "confirm with the team" unless the role, trigger, and expected response are defined.
- Prefer aborting over guessing. Every risky or ambiguous step should have an `Abort If` condition.
- Separate assumptions from open questions. Assumptions are tentative beliefs; open questions are unresolved decisions or missing facts.
- Do not mark an assumption as true unless the source material states it or the user confirms it.

## Required Contract Format

Use this structure for every generated workflow contract.

```markdown
# [Workflow Name] Workflow Contract

## Purpose
[One or two sentences describing what the workflow enables agents to execute.]

## Intended Consuming Agents
- [Agent type or role]
- [Agent type or role]

## Scope

### In Scope
- [What this workflow governs]

### Non-goals
- [What this workflow does not attempt to do]

## Global Execution Rules
- [Rule that applies in every state]
- [Rule that applies in every state]

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Transition | Abort If |
|---|---|---|---|---|---|---|
| [STATE_NAME] | [Goal of this state] | [Inputs needed before action] | [Allowed actions only] | [Durable outputs] | [Condition and next state] | [Stop condition] |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| [STATE_NAME] | [Explicit condition] | [NEXT_STATE] | [Yes/No, role if yes] | [Clarifying note] |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| [Artifact name] | [State or actor] | [State or actor] | [Expected path/format] | [How to verify it exists or is usable] |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| [Decision name] | [When this decision is needed] | [Human role] | [Allowed options] | [What the agent needs back] |

## Abort Conditions
- [Condition that requires the consuming agent to stop]
- [Condition that requires human clarification before proceeding]

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| [Targeted question] | [Why agents cannot safely proceed without this] | [Options the human can confirm, reject, or refine] | [State, transition, or artifact affected] |

## Assumption Log

| Assumption | Source | Risk | How To Validate |
|---|---|---|---|
| [Tentative assumption] | [User input, file, or inference] | [What breaks if wrong] | [How a human or agent can validate] |

## Validation Checklist
- [ ] Every state has a clear goal.
- [ ] Every state defines required inputs.
- [ ] Every state defines allowed actions.
- [ ] Every state defines outputs.
- [ ] Every transition has an explicit condition.
- [ ] Every ambiguous or risky step has an abort condition.
- [ ] Human decision points are named explicitly.
- [ ] Open questions are separated from assumptions.
- [ ] Assumptions are visible and never silently applied.
```

## Writing State Tables

State names should be stable identifiers such as `INTAKE`, `VALIDATE_INPUTS`, `EXECUTE_CHANGE`, `VERIFY_RESULT`, `REQUEST_HUMAN_DECISION`, `DONE`, and `ABORTED`. Use names that fit the workflow, but keep them short and unambiguous.

For each state:

- `Goal`: define the single outcome of the state.
- `Required Inputs`: list only inputs that must exist before the agent can act in that state.
- `Allowed Actions`: define what the agent may do. Keep this operational, not aspirational.
- `Outputs`: name durable artifacts, decisions, or status changes produced by the state.
- `Transition`: state the condition that moves execution forward.
- `Abort If`: name the condition that forces the agent to stop or ask a human.

If a state depends on missing information, do not invent the information. Put the missing fact in `Open Questions`, include plausible answers, and add an `Abort If` condition to the dependent state.

## Writing Transitions

Every transition needs an explicit condition. Avoid vague transitions like "when ready", "after review", or "if successful" unless the contract defines what ready, reviewed, or successful means.

Good transition conditions:

- "All required artifacts exist and pass validation."
- "Human owner selects one option from `Human Decision Points`."
- "`./gradlew test` exits successfully."
- "No blocking open questions affect the next state."

Weak transition conditions to rewrite:

- "When done"
- "After approval"
- "If it looks good"
- "Once the agent understands the request"

## Handling Missing Information

Ask the user before writing only when the missing information prevents identifying the workflow at all. Examples:

- The user says "make a workflow contract" without naming or describing the workflow.
- The user asks to update an existing workflow but does not provide a path and multiple likely files exist.
- The requested edit would overwrite important existing content and the intended target is ambiguous.

Otherwise, write the contract and make the uncertainty explicit inside it. Use `Open Questions`, `Assumption Log`, and `Abort If` fields to prevent consuming agents from treating guesses as facts.

Targeted questions should include plausible assumptions:

```markdown
| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Who is allowed to approve production deployment? | Agents cannot safely transition from `READY_FOR_DEPLOY` to `DEPLOY` without an owner. | Engineering manager, release captain, product owner | `DEPLOY` transition |
```

## Completion Review Pass

When completing an existing workflow, explicitly check for these gaps:

- Missing initial, terminal, or abort states
- States without required inputs
- States with broad or unsafe allowed actions
- Outputs that are not durable artifacts
- Transitions without concrete conditions
- Human approvals without named owner or response format
- Assumptions mixed into facts
- Open questions with no blocked state or artifact
- Risky steps without abort conditions
- Scope creep hidden in prose

Fix the structure directly when the source material supports it. When the source material does not support a fix, record the gap as an open question or assumption instead of inventing an answer.

## Response Format

After creating or updating the contract, reply briefly:

```text
Created/updated docs/workflows/[workflow-name].md.

Blocking open questions: [count and short summary, or "none"]
High-risk assumptions: [count and short summary, or "none"]
```

Do not run evaluations or build a workflow execution engine as part of this skill. The deliverable is the Markdown contract.

## Verification

Before finishing, check that:

- The file is under `docs/workflows/*.md` unless the user requested another path.
- The contract uses the required section order and table formats.
- Every state has a transition and an abort condition.
- Every transition has a concrete condition.
- Human decision points are explicit.
- Open questions and assumptions are separated.
- No missing information was silently applied as fact.
