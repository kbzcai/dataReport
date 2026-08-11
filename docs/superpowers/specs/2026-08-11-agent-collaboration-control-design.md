# 项目级 Agent 协作开关

## 目标

将子 Agent 协作状态保存于当前项目，默认关闭。用户可以显式要求“协作评估”，由主 Agent 判断当前任务是否适合 Agent 工作流；即使适合，也必须先展示路由结果并获得用户确认后才进入协作。

## 方案

- 使用 `.codex/agent-collaboration.toml` 保存项目策略：`enabled = false` 和 `confirm_before_dispatch = true`。
- 该文件是项目策略，不是 Codex 运行时配置；它不会覆盖平台容量、权限或工具限制。
- `AGENTS.md` 规定入口流程：先读取状态；明确的启停意图更新状态并回显；关闭时只有显式“协作评估”触发当前任务判定；开启时每个任务自动判定；任何实际派发前都必须取得当前任务的用户确认。
- `.codex/skills/agent-collaboration-control/SKILL.md` 提供状态读取、评估命令识别、复杂度判断、确认门禁与故障降级规则。
- 模板副本含同一规则、Skill 及默认关闭的状态文件。同步脚本纳入这三类文件。

## 约束与验证

- 不将开关放入 `[agents]`：Tester 已证明该表使当前 Codex 配置解析失败。
- 只将用户直接表达的启停意图视为状态变更；引用、否定、讨论或分析文本不切换状态。
- L0/L1 由主 Agent 执行；L2/L3 评估为适合协作时先请求当前任务确认，未确认则由主 Agent 执行。确认不持久化为全局开关。
- 通过 `codex features list`、`codex debug prompt-input`、TOML 解析和模板文件比较验证；不启动应用服务。
