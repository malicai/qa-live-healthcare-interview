# CLAUDE.md — 项目开发规范(AI 必读)

本项目为 QA Live Healthcare 在线问诊平台(Vue 3 前端 + Spring Boot 后端)。**后续所有开发必须遵循本文件及 `docs/` 下的两份详细规范。**

## 通用规范

- 文档、注释、提交信息使用中文;代码标识符使用英文。
- Git 提交遵循 Conventional Commits(依据 `.asdm/toolsets/basic-tools/spec/git-commit-spec.md`、`git-commit-message-spec.md`):
  - 格式:`<type>(<scope>): <subject>`
  - `type` 取:`feat | fix | refactor | docs | style | test | chore | ci | perf`
  - `subject` ≤ 50 字符,祈使句,结尾不加句号;`body` 每行 ≤ 72 字符
  - 一个提交只做一件事;禁止提交密钥、密码、二进制文件、调试代码
  - 禁止直接提交到 `main`,一律走特性分支 + PR
- 所有用户 prompt 必须按时间顺序追加到 `docs/prompts.md`。
- 禁止提交任何密钥/密码;敏感配置使用环境变量占位。
- 新功能走 PRD 流程,模板见 `.asdm/toolsets/prd-builder/spec/`。

## Server 端(`server/**`)

- 基线:Java 17 + Spring Boot 3.5.7 + Maven Wrapper(不依赖全局 Maven)。
- 包结构:`com.leansofx.<service>` 下按 `controller / service / repository / model / dto / config` 技术分层;目录 kebab-case;类文件名 PascalCase;测试类以 `Test.java` 结尾。
- 代码风格:4 空格缩进;左大括号与语句同行;一行一条语句;重写方法必须加 `@Override`;常量 `UPPER_SNAKE_CASE`。
- REST:Controller 使用 `@RestController` + `@RequestMapping("/api/...")`;**新增接口使用 `/api/v1/` 前缀**;错误响应统一为 `{error:{code,message,details},timestamp,requestId}`;接口文档按 Controller 组织并附 curl 示例。
- 配置:集中在 `src/main/resources/application.properties`;端口 user=8080、question=8081;密钥不落库。
- 异常与日志:禁止空 catch;日志分级,禁止记录密码等敏感信息。
- 测试:JUnit 5 + `@SpringBootTest`;命名 `should ... when ...`;数据库集成测试使用 Testcontainers。
- 验证命令(Windows 用 `mvnw.cmd`):
  - `cd server/qa-service-user && ./mvnw clean compile`
  - `./mvnw test`

## Web 前端(`web/qa-web/**`)

- 基线:Vue 3.5 `<script setup lang="ts">` + TypeScript 5.5 + Vite 5 + Ant Design Vue 4 + vue-router 4 + dayjs。
- 目录:`src/views/`(页面)、`src/components/`(公共组件)、`src/store/`(状态中心)、`src/router/`、`src/data/`、`src/locales/`(国际化资源)。
- 代码风格:2 空格缩进;单引号;一致使用分号;行宽 80–100;函数必须显式声明返回类型;禁用 `any`;对象形状用 `interface`。
- SFC 顺序:`<template>` → `<script setup lang="ts">` → `<style scoped>`;页面/组件文件名 PascalCase。
- 状态:页面通过 `computed` 读取 `store`,不直接修改 `store.state`;不引入 Pinia。
- UI:只使用 `ant-design-vue` 的 `a-` 组件;图标从 `@ant-design/icons-vue` 导入;提示用 `message`;日期用 `dayjs`。
- 验证命令:
  - `cd web/qa-web && npm run build`(含 `vue-tsc` 类型检查)
  - `npx vue-tsc -b`(仅类型检查)

## 详细规范

- [Server 端开发规范](docs/server-coding-standards.md)
- [Web 前端开发规范](docs/web-coding-standards.md)
- ASDM 原始规范:`.asdm/toolsets/context-builder/spec/`、`.asdm/toolsets/basic-tools/spec/`
