# Codex multi-agent project template

This directory is a reusable project-local Codex configuration for Java projects. Copy `AGENTS.md`, `.codex`, and the example `PROJECT_PROFILE.md` into a target project, then complete the profile using that project's actual layout and commands.

```text
target-project/
|-- AGENTS.md
|-- PROJECT_PROFILE.md
`-- .codex/
    |-- config.toml
    `-- agents/
        |-- product.toml
        |-- frontend.toml
        |-- backend.toml
        |-- tester.toml
        `-- repoops.toml
```

`PROJECT_PROFILE.md` is project-specific. It declares the existing backend framework, build tool, module paths, optional frontend stack, database, and ordinary test command. It must never contain credentials or environment-specific endpoints. If it is absent or incomplete, roles detect the stack from the repository instead of treating the template defaults as facts.

The routing levels keep normal requests efficient:

- `L0`: general questions that do not read project files, handled directly.
- `L1`: documentation, Agent configuration, and non-runtime project work; Product performs a compact scope check, while Tester is added only for security or rule-risk work.
- `L2`: a single affected application side; Product, the affected implementation role, then Tester.
- `L3`: cross-side, API-contract, database-schema, permission, or shared-runtime changes; Product, actual parallel implementation where independent, then Tester.

Synchronize reusable rules from this project with:

```powershell
.\sync-from-project.ps1
```

The default command copies `AGENTS.md` and role files only. It deliberately does not copy `PROJECT_PROFILE.md` or `.codex/config.toml`, which are normally project- or machine-specific. Copy the configuration only after manually confirming that it has no local network, model, proxy, or credential settings:

```powershell
.\sync-from-project.ps1 -IncludeConfig
```
