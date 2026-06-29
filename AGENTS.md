# AGENTS.md

## Core Rules

- If a task matches a skill, ask for confirmation before you invoke it
- Always follow the skill instructions exactly (do not partially apply them)
- When `test-driven-development` involves writing, modifying, or reviewing Java tests, invoke and follow `java-junit-testing` before changing those tests

### Intent → Skill Mapping

The agent should automatically map user intent to skills:

- Planning / breakdown → `planning-and-task-breakdown`
- Bug / failure / unexpected behavior → `debugging-and-error-recovery`
- Code review → `code-review-and-quality`
- API or interface design → `api-and-interface-design`
