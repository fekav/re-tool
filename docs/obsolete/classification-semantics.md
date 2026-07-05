## Classification Semantics

`CLASSIFY_REQUIREMENT` classifies raw text on two axes: concept type and property. The concept type records intent and commitment level and is represented by `RequirementType` in source code. The property records whether the classified concept is primarily behavioral or quality-related and is represented by `RequirementProperty`. The `Classification` result also records one overall `ConfidenceScore` and a short `Rationale` for review and persistence.

Low confidence is a consumer review and triage signal, not a classifier failure. Actually the workflow still returns the best-fit `GOAL`, `NEED`, or `REQUIREMENT` classification because it does not define an `UNKNOWN` concept type.

| Concept Type | Intent / Commitment Level | Classifier Signal | Example |
|---|---|---|---|
| `GOAL` | Desired outcome or business objective. Explains why change matters. | Outcome language, benefit, target state, strategic result. Usually not directly testable as one system behavior. | "Reduce failed customer onboarding by 30%." |
| `NEED` | Stakeholder need or capability gap. Explains what someone needs before it is expressed as a binding system obligation. | Stakeholder-centered language such as "needs", "wants", "must be able to", or problem statements. Often generates multiple requirements. | "Support agents need visibility into failed payment attempts." |
| `REQUIREMENT` | Binding product or system obligation. Specifies what the system must do or satisfy. | "shall", "must", "is required to", concrete behavior, or measurable constraint. Should be verifiable. | "The billing service must log failed payment attempts with reason codes." |

| Property | Meaning | Classifier Signal | Example |
|---|---|---|---|
| `FUNCTIONAL` | Behavior, capability, workflow, operation, or interaction. | Action or capability language describing something the system, user, or stakeholder can do. | "The dashboard shall export monthly usage metrics." |
| `QUALITY` | Quality attribute or constraint. | Performance, security, availability, usability, reliability, compliance, scalability, or measurable constraint language. | "The dashboard export must complete within 2 seconds." |

| Evidence Field | Meaning | Validation |
|---|---|---|
| `confidenceScore` | Overall classifier certainty in the concept type and property assignment. | Required number from `0.0` to `1.0`. |
| `rationale` | Short explanation grounded in the raw text. | Required non-blank text. |
