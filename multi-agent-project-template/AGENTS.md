# 多 Agent 协同规则

> 通用模板补充规则：本文件后续“项目技术档案与模板同步”章节优先于任何固定 Vue 或 Spring Cloud 默认描述。

除“版本控制快速通道”外，协调 Agent 必须先按“协同分级路由”判断参与角色和验证深度。不得把低风险任务升级为全量协同，也不得把功能、安全或运行时变更降级后直接交付。

## 角色配置

- Product：`.codex/agents/product.toml`，负责需求范围、业务规则、权限边界、API/数据契约和验收标准。
- Frontend：`.codex/agents/frontend.toml`，按项目技术档案和实际依赖负责前端页面、交互、类型、路由和接口对接。
- Backend：`.codex/agents/backend.toml`，按项目技术档案和实际依赖负责 Java 后端接口、权限、事务、数据访问和测试。
- Tester：`.codex/agents/tester.toml`，负责契约审查、风险分析和验证结果。
- RepoOps：`.codex/agents/repoops.toml`，仅负责 Git/GitHub/SVN 的仓库状态、拉取、提交、推送、分支和标签操作。

## 协同分级路由

1. L0 - 非项目通用问答、无需读取项目文件的状态说明：由协调 Agent 直接处理，不派发角色，不启动服务。
2. L1 - 项目只读分析、文档、`AGENTS.md`、`.codex` 配置、注释、格式或测试代码且不改变应用运行行为：Product 使用轻量判定卡完成范围判断；只有涉及安全、凭据、执行策略、权限或规则冲突时才派发 Tester。不得启动或重启前后端。
3. L2 - 单端功能修改：Product、受影响的 Frontend 或 Backend、Tester 依次参与。只验证受影响服务，禁止为交付无差别重启另一端。
4. L3 - 跨端功能、接口契约、数据库结构、权限或共享运行时逻辑修改：Product 先定义契约，Frontend 与 Backend 在无依赖时实际并行，随后由 Tester 审查。
5. 任一任务命中鉴权、敏感信息、外部调用、数据库迁移、运行时配置或接口契约时，最低升级至 L2；前后端或数据库同时受影响时升级至 L3。无法可靠判定时按更高等级处理并说明原因。

L1 的 Product 轻量判定卡最多包含：范围、影响矩阵、实施角色、验证、服务动作、风险。L2/L3 才输出完整业务规则、API 和 SQL 契约。

## 强制调度协议

本节约束协调 Agent 的实际派发行为；角色 TOML 只定义职责，不能替代运行时的子 Agent 调度。

1. Product Agent 必须先输出固定影响矩阵，并为每项给出一句理由：

   ```text
   frontend=true|false
   backend=true|false
   database=true|false
   contract_changed=true|false
   runtime_behavior_changed=true|false
   restart_frontend=true|false
   restart_backend=true|false
   ```

   随后明确列出必须派发的角色、可并行组和前置依赖。`database=true` 归 Backend Agent 负责；没有实现变更时，矩阵四项均为 `false`，仍须派发 Product，Tester 是否参与按 L1 风险条件决定。
2. 协调 Agent 必须依据已完成的影响矩阵实际启动实施角色：`frontend=true` 时启动 Frontend；`backend=true` 或 `database=true` 时启动 Backend。`contract_changed=true` 时必须至少派发一个相关实现角色并说明契约消费者；若确认只是文档示例变化，必须显式标注为 docs-only 并将 `contract_changed` 设为 `false`。不得在 Product 输出前启动实施角色。
3. 当前端与后端均受影响时，协调 Agent 必须在同一阶段实际并行启动 Frontend 和 Backend，并向两者提供同一份 Product 契约；不得以“可并行”代替实际并行派发。实施任务存在真实依赖时，必须记录依赖原因和串行顺序。
4. 所有实施角色完成后，才可启动 Tester 进行最终审查；Tester 不得与仍会产生待审查改动的实施角色并行执行最终审查。
5. 协调 Agent 的过程更新和最终交付必须保留调度证据：Product 完成情况、已启动的实施角色、并行组、Tester 启动时机及不可用角色的原因。角色未启动、并行槽不足或工具不可用时，必须如实说明降级和风险，不得表述为已完成并行协作。
6. 本协议只约束协调 Agent 的决策和审计，不能单独保证 Codex 运行时具有可用的多 Agent 工具或并发额度；运行时限制不得被配置掩盖。

## 版本控制快速通道

仅当用户当前请求同时满足以下条件时，协调 Agent 才能只调度 RepoOps，不进入固定四角色流程：

1. 请求明确包含独立的版本控制关键词 `git`、`github`、`svn` 或 `subversion`（大小写不敏感）。
2. 用户意图确为版本控制操作，例如状态查看、拉取、比较、分支、标签、提交或推送。
3. 请求不包含业务代码、接口、数据库、配置或测试的修改要求。

仅出现关键词不足以触发快速通道。例如“用 Git 分析代码 Bug”仍按正常流程处理；“git status”“github push”“svn update”可触发 RepoOps。

- 纯版本控制请求：只调度 RepoOps。它仍须核对仓库根目录、远端 URL、分支、工作区和待提交差异。
- 代码修改后附带版本控制请求：先完成 Product、相关实现角色和 Tester 的一次完整流程，再由 RepoOps 执行提交或推送；不得为同一批已审查改动重复调度四角色。
- 存在未审查的业务代码改动时，RepoOps 不得直接提交，应交回正常流程。
- 代码、配置、数据库或文档修改完成后，默认只保留工作区改动；任何角色均不得自动执行 `git add`、`git commit`、`git push`、`svn commit` 或其他提交动作。
- RepoOps 只有在用户后续命令同时包含版本控制关键词和明确动作时才执行对应写操作。例如“git 提交”“github 推送”“svn update”；不得因“代码改好了”“查看 Git 状态”或单独出现“GitHub”等请求自动提交、推送或拉取。
- `git push --force`、`git reset --hard`、`git clean`、变基、删除分支或标签、覆盖远端历史、SVN 回滚/删除等高风险操作，必须在展示目标和影响后取得用户二次明确确认。
- 认证只能使用 Git Credential Manager、SSH 或安全存储的个人访问令牌；不得将账号密码、Token 或密钥写入项目文件、Git 配置、远端 URL 或提交历史。
- 远端拉取或推送失败时，RepoOps 可自动收集 Git 错误、DNS 解析、443 端口连通性、有效 Git/环境代理设置及现有 hosts 记录，并在没有远端分叉时重试一次普通操作。
- RepoOps 不得根据 ping 或 DNS 结果自动新增、修改或删除 Windows hosts 中的 GitHub 映射。GitHub IP 会变化，且 DNS/ICMP 成功不能证明 HTTPS 或代理可用；如确需修改 hosts，必须由用户在单独命令中明确确认准确域名、IP 和影响范围。

## 强制流程

1. 协调 Agent 必须先按“协同分级路由”确定等级；L1 以上必须先由 Product Agent 进行范围和影响分析。
2. L2/L3 涉及前端、后端、数据库、接口或运行时配置时，必须调度相关实现 Agent；L3 的前后端可并行时必须共享 Product Agent 输出的契约并实际并行。
3. L2/L3 变更完成后必须由 Tester Agent 审查权限、兼容性、异常场景和验证结果；L1 仅在“协同分级路由”规定的风险命中时派发 Tester。
4. 子 Agent 不可用时，协调 Agent 必须在交付中明确说明缺失的协同环节及其风险，不得伪称已完成协同。
5. 最终交付必须说明参与的角色、修改内容、验证结果和剩余风险。

## 边界

- Product 和 Tester 默认只读，不直接修改业务代码。
- Frontend 仅修改前端目录及必要的前端配置。
- Backend 仅修改后端、数据库迁移脚本及必要的后端配置。
- RepoOps 仅操作版本控制元数据和用户明确指定的暂存、提交、拉取、推送、分支或标签；不得修改业务代码、数据库、应用配置或凭据。
- 不允许任一角色越过权限边界修改无关模块、凭据或生产数据。

## 运行生命周期决策

Product 必须在影响矩阵中声明 `runtime_behavior_changed`、`restart_frontend` 和 `restart_backend`。只有功能或运行时行为变更才允许操作服务；文档、Agent 配置、`AGENTS.md`、纯测试、注释、格式和只读分析不得为了交付启动、停止或重启前后端。

- 前端页面、路由或前端运行时配置变更时才设 `restart_frontend=true`。开发服务器已运行且支持热更新时，优先验证热更新，不得重启后端；仅依赖、构建或开发服务器配置变更需要重启前端。
- Java 业务逻辑、接口、鉴权、后端运行时资源、依赖或数据库结构迁移变更时设 `restart_backend=true`。不得重启前端，除非前端也受影响。
- 纯数据库数据修正默认不重启服务；仅在缓存刷新、映射或运行时行为受影响时重启相应服务。
- 纯后端测试变更只运行目标测试，不因测试文件本身重启后端。

## 后端变更运行约束

当 `restart_backend=true` 时，涉及 `backend/` 下的 Java 代码、资源配置、Maven/Gradle 依赖或数据库迁移脚本的修改必须遵守以下顺序：

1. 先识别并停止本项目正在运行的后端应用进程；不得在运行中的 JAR、classes 或资源文件上直接覆盖构建。
2. 完成后端修改并运行相关 Maven/Gradle 构建或测试。
3. 构建通过后，使用本地环境变量启动新的后端应用。
4. 验证服务端口、关键接口和本次改动对应功能后，再向用户交付。

前端、纯文档或 Agent 配置变更不得停止后端。数据库数据修正如不涉及运行时行为，可在确认目标后单独执行，并在交付中说明。

## 修改后的验证与交付约束

1. 只验证本次变更相关的最小功能；只有 `restart_frontend=true` 或 `restart_backend=true` 时，才启动并验证对应服务。
2. 需要启动服务而失败时不得宣称任务已完成，必须说明失败原因和当前可用状态。
3. 仅在本次启动或验证了前端时提供前端地址；仅在本次启动或验证了后端接口时提供接口地址。

## 按影响范围验证

- 修改后由协调 Agent 根据影响范围自行判断验证深度。小型、局部且不涉及接口契约、数据库、构建配置或共享核心逻辑的修改，可只运行针对性检查，不要求全量测试或完整重构建。
- 涉及后端接口、数据库、依赖/构建配置、共享核心逻辑或跨模块行为的修改，仍须执行相关 Maven 构建/测试和接口验证；若无法全量验证，必须说明未覆盖范围与风险。

## 模板字段变更与数据处理

- 模板字段增删可能导致历史填报数据无法兼容。若字段结构发生实质变化，可以在页面明确提示并经确认后清空该模板关联的填报数据；不得静默删除，也不得影响其他模板数据。

## 模板同步

`multi-agent-project-template` 是本项目的可复用多 Agent 配置副本。修改以下文件后，必须在同一任务中同步更新模板目录中的对应文件：

- `AGENTS.md`
- `.codex/config.toml`
- `.codex/agents/*.toml`

仅维护本仓库内 `multi-agent-project-template` 模板副本时，可运行 `multi-agent-project-template/sync-from-project.ps1` 执行默认同步。目标项目只复制 `AGENTS.md`、`.codex` 和 `PROJECT_PROFILE.md` 时不包含该脚本，必须忽略本节同步命令。`.codex/config.toml` 默认不同步；仅在明确执行 `-IncludeConfig`，并确认源文件不含本地网络、模型、代理、凭据或其他环境项时才可同步。不得同步 `TEST_ACCOUNTS.md`、`.env`、本地数据库配置、`PROJECT_PROFILE.md` 或其他包含凭据的 Markdown/TOML 文件。

## 项目技术档案与模板同步

协同开始时优先读取项目根目录的 `PROJECT_PROFILE.md`。它记录后端框架、构建工具、模块路径、可选前端技术栈、数据库和常用测试命令，不得包含账号、密码、Token、代理或生产地址。

若档案不存在或字段缺失，Product 和实施角色必须从 `pom.xml`、`build.gradle`、模块目录、`package.json` 和现有脚本探测实际技术栈；不得因通用模板默认值而初始化 Vue、Spring Cloud 或新增依赖。未经用户明确授权，不得替换既有框架、构建工具、模块边界、数据库类型、主要依赖或部署拓扑。技术档案只影响路由和实现方式，不改变 L0-L3 分级与风险升级规则。
