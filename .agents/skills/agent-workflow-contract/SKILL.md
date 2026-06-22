---
name: agent-workflow-contract
description: Creates workflow contracts as state-machine tables. Use this when a workflow must be expressed as an agent-executable contract, especially from incomplete or informal process descriptions. The contract should make states, transitions, artifacts, human decisions, assumptions, failure paths, and abort conditions explicit.
---

# Agent Workflow Contract


Creates explicit workflow contracts from source information so agents can execute workflows based on defined inputs, outputs, responsibilities, and constraints rather than hidden assumptions.

## How it works

1. **Create & Deliver** transform raw workflow idea into structured workflow contract.
2. **Ask & Clarify:** clarify open questions, determine missing parts, stop-and-ask human.

## Usage

This skill primarily outputs workflow documentations and asks the human to fill in the gaps in the workflow documentation.
Invoke it with an idea, and the agent will guide you through the process.

**Trigger Phrases:**
- "Create a workflow for this feature"
- "Is this [workflow] clear enough for an agent?"
- "What is missing in this [workflow]?" 

## Contract Design Rules

- Prefer state-machine structure over prose. Agents need explicit states and transitions more than narrative explanation.
- Keep the contract compact and human-readable. Markdown tables are the artifact; do not introduce YAML, JSON, a formal DSL, or generated diagrams unless the user asks.
- Write allowed actions as specific verbs an agent may perform. If an action is not listed, the consuming agent should treat it as out of scope.
- Make all required inputs concrete. Name documents, code paths, commands, approvals, credentials, data sources, or human decisions where possible.
- Name human decision points explicitly. Avoid vague phrases like "confirm with the team" unless the role, trigger, and expected response are defined.
- Prefer aborting over guessing. Every risky or ambiguous step should have an `Abort If` condition.
- Separate assumptions from open questions. Assumptions are tentative beliefs; open questions are unresolved decisions or missing facts.
- Do not mark an assumption as true unless the source material states it or the user confirms it.
- name every domain term introduced in the workflow contract explicitly with a brief description
- start and terminal states are the workflows boundaries, everything outside the workflows boundaries are out of scope.

## Output: Contract Format

The final output is a markdown one-pager saved to `docs/workflows/[workflow-name].md` (after user confirmation), containing

```markdown
# [Workflow Name] Workflow Contract

## Purpose
[One or two sentences describing what the workflow enables agents to execute.]

## Scope

### In Scope
- [What this workflow governs]

### Non-goals
- [What this workflow does not attempt to do]

## State Table

| State | Goal | Required Inputs | Allowed Actions | Outputs | Abort If |
|---|---|---|---|---|---|
| [STATE_NAME] [START/TERMINAL or nothing] | [Goal of this state] | [Inputs needed before action] | [Allowed actions only] | [Durable outputs] | [Stop condition] |

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

| Assumption | Source | Risk Level | Impact If Wrong | How To Validate |
|---|---|---|---|---|
| [Tentative assumption] | [User input, file, or inference] | [HIGH/MEDIUM/LOW] | [What breaks if wrong] | [How a human or agent can validate] |

```
## Output: Glossary

If not exists, save domain terms in `docs/glossary.md`, containing:

| Term | Type | Aliases | Used In | Description |
|---|---|---|---|---|
| [Domain term] | [STATE/CONDITION/ARTIFACT] | [Known synonyms, or "—"] | [Workflow documentation section(s) using this term] | [brief description] |

### Matching Policy

Before adding a new glossary entry, check the candidate term against existing entries:

Never resolve a naming collision (same term, different meaning across workflows) by silently picking one meaning — surface it as an Open Question instead.

Keep the glossary always in sync with every workflow document that references it.


## Phase 1: Writing State Tables

- State names should be stable identifiers such as `INTAKE`, `VALIDATE_INPUTS`, `EXECUTE_CHANGE`, `VERIFY_RESULT`, `REQUEST_HUMAN_DECISION`, `DONE`, and `ABORTED`. 
- Use names that fit the workflow, but keep them short and unambiguous.

For each state:

- `Goal`: define the single outcome of the state.
- `Required Inputs`: list only inputs that must exist before the agent can act in that state. Make all required inputs concrete. Name documents, or human decisions where possible.
- `Allowed Actions`: define what the agent may do. Keep this operational, not aspirational. 
- `Outputs`: name durable artifacts, decisions, or status changes produced by the state.
- `Transition`: state the condition that moves execution forward.
- `Abort If`: name the condition that forces the agent to stop or ask a human.

If a state depends on missing information, do not invent the information. Insert `MISSING` in the corresponding field and put the missing fact in `Open Questions`, include plausible answers, and add an `Abort If` condition to the dependent state.


## Phase 2: Writing Transitions

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

## Phase 3: Handling Missing Information

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

## Phase 4: Completion Review Pass

When user ask for completing, or making an existing workflow executable, explicitly check for these gaps:

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

Then stop and interview the user for these gaps like below:

### Ask one question at a time, each with a guess attached

Format:

```
Q: <one focused question>
GUESS: <your hypothesis for the answer, with the reasoning that produced it>
```

Wait for the user to react before asking the next question.

**Why one at a time, not a batch:**

- The user can't react to your hypotheses if you bury them in a list
- Batches encourage skim-reading and surface answers
- The third question often depends on the answer to the first; asking them all at once locks in the wrong framing
- The user's energy for thinking carefully is finite; spend it one question at a time

**Why attach a guess:**

- The user reacts faster to a wrong guess than they generate an answer from scratch
- It commits you to a hypothesis you can be visibly wrong about, which keeps you honest
- It surfaces *your* assumptions, which is what the interview is meant to expose

The risk here is a polite user agreeing with your guess to be agreeable. Mitigate by being visibly willing to be wrong, and occasionally guess in a direction you expect the user to push back on.

## Phase 5: Closing the Loop on Soft Sections

`Open Questions`, `Abort Conditions`, and the `Assumption Log` are narrative by nature — they exist to hold uncertainty that hasn't been resolved into structure yet. But narrative that never resolves into the State Table or Transition Table is invisible to a consuming agent: it reads like prose hiding inside a section header, which is exactly what this contract format exists to prevent. Each rule below names the concrete structural change that
must exist before a soft-section entry can be considered closed.

### Open Questions → Abort If

**Trigger:** An `Open Questions` entry names a state, transition, or artifact in its `Blocks` column. 
**Required target:** The named state gets an explicit `Abort If` condition that reflects the unresolved question.
**If not closed:** The dependent state is incomplete. Do not let the question stand on its own as the only record of the risk.

### Abort Conditions → Transition Table

**Trigger:** An entry in the global `Abort Conditions` list is not scoped to a single state — it applies regardless of where execution currently is.
**Required target:** A row in the Transition Table of the form `ANY_STATE → ABORTED` (or `ANY_STATE → REQUEST_HUMAN_DECISION` if human clarification is needed first).
**If not closed:** The condition has no trigger point a consuming agent can act on. Either give it a Transition Table row, or rewrite it as a per-state
`Abort If` if it's actually state-specific.

### Assumption Log → Abort If or Allowed Actions

**Trigger:** An `Assumption Log` entry carries non-trivial `Risk`. 
**Required target:** Either (a) a validation step added to the dependent state's `Allowed Actions` that checks the assumption before relying on it,
or (b) an `Abort If` condition that fires if the assumption later proves false.
**If not closed:** The assumption is not actually tentative — it's load-bearing and unverified. If neither (a) nor (b) is possible, it isn't
safe to assume; move it to `Open Questions` instead.

No soft-section entry is finished once it's written down. It's finished once it has produced — or explicitly failed to produce — one of these structural counterparts.

## Response Format

After creating or updating the contract, reply briefly:

```text
Created/updated docs/workflows/[workflow-name].md.

Blocking open questions: [count and short summary, or "none"]
High-risk assumptions: [count and short summary, or "none"]
```

## Red Flags

- Making assumptions and decisions without documenting them.
- Introducing new domain terms without saving them in the glossary.
- Any `Open Questions`, `Abort Conditions`, or `Assumption Log` entry that does not close the loop per Phase 5

## Verification

Before finishing, check that:

- The file is under `docs/workflows/*.md` unless the user requested another path.
- The contract uses the required section order and table formats.
- Every state has a transition and an abort condition.
- Every transition has a concrete condition.
- Human decision points are explicit.
- Open questions and assumptions are separated.
- No missing information was silently applied as fact.
- `glossary.md` is in sync with workflow document.
- Every `Open Questions`, `Abort Conditions`, and `Assumption Log` entry satisfies its Phase 5 closing rule.