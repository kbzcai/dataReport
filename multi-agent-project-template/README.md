# Codex 多 Agent 项目模板

本目录保留了可复用的项目级 Codex 配置结构。使用时，将本目录中的 `AGENTS.md` 和 `.codex` 目录复制到目标项目根目录。

目录结构：

```text
目标项目/
├── AGENTS.md
└── .codex/
    ├── config.toml
    └── agents/
        ├── product.toml
        ├── frontend.toml
        ├── backend.toml
        └── tester.toml
```

后端角色面向 Java、Spring Boot 和 Spring Cloud 项目。复制后可按目标项目技术栈、路径和构建命令调整角色说明。

当源项目的 `AGENTS.md`、`.codex/config.toml` 或 `.codex/agents/*.toml` 更新后，在本目录执行：

```powershell
.\sync-from-project.ps1
```

脚本只同步多 Agent 配置，不同步测试账号、本地密码或环境变量文件。
