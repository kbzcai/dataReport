# 项目级 Agent 协作开关 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `E:\sjtb_agent` 增加默认关闭、跨会话保留但不跨项目继承的 Agent 协作策略。

**Architecture:** 状态与 Codex 配置分离，状态由项目根 `.codex/agent-collaboration.toml` 保存。`AGENTS.md` 和项目内 Skill 同时解释该策略，模板及同步脚本保证新项目副本也以关闭状态开始。

**Tech Stack:** TOML、Markdown、PowerShell、Codex CLI。

---

### Task 1: 修复阻断 Codex 解析的项目配置

**Files:**
- Modify: `.codex/config.toml`
- Modify: `multi-agent-project-template/.codex/config.toml`

- [ ] **Step 1: 运行配置解析基线检查**

Run: `codex features list`
Expected: FAIL，错误包含 `invalid type: string "high", expected struct AgentRoleToml`。

- [ ] **Step 2: 删除无效的 `[agents]` 表及其字段**

删除 `[agents]`、`enabled`、`max_concurrent_threads_per_session`、`default_subagent_reasoning_effort` 和 `interrupt_message`，保留已有项目通用配置。

- [ ] **Step 3: 验证配置已可解析**

Run: `codex features list`
Expected: 命令退出码为 0。

### Task 2: 添加项目策略和可发现 Skill

**Files:**
- Create: `.codex/agent-collaboration.toml`
- Create: `.codex/skills/agent-collaboration-control/SKILL.md`

- [ ] **Step 1: 验证当前 prompt 输入不含项目协作 Skill**

Run: `codex debug prompt-input`
Expected: 输出不含 `agent-collaboration-control`。

- [ ] **Step 2: 创建默认关闭的策略文件**

```toml
enabled = false
```

- [ ] **Step 3: 创建最小 Skill**

Skill 必须读取策略文件，将明确的启停意图归一化，并在关闭状态下阻止子 Agent 调度。

- [ ] **Step 4: 验证 Skill 已被 Codex 发现**

Run: `codex debug prompt-input`
Expected: 输出包含 `agent-collaboration-control`；否则记录实际可发现路径并停止，不假设该目录有效。

### Task 3: 同步执行规则与模板

**Files:**
- Modify: `AGENTS.md`
- Modify: `multi-agent-project-template/AGENTS.md`
- Modify: `multi-agent-project-template/sync-from-project.ps1`
- Create: `multi-agent-project-template/.codex/agent-collaboration.toml`
- Create: `multi-agent-project-template/.codex/skills/agent-collaboration-control/SKILL.md`

- [ ] **Step 1: 增加入口状态读取和复杂度门槛**

规则必须仅接受用户直接、肯定的启停意图，避免引用或否定文本切换状态；关闭时 L0/L1 主 Agent 执行，L2/L3 先请求开启。

- [ ] **Step 2: 将策略文件和 Skill 加入同步清单**

`sync-from-project.ps1` 必须同时复制 `.codex\\agent-collaboration.toml` 和 `.codex\\skills\\agent-collaboration-control\\SKILL.md`。

- [ ] **Step 3: 运行同步并验证模板一致**

Run: `powershell -ExecutionPolicy Bypass -File multi-agent-project-template/sync-from-project.ps1`
Expected: 输出 `Multi-agent template files and config synchronized.`，并且每个同步文件内容一致。

### Task 4: 进行定向验收

**Files:**
- Verify: `.codex/config.toml`
- Verify: `.codex/agent-collaboration.toml`
- Verify: `.codex/skills/agent-collaboration-control/SKILL.md`

- [ ] **Step 1: 静态解析所有 TOML 文件**

Run: PowerShell `ConvertFrom-Toml` 或 Codex CLI 配置解析。
Expected: 所有本次 TOML 无解析错误。

- [ ] **Step 2: 检查策略默认值和规则覆盖范围**

Run: `rg -n "agent-collaboration|子代理|Agent 协作|enabled = false" AGENTS.md .codex multi-agent-project-template`
Expected: 项目和模板均出现状态、语义指令、L0/L1 与 L2/L3 门槛。

- [x] **Step 3: 关闭状态下由主 Agent 复审实施结果**

Expected: 检查配置解析、Skill 发现、模板一致性和未改动应用模块；不运行 Maven/npm、不启动服务。协作状态开启时，此步骤改由 Tester 执行。
