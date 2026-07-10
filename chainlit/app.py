import json
import os
from dataclasses import dataclass
from typing import Any

import chainlit as cl
import httpx


QUARKUS_BASE_URL = os.getenv("QUARKUS_BASE_URL", "http://localhost:8080")
QUARKUS_TIMEOUT_SECONDS = 360
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "granite4.1:8b")

SYSTEM_MESSAGE = {
    "role": "system",
    "content": (
        "You are a concise requirements-review assistant. Use tools when the "
        "user asks to ingest/record/intake a new requirement, asks about pending node-match "
        "reviews, or wants to find/search/list for existing requirements with given concept name argument. Do not invent anything, "
        "just summarize the tool response. Do not counter with questions."
    ),
}


@dataclass(frozen=True)
class DirectCommandResult:
    content: str
    ingestion_result: dict[str, Any] | None = None


TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "ingest_requirement",
            "description": "Record and process a new requirement from its original text.",
            "parameters": {
                "type": "object",
                "properties": {
                    "originalText": {
                        "type": "string",
                        "description": "The exact original requirement text to ingest.",
                    },
                },
                "required": ["originalText"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_pending_node_match_reviews",
            "description": "List open node-match reviews waiting for a human decision.",
            "parameters": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "find_requirements",
            "description": "Find or search for existing requirements, filtered by a concept name.",
            "parameters": {
                "type": "object",
                "properties": {
                    "concept": {
                        "type": "string",
                        "description": "The concept to filter the requirements by.",
                    },
                },
                "required": ["concept"],
                "additionalProperties": False,
            },
        },
    },
]


@cl.on_chat_start
async def on_chat_start() -> None:
    welcome_message = build_welcome_message()
    cl.user_session.set(
        "messages",
        [
            SYSTEM_MESSAGE,
            {"role": "assistant", "content": welcome_message},
        ],
    )
    await cl.Message(content=welcome_message).send()


def build_welcome_message() -> str:
    tool_lines = "\n".join(format_tool(tool) for tool in TOOLS)
    return (
        "Hello. I currently know these tools:\n\n"
        f"{tool_lines}\n\n"
        "Commands for using Quarkus REST API directly (similar to HTTP request, without using LLM for process prompt/response):\n"
        "- `/ingest <Requirement-Text>`\n"
        "- `/reviews`\n"
        "- `/find [concept]`\n\n"
    )


def format_tool(tool: dict[str, Any]) -> str:
    function = tool.get("function") or {}
    name = function.get("name") or "unknown_tool"
    description = function.get("description") or "No description available."
    return f"- `{name}`: {description}"


@cl.on_message
async def on_message(message: cl.Message) -> None:
    messages = cl.user_session.get("messages") or [SYSTEM_MESSAGE]
    messages.append({"role": "user", "content": message.content})

    async with httpx.AsyncClient(timeout=60.0) as client:
        direct_response = await execute_direct_command(client, message.content)
        if direct_response is not None:
            messages.append({"role": "assistant", "content": direct_response.content})
            cl.user_session.set("messages", messages)
            await cl.Message(content=direct_response.content).send()
            await prompt_reviews_after_ingestion(
                client,
                direct_response.ingestion_result,
            )
            return

        first_response = await ollama_chat(client, messages, TOOLS)
        assistant_message = first_response.get("message", {})
        tool_calls = assistant_message.get("tool_calls") or []

        if not tool_calls:
            content = assistant_message.get("content") or "No response."
            messages.append({"role": "assistant", "content": content})
            cl.user_session.set("messages", messages)
            await cl.Message(content=content).send()
            return

        tool_call = tool_calls[0]
        tool_result = await execute_tool(client, tool_call)
        assistant_message = {
            **assistant_message,
            "tool_calls": [tool_call],
        }
        messages.append(assistant_message)
        messages.append(tool_message(tool_call, tool_result))

        final_response = await ollama_chat(client, messages, None)
        final_message = final_response.get("message", {})
        content = final_message.get("content") or json.dumps(tool_result, indent=2)
        messages.append({"role": "assistant", "content": content})
        cl.user_session.set("messages", messages)
        await cl.Message(content=content).send()
        if tool_call_name(tool_call) == "ingest_requirement":
            await prompt_reviews_after_ingestion(client, tool_result)


async def ollama_chat(
    client: httpx.AsyncClient,
    messages: list[dict[str, Any]],
    tools: list[dict[str, Any]] | None,
) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "model": OLLAMA_MODEL,
        "messages": messages,
        "stream": False,
    }
    if tools is not None:
        payload["tools"] = tools

    response = await client.post(f"{OLLAMA_BASE_URL}/api/chat", json=payload)
    response.raise_for_status()
    return response.json()


async def execute_tool(
    client: httpx.AsyncClient,
    tool_call: dict[str, Any],
) -> dict[str, Any]:
    function = tool_call.get("function") or {}
    name = function.get("name")
    arguments = parse_arguments(function.get("arguments"))

    if name == "ingest_requirement":
        return await post_quarkus(
            client,
            "/app/c",
            {
                "command": "IngestRequirementCommand",
                "payload": arguments,
            },
        )

    if name == "list_pending_node_match_reviews":
        return await post_quarkus(
            client,
            "/app/q",
            {
                "query": "ListPendingNodeMatchReviewsQuery",
                "payload": {},
            },
        )

    if name == "find_requirements":
        return await post_quarkus(
            client,
            "/app/q",
            {
                "query": "FindRequirementsQuery",
                "payload": arguments,
            },
        )

    return {"error": f"Unknown tool: {name}"}


async def execute_direct_command(
    client: httpx.AsyncClient,
    content: str,
) -> DirectCommandResult | None:
    stripped_content = content.strip()
    lower_content = stripped_content.lower()

    if lower_content.startswith("ingest:"):
        return await ingest_requirement(
            client,
            stripped_content.partition(":")[2].strip(),
        )

    if lower_content.startswith("requirement:"):
        return await ingest_requirement(
            client,
            stripped_content.partition(":")[2].strip(),
        )

    if lower_content == "reviews":
        return DirectCommandResult(await list_pending_reviews(client))

    if lower_content.startswith("find:"):
        return DirectCommandResult(
            await find_requirements(client, stripped_content.partition(":")[2].strip())
        )

    if not stripped_content.startswith("/"):
        return None

    command, _, rest = stripped_content.partition(" ")
    command = command.lower()
    rest = rest.strip()

    if command == "/ingest":
        return await ingest_requirement(client, rest)

    if command == "/reviews":
        return DirectCommandResult(await list_pending_reviews(client))

    if command == "/find":
        return DirectCommandResult(await find_requirements(client, rest))

    return None


async def ingest_requirement(
    client: httpx.AsyncClient,
    original_text: str,
) -> DirectCommandResult:
    if not original_text:
        return DirectCommandResult("Please provide the requirement text.")
    result = await post_quarkus(
        client,
        "/app/c",
        {
            "command": "IngestRequirementCommand",
            "payload": {"originalText": original_text},
        },
    )
    return DirectCommandResult(
        format_result("Requirement Ingestion", result),
        result,
    )


async def list_pending_reviews(client: httpx.AsyncClient) -> str:
    result = await list_pending_reviews_result(client)
    return format_result("Open Reviews", result)


async def find_requirements(client: httpx.AsyncClient, concept: str) -> str:
    if not concept:
        return DirectCommandResult("Please provide a search concept.")

    result = await post_quarkus(
        client,
        "/app/q",
        {
            "query": "FindRequirementsQuery",
            "payload": {"concept": concept},
        },
    )
    return format_result("Found Requirements", result)


async def prompt_reviews_after_ingestion(
    client: httpx.AsyncClient,
    ingestion_result: dict[str, Any] | None,
) -> None:
    if not is_review_required(ingestion_result):
        return

    pending_reviews = await list_pending_reviews_result(client)
    if "error" in pending_reviews:
        await cl.Message(
            content=format_result("Could not load pending reviews", pending_reviews)
        ).send()
        return

    correlation_id = correlation_id_value(ingestion_result.get("correlationId"))
    matching_reviews = [
        review
        for review in pending_reviews.get("reviews", [])
        if correlation_id_value(review.get("correlationId")) == correlation_id
    ]

    if not matching_reviews:
        await cl.Message(
            content=(
                "Requirement requires review, but no matching pending review "
                "was returned by Quarkus."
            )
        ).send()
        return

    for review in matching_reviews:
        await prompt_review_decision(client, review)


async def prompt_review_decision(
    client: httpx.AsyncClient,
    review: dict[str, Any],
) -> None:
    actions = [
        cl.Action(
            name="map_existing",
            payload={
                "decision": "MAP_EXISTING",
                "candidateKey": candidate["candidate"]["candidateKey"],
            },
            label=f"Map existing: {candidate['candidate']['label']}",
        )
        for candidate in review.get("candidates", [])
        if candidate.get("candidate", {}).get("candidateKey")
    ]
    actions.append(
        cl.Action(
            name="create_new",
            payload={"decision": "CREATE_NEW"},
            label="Create new node",
        )
    )

    response = await cl.AskActionMessage(
        content=review_prompt_content(review),
        actions=actions,
        timeout=600,
    ).send()
    payload = action_response_payload(response)
    if payload is None:
        await cl.Message(content="Review decision timed out.").send()
        return

    submit_payload = {
        "reviewId": review["reviewId"],
        "decision": payload["decision"],
        "rationale": "Selected via Chat UI review action by human.",
    }
    if payload["decision"] == "MAP_EXISTING":
        submit_payload["candidateKey"] = payload["candidateKey"]

    result = await submit_review_decision(client, submit_payload)
    await cl.Message(content=format_result("Review Decision", result)).send()


def review_prompt_content(review: dict[str, Any]) -> str:
    element = review.get("requirementElement", {})
    candidates = review.get("candidates", [])
    candidate_lines = "\n".join(
        "- {label} (`{key}`)".format(
            label=candidate.get("candidate", {}).get("label", "Unknown candidate"),
            key=candidate.get("candidate", {}).get("candidateKey", "unknown"),
        )
        for candidate in candidates
    )
    if not candidate_lines:
        candidate_lines = "- No existing candidate returned."

    return (
        "Node match review required.\n\n"
        f"Review ID: `{review.get('reviewId', 'unknown')}`\n"
        "Element: `{type}` `{text}`\n"
        "Rationale: {rationale}\n\n"
        "Candidates:\n"
        "{candidate_lines}"
    ).format(
        type=element.get("type", "UNKNOWN"),
        text=element.get("text", ""),
        rationale=review.get("rationale", "No rationale returned."),
        candidate_lines=candidate_lines,
    )


def action_response_payload(response: Any) -> dict[str, Any] | None:
    if response is None:
        return None
    if isinstance(response, dict):
        return response.get("payload") or response
    payload = getattr(response, "payload", None)
    if isinstance(payload, dict):
        return payload
    return None


async def list_pending_reviews_result(client: httpx.AsyncClient) -> dict[str, Any]:
    return await post_quarkus(
        client,
        "/app/q",
        {
            "query": "ListPendingNodeMatchReviewsQuery",
            "payload": {},
        },
    )


async def submit_review_decision(
    client: httpx.AsyncClient,
    payload: dict[str, Any],
) -> dict[str, Any]:
    return await post_quarkus(
        client,
        "/app/c",
        {
            "command": "SubmitNodeMatchReviewDecisionCommand",
            "payload": payload,
        },
    )


def is_review_required(result: dict[str, Any] | None) -> bool:
    return bool(result) and result.get("status") == "REVIEW_REQUIRED"


def correlation_id_value(correlation_id: Any) -> str | None:
    if isinstance(correlation_id, dict):
        value = correlation_id.get("value")
        return str(value) if value is not None else None
    if correlation_id is not None:
        return str(correlation_id)
    return None


async def post_quarkus(
    client: httpx.AsyncClient,
    path: str,
    payload: dict[str, Any],
) -> dict[str, Any]:
    try:
        response = await client.post(
            f"{QUARKUS_BASE_URL}{path}",
            json=payload,
            timeout=QUARKUS_TIMEOUT_SECONDS,
        )
    except httpx.RequestError as error:
        return {
            "error": f"Quarkus is not reachable at {QUARKUS_BASE_URL}{path}",
            "detail": str(error),
        }

    try:
        body = response.json()
    except ValueError:
        body = {"raw": response.text}

    if response.is_error:
        return {
            "error": body,
            "status": response.status_code,
        }

    return body


def parse_arguments(arguments: Any) -> dict[str, Any]:
    if arguments is None:
        return {}
    if isinstance(arguments, dict):
        return arguments
    if isinstance(arguments, str) and arguments.strip():
        return json.loads(arguments)
    return {}


def format_result(title: str, result: dict[str, Any]) -> str:
    return f"{title}:\n\n```json\n{json.dumps(result, indent=2, ensure_ascii=False)}\n```"


def tool_call_name(tool_call: dict[str, Any]) -> str | None:
    function = tool_call.get("function") or {}
    name = function.get("name")
    return str(name) if name is not None else None


def tool_message(
    tool_call: dict[str, Any],
    result: dict[str, Any],
) -> dict[str, str]:
    function = tool_call.get("function") or {}
    name = function.get("name") or "unknown_tool"
    return {
        "role": "tool",
        "name": name,
        "tool_name": name,
        "content": json.dumps(result),
    }
