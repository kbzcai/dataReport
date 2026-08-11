---
name: agent-collaboration-control
description: Use when a project task may need Product, Frontend, Backend, Tester, or other Agent collaboration, or when the user asks to enable or disable agent cooperation.
---

# Agent Collaboration Control

This project-local policy controls whether the coordinator may dispatch child Agents. It is persistent in the project directory, defaults to off, and does not change Codex platform capabilities.

## State

Read `.codex/agent-collaboration.toml` before routing a project task. Treat a missing, malformed, or non-boolean `enabled` as `false`. Treat a missing, malformed, or non-boolean `confirm_before_dispatch` as `true`.

- A direct user instruction with the meaning of “开启/启用/打开/进入 Agent 协作、子代理、多 Agent 模式” sets `enabled = true`.
- A direct user instruction with the meaning of “关闭/停用/不要使用 Agent 协作、子代理、多 Agent 模式” sets `enabled = false`.
- Do not change state when those words occur only in a quotation, hypothetical question, documentation analysis, or negated description unrelated to the current task.
- After a state change, report the new project state. The state is not inherited by another project and is not a temporary per-message override.

`enabled = false` means automatic routing is off. An explicit request such as `协作评估：<任务>` or `判断是否适合 Agent 协作：<任务>` still starts a read-only routing assessment for that task. It does not change the persisted state.

`enabled = true` means run the same routing assessment for every project task. It does not grant permission to dispatch without confirmation.

`confirm_before_dispatch = true` is mandatory for this project. Before the first child Agent is created for a task, show the decision level, reasons, impact matrix, roles, parallel group, dependencies, and known risks, then ask the user for explicit confirmation. A confirmation is scoped to the current task only and must not rewrite `enabled`.

If the user declines, does not confirm, or asks to keep the task local, do not dispatch any child Agent; continue as the main Agent when the request is otherwise actionable.

## Routing command

When automatic routing is disabled, recognize only a direct user request containing one of these meanings as an assessment trigger:

- `协作评估：...`
- `判断是否适合 Agent 协作：...`
- `帮我判断是否需要子 Agent：...`

The assessment itself is read-only. It must not create Agents, modify application code, start services, or change the persisted switch.

## Routing

- L0 and L1 tasks: keep execution in the coordinator and do not ask for collaboration confirmation.
- L2 and L3 tasks, or work involving security, permissions, external calls, database changes, API contracts, or shared runtime behavior: after the assessment, ask for confirmation before any dispatch. If confirmation is absent, continue as the coordinator and state the unverified collaboration risk.
- When the user confirms, apply the existing `AGENTS.md` impact matrix and dispatch protocol. Product must still complete the required contract/impact decision before implementation roles are created.
- This file does not override platform capacity, permissions, or required validation.

## Safety

Never place this state in `.codex/config.toml` or in a global Codex configuration. A state file only expresses project policy; it cannot bypass unavailable Agent capacity or invalid role definitions.
