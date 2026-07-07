import json
import os
import shlex
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
        "user asks to ingest a new requirement, asks about pending node-match "
        "reviews, or submits a review decision. Do not invent requirement "
        "text, review IDs, candidate keys, or decisions."
    ),
}

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
            "name": "submit_node_match_review_decision",
            "description": "Submit a human decision for one pending node-match review.",
            "parameters": {
                "type": "object",
                "properties": {
                    "reviewId": {
                        "type": "string",
                        "description": "The reviewId returned by list_pending_node_match_reviews.",
                    },
                    "decision": {
                        "type": "string",
                        "enum": ["MAP_EXISTING", "CREATE_NEW"],
                    },
                    "candidateKey": {
                        "type": "string",
                        "description": "Required for MAP_EXISTING; omit for CREATE_NEW.",
                    },
                    "rationale": {
                        "type": "string",
                        "description": "Short explanation of the human review decision.",
                    },
                },
                "required": ["reviewId", "decision", "rationale"],
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
        "Hallo. Ich kenne aktuell diese Werkzeuge:\n\n"
        f"{tool_lines}\n\n"
        "Robuste Direktbefehle ohne LLM:\n"
        "- `/ingest <Requirement-Text>`\n"
        "- `ingest: <Requirement-Text>`\n"
        "- `/reviews`\n"
        "- `/review <reviewId> MAP_EXISTING <candidateKey> <rationale>`\n"
        "- `/review <reviewId> CREATE_NEW <rationale>`\n\n"
        "Schreibe ein neues Requirement, frage nach offenen Reviews, "
        "oder entscheide ein Review mit Review-ID und Entscheidung."
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
            messages.append({"role": "assistant", "content": direct_response})
            cl.user_session.set("messages", messages)
            await cl.Message(content=direct_response).send()
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

    if name == "submit_node_match_review_decision":
        return await post_quarkus(
            client,
            "/app/c",
            {
                "command": "SubmitNodeMatchReviewDecisionCommand",
                "payload": arguments,
            },
        )

    return {"error": f"Unknown tool: {name}"}


async def execute_direct_command(
    client: httpx.AsyncClient,
    content: str,
) -> str | None:
    stripped_content = content.strip()
    lower_content = stripped_content.lower()

    if lower_content.startswith("ingest:"):
        return await ingest_requirement(client, stripped_content.partition(":")[2].strip())

    if lower_content.startswith("requirement:"):
        return await ingest_requirement(client, stripped_content.partition(":")[2].strip())

    if lower_content == "reviews":
        return await list_pending_reviews(client)

    if lower_content.startswith("review:"):
        return await execute_direct_review_command(
            client,
            stripped_content.partition(":")[2].strip(),
        )

    if not stripped_content.startswith("/"):
        return None

    command, _, rest = stripped_content.partition(" ")
    command = command.lower()
    rest = rest.strip()

    if command == "/ingest":
        return await ingest_requirement(client, rest)

    if command == "/reviews":
        return await list_pending_reviews(client)

    if command == "/review":
        return await execute_direct_review_command(client, rest)

    return None


async def ingest_requirement(
    client: httpx.AsyncClient,
    original_text: str,
) -> str:
    if not original_text:
        return "Bitte gib den Requirement-Text an."
    result = await post_quarkus(
        client,
        "/app/c",
        {
            "command": "IngestRequirementCommand",
            "payload": {"originalText": original_text},
        },
    )
    return format_result("Requirement-Aufnahme", result)


async def list_pending_reviews(client: httpx.AsyncClient) -> str:
    result = await post_quarkus(
        client,
        "/app/q",
        {
            "query": "ListPendingNodeMatchReviewsQuery",
            "payload": {},
        },
    )
    return format_result("Offene Reviews", result)


async def execute_direct_review_command(
    client: httpx.AsyncClient,
    rest: str,
) -> str:
    try:
        parts = shlex.split(rest)
    except ValueError as error:
        return f"Review-Befehl konnte nicht gelesen werden: {error}"

    if len(parts) < 3:
        return (
            "Format: `/review <reviewId> MAP_EXISTING <candidateKey> <rationale>` "
            "oder `/review <reviewId> CREATE_NEW <rationale>`"
        )

    review_id = parts[0]
    decision = parts[1].upper()
    if decision == "MAP_EXISTING":
        if len(parts) < 4:
            return "Format: `/review <reviewId> MAP_EXISTING <candidateKey> <rationale>`"
        payload = {
            "reviewId": review_id,
            "decision": decision,
            "candidateKey": parts[2],
            "rationale": " ".join(parts[3:]),
        }
    elif decision == "CREATE_NEW":
        payload = {
            "reviewId": review_id,
            "decision": decision,
            "rationale": " ".join(parts[2:]),
        }
    else:
        return "Entscheidung muss `MAP_EXISTING` oder `CREATE_NEW` sein."

    result = await post_quarkus(
        client,
        "/app/c",
        {
            "command": "SubmitNodeMatchReviewDecisionCommand",
            "payload": payload,
        },
    )
    return format_result("Review-Entscheidung", result)


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
