---
name: agent-workflow-contract
description: Creates or reviews agent-executable workflow contracts as compact state-machine tables. Use this whenever a user asks for a workflow, process, handoff, runbook, agent instructions, completion criteria, or whether a workflow is clear enough for an agent, especially when the source is informal, incomplete, cross-role, or risky. The skill turns ambiguity into explicit states, transitions, artifacts, human decisions, assumptions, open questions, abort paths, glossary terms, and verification checks.
---

# Agent Workflow Contract

Use this skill to turn workflow source material into a contract another agent can execute without relying on hidden assumptions.

## Core Principles

A workflow contract is not a prose explanation. It is an executable agreement: clear boundaries, explicit state transitions, concrete artifacts, named human decisions, and visible failure paths. Prefer durable structure over narrative because future agents will follow the tables more reliably than paragraphs.

- Start from the user's source material. Use referenced files, existing workflow docs, glossary entries, and repository conventions before inventing structure.
- Keep the working context small. Read only sources needed to identify the workflow boundary, states, artifacts, and decision points.
- Preserve provenance. When a fact comes from a file, user statement, or inference, keep that source visible in the contract or assumption log.
- Convert uncertainty into structure. Use `Open Questions`, `Assumption Log`, `Abort If`, and `Human Decision Points` instead of burying ambiguity in prose.
- Ask before writing only when the workflow itself or the target file cannot be identified. Otherwise produce the contract and mark unresolved facts explicitly.
- Do not silently widen scope. Start and terminal states define the workflow boundary; everything outside them belongs in `Non-goals` or a separate workflow.
- Do not introduce YAML, JSON, diagrams, or a formal DSL unless the user explicitly asks. Markdown tables are the contract artifact.

## Operating Modes

Choose the mode that matches the user's request:

- **Create:** Turn an idea, notes, chat transcript, or process description into a new workflow contract.
- **Review:** Inspect an existing workflow contract for gaps, unsafe assumptions, weak transitions, or missing artifacts. Return findings unless the user asks for edits.
- **Update:** Modify an existing workflow contract and keep the glossary synchronized.
- **Make executable:** Strengthen a draft workflow until another agent could execute it with clear stop conditions.

## Default Artifacts

- Save workflow contracts to `docs/workflows/[workflow-name].md` unless the user requests another path.
- Save or update domain terms in `docs/glossary.md`.
- If the user only asks for a review, do not write files unless they ask for changes.

## Contract Template

Use this section order for every workflow contract.

```markdown
# [Workflow Name] Workflow Contract

## Purpose
[One or two sentences describing what this workflow enables an agent to execute.]

## Scope

### In Scope
- [What this workflow governs]

### Non-goals
- [What this workflow deliberately excludes]

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Abort If |
|---|---|---|---|---|---|
| [STATE_NAME] (START/TERMINAL if applicable) | [Single outcome for this state] | [Concrete inputs needed before action] | [Specific allowed verbs only] | [Durable outputs, decisions, or status changes] | [Concrete stop condition] |

## Transition Table

| From | Condition | To | Human Needed? | Notes |
|---|---|---|---|---|
| [STATE_NAME] | [Concrete condition that can be checked] | [NEXT_STATE] | [No / Yes: role and required response] | [Clarifying note or "-"] |

## Required Artifacts

| Artifact | Produced By | Required By | Format/Location | Validation |
|---|---|---|---|---|
| [Artifact name] | [State or actor] | [State or actor] | [Expected path or format] | [How to verify it exists and is usable] |

## Human Decision Points

| Decision | Trigger | Owner/Role | Options | Required Response |
|---|---|---|---|---|
| [Decision name] | [When this decision is needed] | [Human role] | [Allowed options] | [Exact response shape the agent needs] |

## Abort Conditions
- [Global condition that requires the consuming agent to stop]
- [Global condition that requires human clarification before continuing]

## Open Questions

| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| [Targeted question] | [Why agents cannot safely proceed without this] | [Options a human can confirm, reject, or refine] | [State, transition, or artifact affected] |

## Assumption Log

| Assumption | Source | Risk Level | Impact If Wrong | How To Validate |
|---|---|---|---|---|
| [Tentative assumption] | [User input, file path, or inference] | [HIGH/MEDIUM/LOW] | [What breaks if wrong] | [Validation action or owner] |
| [Tentative assumption] | [User input, file path, or inference] | [Medium/MEDIUM/LOW] | [What breaks if wrong] | [Validation action or owner] |
```

## Glossary Template

If `docs/glossary.md` does not exist, create it with this table. If it exists, update it without duplicating terms.

```markdown
| Term | Type | Aliases | Used In | Description |
|---|---|---|---|---|
| [Domain term] | [STATE/CONDITION/ARTIFACT/ROLE/DECISION] | [Known synonyms, or "-"] | [Workflow section or file] | [Brief operational meaning] |
```

Before adding a term, compare it with existing entries by meaning, not only exact spelling. Never resolve a naming collision by silently choosing one meaning; add an open question instead. Keep every workflow document and glossary entry consistent after edits.

## State Rules

- Use stable uppercase identifiers such as `INTAKE`, `VALIDATE_INPUTS`, `EXECUTE_CHANGE`, `VERIFY_RESULT`, `REQUEST_HUMAN_DECISION`, `DONE`, and `ABORTED`.
- Mark exactly one start state with `(START)`.
- Mark terminal states with `(TERMINAL)`. `DONE` and `ABORTED` are common terminal states.
- Give each state one goal. If a row has multiple goals, split it into multiple states.
- List only required inputs that must exist before the agent acts in that state.
- Write allowed actions as concrete verbs the agent may perform. If an action is not listed, it is out of scope for the consuming agent.
- Name durable outputs. Avoid vague outputs such as "updated status" unless the location or status value is defined.
- Add an `Abort If` condition for every non-terminal state. The condition should tell the consuming agent when to stop or request human input.
- If a field depends on missing information, write `MISSING`, add an open question, and add a matching `Abort If` condition.

## Transition Rules

Every non-terminal state needs at least one outgoing transition. Every transition needs a condition that another agent can evaluate without guessing.

Good transition conditions:

- `All required artifacts exist and pass validation.`
- `Human owner selects one option from Human Decision Points.`
- `./gradlew test` exits successfully.
- `No blocking open questions affect the next state.`

Rewrite weak conditions:

- `When done`
- `After approval`
- `If it looks good`
- `Once the agent understands the request`

Use `ANY_STATE -> ABORTED` for global failures. Use `ANY_STATE -> REQUEST_HUMAN_DECISION` when execution can continue only after clarification.

## Handling Missing Information

Ask the user before drafting only when the missing information prevents identifying the workflow or edit target. Examples:

- The user says "make a workflow contract" without naming or describing the workflow.
- The user asks to update an existing workflow but does not provide a path and multiple likely files exist.
- The requested edit would overwrite important existing content and the intended target is ambiguous.

Otherwise, draft the contract and make uncertainty explicit. Targeted questions should include plausible assumptions:

```markdown
| Question | Why It Matters | Plausible Assumptions | Blocks |
|---|---|---|---|
| Who is allowed to approve production deployment? | Agents cannot safely transition from `READY_FOR_DEPLOY` to `DEPLOY` without an owner. | Engineering manager, release captain, product owner | `DEPLOY` transition |
```

## Closing The Loop

Soft sections are only useful when they affect executable structure. Before finishing, connect each soft-section entry to a table row or explain why it cannot be closed yet.

| Source Entry | Required Structural Counterpart |
|---|---|
| `Open Questions` row with a `Blocks` value | Matching `Abort If` condition in the blocked state or transition |
| Global `Abort Conditions` entry | `ANY_STATE -> ABORTED` or `ANY_STATE -> REQUEST_HUMAN_DECISION` transition |
| Medium/high-risk `Assumption Log` row | Validation action in `Allowed Actions` or an `Abort If` condition |
| Medium/Medium-risk `Assumption Log` row | Validation action in `Allowed Actions` or an `Abort If` condition |
| Human approval or choice | `Human Decision Points` row plus transition condition using the required response |
| New domain term | `docs/glossary.md` entry using the same meaning |

If a soft-section entry cannot be connected to structure, keep it visible and mark the affected state, transition, or artifact as blocked.

## Make Workflow Contract executable

When the user asks to complete a workflow contract documentation or make it executable, check:

- for open questions
- medium and high risk assumptions
- human decisions

and resolve them by interviewing the user:

Ask one question at a time:

```text
Q: <one focused question>
GUESS: <your hypothesis for the answer, with the reasoning that produced it>
```

Wait for the user to respond before asking the next question. A guess makes assumptions visible and easier to correct.

## Review-Only Output

When reviewing an existing workflow without editing it, lead with findings:

```text
Findings:
- [Severity] [Section/table]: [issue and why it matters]

Open questions:
- [Question that blocks executability]

Assumptions

Recommended edits:
- [Concrete change]
```

Focus on bugs in executability: hidden assumptions, missing transitions, unsafe actions, ambiguous approvals, missing artifacts, and unresolved glossary terms.

## Final Response

After creating or updating files, reply briefly:

```text
Created/updated docs/workflows/[workflow-name].md.
Glossary: [updated docs/glossary.md / no changes needed]

Blocking open questions: [count and short summary, or "none"]
High-risk assumptions: [count and short summary, or "none"]
Medium-risk assumptions: [count and short summary, or "none"]
```

If no files were changed because the user requested review only, summarize the review result instead.

## Verification Checklist

Before finishing, verify that:

- The contract is saved under `docs/workflows/*.md` unless the user requested another path.
- The required section order and table formats are present.
- Exactly one start state exists.
- Every terminal state is marked `(TERMINAL)`.
- Every non-terminal state has at least one outgoing transition and one `Abort If` condition.
- Every transition has a concrete condition.
- Human decision points are explicit and referenced by transitions.
- Open questions and assumptions are separated.
- No missing information was silently applied as fact.
- `docs/glossary.md` is in sync with all new or changed domain terms.
- Every `Open Questions`, `Abort Conditions`, and `Assumption Log` entry satisfies the closing rules or clearly blocks a named structure.
- Missing start, terminal, or abort states
- Non-terminal states without outgoing transitions
- States without concrete required inputs
- Broad or unsafe allowed actions
- Outputs that are not durable artifacts, decisions, or status changes
- Transitions without concrete conditions
- Human approvals without owner, options, or response format
- Assumptions mixed into facts
- Risky steps without abort conditions
- Domain terms missing from the glossary
- Scope creep hidden in prose
