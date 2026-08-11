# 数据填报系统

基于 Spring Boot 3.3、Vue 3、MySQL 8 的动态数据填报系统。管理员维护用户和权限，模板管理员维护动态字段，填报人员可手工或通过 Excel 提交数据。

## 主要功能

- 动态模板与模板版本：支持字段校验、默认值、选项、唯一值、查询和汇总配置。
- Excel：支持模板导入、单模板导入、多 Sheet 自动匹配、导入预览、失败错误 CSV 和批次记录。
- 填报任务：支持人员或部门范围、填报期限、逾期申请、进度、提醒和周期性定时发布。
- 数据管理：支持分页查询、字段精确检索、导出、修改申请、审核和操作日志。

## 角色

| 角色 | 权限摘要 |
| --- | --- |
| `ADMIN` | 管理用户、部门、模板和全部数据；审核填报及修改申请。 |
| `MAINTAINER` | 维护模板、模板版本和定时发布规则。 |
| `LEADER` | 查看任务与填报数据、导出数据、管理任务、处理逾期填报申请。 |
| `REPORTER` | 填报本人任务、导入 Excel、查看本人数据并提交修改或逾期申请。 |

具体权限以接口上的 Spring Security 鉴权为准。

## 本地启动

前提：JDK 17、Maven、Node.js 和 MySQL 8。

1. 执行 `database/schema.sql`，创建本地 `data_reporting` 数据库。
2. 设置 `DB_USERNAME`、`DB_PASSWORD`、至少 32 位的 `JWT_SECRET`，首次启动还需设置 `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD`。
3. 启动后端：

   ```powershell
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. 启动前端：

   ```powershell
   npm --prefix frontend install
   npm --prefix frontend run dev
   ```

默认地址：前端 `http://127.0.0.1:5173`，后端 `http://127.0.0.1:8080`。

## 使用流程

1. 管理员创建部门、用户和角色。
2. 模板管理员手工创建模板或导入模板 Excel。
3. 管理员或领导创建并发布填报任务；模板管理员可配置周期性发布规则。
4. 填报人员手工填报或导入 Excel，系统统一校验后整批写入。
5. 管理人员查询、导出和审核数据；填报人员对已提交数据通过修改申请处理。

## 文档与安全

- 接口、Excel 参数和返回规则见 [后端说明](backend/README.md)。
- 数据库结构见 `database/schema.sql`。
- 本地敏感配置仅通过环境变量提供，不提交账号、密码、Token 或生产地址。
