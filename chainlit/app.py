import json
import os
from typing import Any

import chainlit as cl
import httpx


QUARKUS_BASE_URL = os.getenv("QUARKUS_BASE_URL", "http://localhost:8080")
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "granite4.1:8b")

SYSTEM_MESSAGE = {
    "role": "system",
    "content": (
        "You are a concise requirements-review assistant. Use tools when the "
        "user asks about pending node-match reviews or submits a review "
        "decision. Do not invent review IDs, candidate keys, or decisions."
    ),
}

TOOLS = [
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
    cl.user_session.set("messages", [SYSTEM_MESSAGE])


@cl.on_message
async def on_message(message: cl.Message) -> None:
    messages = cl.user_session.get("messages") or [SYSTEM_MESSAGE]
    messages.append({"role": "user", "content": message.content})

    async with httpx.AsyncClient(timeout=60.0) as client:
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


async def post_quarkus(
    client: httpx.AsyncClient,
    path: str,
    payload: dict[str, Any],
) -> dict[str, Any]:
    response = await client.post(f"{QUARKUS_BASE_URL}{path}", json=payload)
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
