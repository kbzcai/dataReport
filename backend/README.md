# 数据填报系统后端

Spring Boot 3.3 / Java 17 / MySQL 8 REST 服务。动态模板的字段定义保存为 JSON，填报数据按模板保存为 JSON，模板字段会同时作为 Excel 导入表头。

## 启动

需要 JDK 17、Maven 和本机 MySQL。启动时会按 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 读取数据库配置；密码必须通过环境变量提供。建议先执行项目根目录的 `database/schema.sql` 创建新的 `data_reporting` 数据库。

```powershell
cd E:\sjtb_agent\backend
mvn spring-boot:run
```

首次启动可通过 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD` 创建管理员；未提供密码时不会创建账号。`JWT_SECRET` 至少 32 字符。

## 角色

`ADMIN` 管理用户、角色、模板及所有数据，并审核填报和修改申请；`MAINTAINER` 维护模板和定时发布规则；`LEADER` 查看任务与填报、导出数据、管理任务并处理逾期填报申请；`REPORTER` 只能按模板手工填报或导入、查看本人数据并提交修改或逾期申请。兼容保留 `EDITOR`、`VIEWER` 角色。登录后将返回的 token 作为 `Authorization: Bearer <token>` 发送。

## 主要接口

- `POST /api/auth/login`：登录。
- `GET/POST/PUT/DELETE /api/users`：管理员用户及角色管理。
- `GET/POST/PUT/DELETE /api/templates`：模板查询及维护；`GET /api/templates/{id}/excel-template` 下载 Excel 空模板。
- `POST /api/templates/import`：上传只能包含一行表头的 `.xlsx` 创建自定义模板（参数 `code`、`name`、可选 `description`、`file`）；也可先手动创建空模板，再调用 `POST /api/templates/{id}/file` 导入表头。
- `POST /api/templates/import-preview`：预览模板 Excel 的所有 Sheet，逐个校验只能有表头、不能有数据内容，并返回 Sheet 顺序和错误提示。
- `POST /api/templates/import-confirm`：参数 `names` 为按 Sheet 顺序排列的模板名称 JSON 数组，确认后按相同顺序批量新增模板；无效 Sheet 或未命名时整批拒绝。
- `GET/POST/PUT/DELETE /api/reports`：填报记录查询、创建、修改、删除；`PATCH /api/reports/{id}/review` 仅由管理员审核。
- `GET /api/reports/summary`：按模板和填报人返回数据条数；`GET /api/reports/page?templateId={id}&reporterId={id}&page=0&size=10`：分页返回指定填报人的模板明细，`size` 支持 10、20、50。
- `POST /api/reports/import?templateId={id}`：上传 `.xlsx` 并按指定模板导入全部非空 Sheet，支持单行、多行、合并单元格表头；错误会包含 Sheet 名称。
- `POST /api/reports/import`：不传 `templateId` 时按工作表自动匹配多个模板。系统优先将 Sheet 名与启用模板的名称或编码匹配，未命中时按表头唯一匹配；空 Sheet 跳过，匹配不到或匹配到多个模板会拒绝整个文件并返回 Sheet 名称。
- `POST /api/reports/import-preview`：上传 Excel，仅解析并返回非空 Sheet 顺序、名称、建议模板及匹配状态（`NAME`、`HEADER`、`UNMATCHED`、`AMBIGUOUS`），不写入数据。
- `POST /api/reports/import-confirm`：上传同一 Excel，并以 `mapping` JSON 提交确认映射。JSON 数组按非空 Sheet 顺序映射模板 ID，例如 `[1,2]`；JSON 对象可按 Sheet 名映射，例如 `{"经营数据":1,"人员数据":2}`，也可使用数字键表示顺序，例如 `{"0":1,"1":2}`。每个非空 Sheet 都必须有启用模板 ID，且表头校验通过后才会在同一事务中导入。
- `POST /api/reports/{id}/change-requests`：填报人员仅可为本人非草稿记录提交修改申请，需提交拟修改数据、原因和记录的 `baseUpdatedAt`；同一记录只能有一个 `PENDING` 申请。
- `GET /api/change-requests?status=PENDING`：填报人员仅查询本人申请，管理员和领导可查询全部；管理员可通过 `PATCH /api/change-requests/{id}/approve|reject` 审批或驳回，填报人员通过 `PATCH /api/change-requests/{id}/cancel` 撤回。审批会复核 `baseUpdatedAt`，冲突时返回 `409`；驳回必须填写意见。
- `GET/POST/PUT/DELETE /api/tasks`：管理员和领导创建、更新任务，只有管理员可删除；管理员和领导查看全部任务，填报人员和编辑人员仅查看分配给自己的任务。任务响应包含人员展示信息与分配、已提交、待提交进度。
- `GET /api/tasks/assignable-users`：管理员和领导查询可分配的启用 `REPORTER`/`EDITOR` 用户。任务只能按 `DRAFT -> PUBLISHED -> CLOSED` 推进，开始时间不得晚于截止时间；已有填报记录的任务不能修改模板或删除。

模板字段请求示例：

```json
{"code":"daily","name":"日报","description":"每日数据","columns":[{"key":"date","label":"日期","type":"date","required":true},{"key":"amount","label":"数量","type":"number","required":true}]}
```
