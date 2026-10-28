# Web 前端开发规范(Vue 3 / TypeScript)

> 适用范围:`web/qa-web/` 全部前端代码。
> 规则来源:`.asdm/toolsets/context-builder/spec/standard-coding-style.md`(TypeScript 节 + 通用模式)、`standard-project-structure.md`(TS 变体),并结合本项目实际 Vue 代码落地(ASDM 模板无 Vue 专节)。
> 与 ASDM 通用模板冲突处以本文「规范裁定」章节为准。

## 1. 技术基线

| 项 | 约定 |
| ---- | ---- |
| 框架 | Vue 3.5,统一使用 `<script setup lang="ts">` |
| 语言 | TypeScript 5.5(strict 模式) |
| 构建 | Vite 5 |
| UI | Ant Design Vue 4(`a-` 前缀组件) |
| 路由 | vue-router 4(history 模式) |
| 日期 | dayjs |
| 状态 | 项目自带 reactive store(`src/store/index.ts`),不引入 Pinia |

## 2. 命名规范

| 元素 | 规则 | 示例 |
| ---- | ---- | ---- |
| 变量 / 函数 / 方法 | camelCase | `currentDoctor`、`getActiveDoctors` |
| 类 / 接口 / 类型 | PascalCase | `Doctor`、`Question` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 页面 / 组件文件 | PascalCase | `Doctors.vue`、`AppHeader.vue` |
| 目录 | kebab-case | `src/views` 下的多词目录用 `doctor-room/` |
| 路由 path | kebab-case | `/doctor/login`、`/consultation` |
| CSS class | kebab-case | `.doctor-card` |
| 测试文件 | `.spec.ts` 后缀 | `store.spec.ts` |

## 3. 代码格式

- 缩进:**2 空格**。
- 字符串使用单引号;模板字符串使用反引号;一致使用分号。
- 行宽 80–100 字符(CSS 样式规则可放宽)。
- 函数必须**显式声明返回类型**。
- **禁用 `any`**,使用明确类型;对象形状用 `interface`,联合值用字面量联合类型(如 `'pending' | 'answered'`)。
- 注释解释「为什么」,复杂逻辑/业务规则需注释;公共函数使用 JSDoc(`@param` / `@returns`)。

## 4. 目录结构

```
web/qa-web/src/
├── views/        # 页面组件(Home、Doctors、Consultation、DoctorLogin、DoctorRoom、About)
├── components/   # 可复用公共组件(AppHeader、AppFooter)
├── store/        # 全局状态(单文件 reactive store,集中定义并导出 interface)
├── router/       # 路由定义
├── data/         # 静态数据(当前 JSON 数据源)
├── locales/      # 国际化语言资源(zh-CN / en-US)
├── assets/       # 静态资源
└── style.css     # 全局样式
```

- 新增页面放入 `views/` 并在 `router/index.ts` 注册路由。
- 跨页面复用的 UI 放入 `components/`;仅单页面使用的子组件可放在页面同级目录。
- 嵌套目录不超过 3–4 层。

## 5. Vue SFC 约定

- 单文件组件内容顺序:**`<template>` → `<script setup lang="ts">` → `<style scoped>`**。
- 页面样式一律使用 `<style scoped>`,避免全局污染。
- 组件 props / emits 必须声明类型;复杂 props 使用 `interface` 定义。
- 模板中避免复杂表达式,计算逻辑放入 `computed` 或函数。
- 列表渲染必须提供稳定 `:key`。
- 页面级数据读取使用 `computed(() => store...)`,保证响应式。

## 6. 状态管理约定

- 全局状态集中在 `src/store/index.ts`,使用 `reactive` 定义,导出 `store` 对象。
- 页面/组件**只读** `store.state` 或调用 store 方法;**禁止直接修改 `store.state`**。
- 数据修改必须封装为 store 方法(如 `loginDoctor`、`addQuestion`)。
- 实体 interface 在 store 中集中定义并导出,供页面复用。
- 当前为内存状态,刷新后重置属预期;接入后端后由 API 替换数据来源。

## 7. UI 与第三方使用约定

- 只使用 `ant-design-vue` 组件(模板中 `a-` 前缀,如 `a-button`、`a-table`、`a-form`)。
- 图标统一从 `@ant-design/icons-vue` 导入。
- 操作反馈使用 `message`(来自 `ant-design-vue`),不使用 `alert`/`console`。
- 日期格式化统一使用 `dayjs`。
- 新增依赖需说明用途,避免引入与现有技术栈重叠的库(如状态库、日期库、i18n 库)。

## 8. 国际化约定(首页中英文切换)

- 语言资源文件保存在 `src/locales/`(如 `zh-CN.ts`、`en-US.ts`)。
- 文案 key 使用 camelCase 或点号分组(如 `home.hero.title`),中英文 key 保持一致。
- 页面中禁止硬编码双语文案,统一从语言资源读取。
- 按面试要求,仅首页需要国际化,其他页面保持现状。

## 9. 测试规范

- 测试文件使用 `.spec.ts` 后缀,与被测文件同目录或放入 `__tests__/`。
- 结构:Arrange–Act–Assert;命名 `should <expected behavior> when <condition>`。
- 优先测试 store 方法与纯逻辑函数。
- 新增/修改 store 方法或工具函数必须补充对应测试。

## 10. 验证命令

```bash
cd web/qa-web
npm run build      # vue-tsc 类型检查 + 生产构建
npx vue-tsc -b     # 仅类型检查
```

## 11. 规范裁定(ASDM 模板冲突处理)

| 冲突点 | 裁定 |
| ---- | ---- |
| ASDM 无 Vue 专节 | 采用 TypeScript 规则 + 本文第 5–8 节的项目 Vue 约定 |
| TS 目录模板(`controllers/services/models/repositories/...`)为 Node 后端模板 | 不适用,按本项目实际 `src/` 结构(第 4 节) |
| 缩进/引号等 TS 规则 | 与现有代码一致:2 空格 + 单引号 + 分号 |
| ESLint 规则(`explicit-function-return-type` 等) | 纳入规范;当前项目未配置 ESLint,新增配置时按此执行 |

## 12. 规范来源

- `.asdm/toolsets/context-builder/spec/standard-coding-style.md`(TS 命名/格式/类型/通用模式/测试)
- `.asdm/toolsets/context-builder/spec/standard-project-structure.md`(目录与命名原则)
- `.asdm/toolsets/basic-tools/spec/git-commit-spec.md`、`git-commit-message-spec.md`(提交规范)
- 现有代码约定:`web/qa-web/src/` 下全部源码
