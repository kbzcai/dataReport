# Codex 多 Agent 项目模板

本目录提供可复制到新项目的 Codex 协作规则、角色定义和项目级配置。适用于已授权的本地开发项目；不得把密码、Token、生产地址或代理配置写入模板。

```text
target-project/
|-- AGENTS.md
|-- PROJECT_PROFILE.md
`-- .codex/
    |-- config.toml
    |-- agent-collaboration.toml
    |-- agents/
    `-- skills/agent-collaboration-control/SKILL.md
```

## 使用

1. 将 `AGENTS.md`、`.codex/` 和 `PROJECT_PROFILE.md` 复制到目标项目根目录。
2. 按目标项目实际技术栈、模块路径和测试命令填写 `PROJECT_PROFILE.md`。该文件不得包含凭据；字段不完整时，角色必须先探测项目，不能假定 Spring Boot、Vue 或目录结构。
3. 默认 `enabled = false`。需要评估某个任务时，输入：`协作评估：<任务描述>`。
4. 若任务适合协作，协调 Agent 会先展示等级、角色、依赖和风险；只有获得你的明确确认后才派发子 Agent。拒绝或未确认时由主 Agent 处理。
5. 将 `enabled = true` 后，每个项目任务都会自动评估；实际派发前仍必须确认。

`L0/L1` 任务由主 Agent 处理；`L2/L3` 任务在确认后按 `AGENTS.md` 的 Product、实施角色和 Tester 流程执行。详细的权限、并行和审计规则以 `AGENTS.md` 为准。

## 同步

在模板目录执行以下命令，可从当前项目同步规则、`.codex/config.toml`、协作策略、Skill 和角色文件：

```powershell
.\sync-from-project.ps1
```

脚本不会覆盖 `PROJECT_PROFILE.md`，避免将一个项目的技术事实误带入另一个项目。
