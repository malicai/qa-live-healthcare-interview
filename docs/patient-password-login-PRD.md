# Feature PRD: 问诊用户用户名密码登录

| 项目 | 内容 |
| ---- | ---- |
| Feature ID | `FEAT-001` |
| 特性名称 | 问诊用户用户名密码登录 |
| 创建日期 | `2026-10-28` |
| 状态 | `COMPLETED` |
| 文档语言 | 简体中文 |
| 作者 | malicai |
| 关联文档 | [README.md](../README.md)、[PRD_TEMPLATE.md](./PRD_TEMPLATE.md)、[doctor-data-api-design.md](./doctor-data-api-design.md)、[server-coding-standards.md](./server-coding-standards.md)、[web-coding-standards.md](./web-coding-standards.md) |

## 1. 概述(Overview)

### 1.1 特性摘要

当前问诊患者通过「姓名 + 生日」在浏览器内验证身份,患者数据保存在前端 JSON 文件中,没有密码、没有服务端认证,任何人知道姓名和生日即可冒用身份。本特性为问诊患者提供**用户名 + 密码**的注册与登录方式,患者账号及密码哈希存储于 MySQL,由 `qa-service-user` 提供注册/登录接口,前端问诊页使用新的登录/注册表单替换原有姓名+生日验证流程。

受益方:患者(账号安全、可找回自己的问诊记录)、医生(问诊对象身份可信)、系统(患者凭据不再暴露在前端)。

### 1.2 目标(Objectives)

- 目标 1:患者可使用用户名 + 密码注册账号,用户名全局唯一
- 目标 2:患者可使用用户名 + 密码登录,失败时返回明确错误
- 目标 3:患者账号与密码哈希存储于 MySQL,前端不再保存患者数据与凭据
- 目标 4:保留既有 5 位患者与其历史问题的关联(通过种子账号实现)

### 1.3 范围(Scope)

**包含:**

- `patient` 数据表(Flyway 迁移)与 5 位既有患者的种子账号
- 患者注册接口、登录接口(含参数校验、唯一性校验、密码哈希校验)
- 前端 API 层、store 患者登录/注册方法、问诊页登录/注册表单
- 后端自动化测试、curl 接口测试与相关文档更新

**不包含:**

- JWT / 服务端会话 / 权限体系(本期登录成功后仅在前端内存记录当前患者)
- 患者资料编辑、找回密码、第三方登录、短信验证
- 医生登录改造(已通过 `POST /api/v1/doctors/login` 完成)
- 问题数据迁移至 MySQL(仍使用前端 JSON,后续需求处理)
- 管理端患者管理、登录限流/防暴力破解(列为后续增强)

## 2. 用户故事(User Stories)

### 故事 1:新患者注册

**作为** 首次使用平台的患者,
**我希望** 使用用户名和密码注册账号,
**以便** 安全地发起问诊并查看自己的记录。

**验收标准:**

- [ ] 注册表单包含用户名、密码、姓名、生日(必填),手机号、性别(选填)
- [ ] 密码长度 ≥ 6,注册成功后自动登录并进入问诊页
- [ ] 用户名已存在时提示「用户名已存在」,接口返回 409
- [ ] 用户名格式非法或必填项为空时,接口返回 400 并给出字段级错误

### 故事 2:已注册患者登录

**作为** 已注册患者,
**我希望** 使用用户名和密码登录,
**以便** 查看和提交我的问诊问题。

**验收标准:**

- [ ] 输入正确用户名和密码后进入问诊页,显示「XXX 的问诊」
- [ ] 密码错误或用户名不存在时提示「用户名或密码错误」,接口返回 401(不区分具体原因)
- [ ] 用户名或密码为空时前端表单校验拦截
- [ ] 登录成功后刷新页面需重新登录(内存会话,符合本期范围)

### 故事 3:既有患者数据延续

**作为** 已存在于系统中的患者,
**我希望** 使用预置账号登录,
**以便** 继续查看此前提交的问题。

**验收标准:**

- [ ] 5 位既有患者拥有种子账号,密码统一为 `123456`(bcrypt 存储)
- [ ] 种子患者登录后,仍能按原 `patientId` 看到历史问题
- [ ] 种子数据仅在数据库无记录时初始化一次,不重复插入

## 3. 功能需求(Functional Requirements)

| ID | 需求描述 | 优先级 | 关联故事 |
| ---- | ---- | ---- | ---- |
| REQ-001 | 提供 `POST /api/v1/patients/register` 注册接口,接收 `username`、`password`、`name`、`birthday`(必填)及 `phone`、`gender`(选填) | High | 故事 1 |
| REQ-002 | 用户名全局唯一,创建前校验;重复时返回 409 `CONFLICT`,错误信息「用户名已存在」 | High | 故事 1 |
| REQ-003 | 密码长度 ≥ 6,使用 bcrypt(rounds=10)哈希后存储,任何接口与日志不得输出密码或哈希 | High | 故事 1、2 |
| REQ-004 | 提供 `POST /api/v1/patients/login` 登录接口,用户名不存在或密码错误统一返回 401 `UNAUTHORIZED`,信息「用户名或密码错误」 | High | 故事 2 |
| REQ-005 | 注册/登录成功均返回 `PatientResponse`(`id`、`username`、`name`、`birthday`、`phone`、`gender`),不包含密码字段 | High | 故事 1、2 |
| REQ-006 | 前端问诊页以「登录 / 注册」两个 Tab 替换原有姓名+生日验证表单,保留原问诊问题列表与提交功能 | High | 故事 1、2 |
| REQ-007 | 登录/注册成功后写入 `store.state.currentPatient`,页面展示当前患者姓名;刷新后内存状态重置 | Medium | 故事 2 |
| REQ-008 | 提供「切换用户/退出登录」操作,清除 `currentPatient` | Medium | 故事 2 |
| REQ-009 | 初始化 5 位既有患者为种子账号(`patient001`~`patient005` 对应的用户名见 10.3),密码 `123456`,保留原 `id` 以关联历史问题 | High | 故事 3 |
| REQ-010 | 所有错误响应遵循统一格式 `{error:{code,message,details},timestamp,requestId}`,状态码符合规范(400/401/409/500) | High | 故事 1、2 |
| REQ-011 | 生日使用 `YYYY-MM-DD` 格式,后端以 `DATE` 存储;手机号、性别可为空 | Medium | 故事 1 |

## 4. 非功能需求(Non-Functional Requirements)

### 4.1 性能(Performance)

- 注册、登录接口在本机/内网环境下响应时间 ≤ 500ms(P95)
- bcrypt rounds=10,单次校验耗时约 50–100ms,可接受
- 用户名查询走唯一索引,避免全表扫描

### 4.2 安全(Security)

- 密码仅以 bcrypt 哈希存储,禁止明文、可逆加密;日志与 API 响应不得包含密码/哈希
- 登录失败不区分「用户名不存在」与「密码错误」,避免账号枚举
- 使用 Spring Data JPA 参数绑定,防止 SQL 注入
- 沿用现有 CORS 配置;生产环境应收紧 `allowed-origins`
- 本期不实现登录限流,后续建议增加 IP/账号维度限流与失败锁定

### 4.3 可扩展性(Scalability)

- `patient` 表 `username` 建唯一索引,支撑后续账号规模增长
- 接口路径使用 `/api/v1` 版本化,便于后续增加资料修改、找回密码等能力
- 患者 ID 使用字符串主键,兼容既有 `patientId` 引用

### 4.4 可靠性(Reliability)

- 注册过程使用事务:唯一性校验与插入在同一事务内,避免并发重复
- 统一异常处理,数据库异常返回 500 `INTERNAL_ERROR`,不泄漏堆栈
- 前端对网络错误、401、409、400 分别给出可读提示

## 5. 技术需求(Technical Requirements)

### 5.1 架构约束(Architecture Considerations)

- 后端:`qa-service-user`(Spring Boot 3.5.7 / Java 17 / JPA / Flyway),新增 `patient` 相关实体、Repository、Service、Controller、DTO
- 前端:Vue 3 + TypeScript + Vite + Ant Design Vue,新增 `src/api/patients.ts`,复用 `src/api/http.ts` 的请求封装与错误解析
- 数据流:MySQL → 后端 API → 前端 store(`currentPatient`)→ 问诊页
- 复用现有模式:`DoctorController` / `DoctorService` / `DoctorRepository` / `ApiException` / `GlobalExceptionHandler` / `PasswordConfig`

### 5.2 依赖(Dependencies)

- 内部:`qa-service-user`、MySQL 8(`192.168.88.104:3306`,已部署)、Flyway 迁移目录
- 外部:Spring Data JPA、Flyway、spring-security-crypto(bcrypt)、MySQL Connector/J;前端无新增第三方依赖
- 数据:既有问题 JSON 中的 `patientId`(`patient001`~`patient005`)需与种子数据保持一致

### 5.3 技术约束(Constraints)

- 本期不引入 JWT/会话,登录态仅保存在前端内存,刷新后需重新登录
- 问题数据仍为前端 JSON,患者账号迁移不影响其读取逻辑
- 前端患者 JSON(`src/data/patient-user.json`)在本期完成后不再被引用并删除
- 遵循项目提交规范与编码规范(见 `CLAUDE.md`)

### 5.4 接口契约

#### 5.4.1 注册

`POST /api/v1/patients/register`

```json
{
  "username": "patient-li-si",
  "password": "123456",
  "name": "李四",
  "birthday": "1992-04-18",
  "phone": "13800000000",
  "gender": "男"
}
```

成功 `201 Created`:

```json
{
  "patient": {
    "id": "patienta1b2c3d4",
    "username": "patient-li-si",
    "name": "李四",
    "birthday": "1992-04-18",
    "phone": "13800000000",
    "gender": "男"
  }
}
```

错误:`400 VALIDATION_ERROR`(字段校验)、`409 CONFLICT`(用户名已存在)。

#### 5.4.2 登录

`POST /api/v1/patients/login`

```json
{ "username": "patient-zhao-ming", "password": "123456" }
```

成功 `200 OK`:返回同上 `{ "patient": { ... } }`。
错误:`400 VALIDATION_ERROR`、`401 UNAUTHORIZED`。

#### 5.4.3 数据表

```sql
CREATE TABLE patient (
  id            VARCHAR(32)  PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  name          VARCHAR(64)  NOT NULL,
  birthday      DATE         NOT NULL,
  phone         VARCHAR(32)  NOT NULL DEFAULT '',
  gender        VARCHAR(16)  NOT NULL DEFAULT '',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_patient_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 6. 成功标准(Success Criteria)

- [ ] `./mvnw test` 退出码为 0,新增患者接口测试全部通过
- [ ] curl 验证:注册成功 201、重复用户名 409、密码过短 400、登录成功 200、密码错误 401、空字段 400
- [ ] 所有响应中不含 `password` / `passwordHash` 字段
- [ ] `npm run build` 退出码为 0(含 `vue-tsc` 类型检查)
- [ ] 无头浏览器验证:注册/登录后问诊页显示「XXX 的问诊」;退出后回到登录/注册页
- [ ] 5 位种子患者可用预置账号登录,并能看到各自历史问题
- [ ] 前端代码中不再引用 `src/data/patient-user.json`

## 7. 任务拆分(Task Breakdown)

### 7.1 拆分原则

- 每个任务 1–2 人时(约等于 AI 5–10 分钟),可独立验证
- 后端先行、前端后接,测试与文档随任务同步
- 任务总数 10 个,满足 ≤ 10 的限制

### 7.2 任务清单

| Task ID | 任务名称 | 描述 | 依赖 | 验收标准 / 验证命令 | 工作量 | 状态 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| TASK-001 | 患者表迁移与种子数据 | 新增 Flyway `V3` 建表、`V4` 种子 5 位患者(用户名见 10.3,bcrypt 密码) | 无 | 启动后 `SHOW TABLES` 含 `patient`;`SELECT COUNT(*)` = 5 | 1h | DONE |
| TASK-002 | 患者实体与数据访问 | `Patient` 实体、`PatientRepository`(`findByUsername`、`existsByUsername`)、`PatientResponse` DTO | TASK-001 | `./mvnw clean compile` 通过 | 1h | DONE |
| TASK-003 | 注册接口 | `PatientRegisterRequest` 校验、`PatientService.register`(唯一性、bcrypt、事务)、`POST /api/v1/patients/register` | TASK-002 | curl 注册 201;重复用户名 409;密码<6 返回 400 | 1.5h | DONE |
| TASK-004 | 登录接口 | `PatientService.login`、`POST /api/v1/patients/login` | TASK-002 | curl 登录 200;错误密码 401;响应不含密码 | 1h | DONE |
| TASK-005 | 后端自动化测试 | `PatientApiTest`:注册/登录/重复/错误密码/参数校验/种子账号 | TASK-003、TASK-004 | `./mvnw test` 全部通过 | 1.5h | DONE |
| TASK-006 | 前端 API 层 | `src/api/patients.ts`(注册、登录),复用 `http.ts` | TASK-003、TASK-004 | `npm run build` 通过 | 0.5h | DONE |
| TASK-007 | store 接入 | `Patient` 接口增加 `username`;新增 `registerPatient`/`loginPatient`;移除 `verifyPatient` 与患者 JSON 导入 | TASK-006 | 类型检查通过;登录后 `currentPatient` 正确 | 1h | DONE |
| TASK-008 | 问诊页登录/注册表单 | `Consultation.vue` 用「登录 / 注册」Tab 替换姓名+生日表单,接入接口与错误提示 | TASK-007 | 页面可注册、登录、退出;错误提示正确 | 2h | DONE |
| TASK-009 | 前后端联调与端到端验证 | 真实 MySQL 环境联调,无头浏览器验证登录/注册/历史问题 | TASK-008 | 成功标准中的页面验证全部通过 | 1h | DONE |
| TASK-010 | 文档与测试记录 | 更新 `README-Intr.md`、API 设计文档、`docs/prompts.md`,补充 curl 测试说明 | TASK-009 | 文档与实现一致 | 0.5h | DONE |

### 7.3 任务状态

| 状态 | 含义 |
| ---- | ---- |
| TODO | 待开始 |
| IN PROGRESS | 进行中 |
| DONE | 已完成并通过验证 |
| BLOCKED | 被阻塞(需注明原因) |
| CANCELLED | 已取消 |

### 7.4 子特性拆分(任务数 > 10 时)

本期任务数为 10,无需拆分。若执行中新增需求导致任务数超过 10,按以下方案拆分:

| 子特性 ID | 名称 | 包含任务 | 说明 |
| ---- | ---- | ---- | ---- |
| FEAT-001-A | 后端患者账号与认证 | TASK-001 ~ TASK-005 | 数据库、实体、注册/登录接口与测试 |
| FEAT-001-B | 前端登录注册接入 | TASK-006 ~ TASK-009 | API 层、store、问诊页表单与联调 |
| FEAT-001-C | 文档与验收 | TASK-010 及后续 | 文档、接口测试记录、验收 |

## 8. 实现说明(Implementation Notes)

- **复用现有模式**:后端参照 `server/qa-service-user/src/main/java/com/leansofx/qaserviceuser/` 下的 `DoctorController`、`DoctorService`、`DoctorRepository`、`dto/DoctorResponse`、`exception/ApiException`、`config/PasswordConfig`;错误响应由 `GlobalExceptionHandler` 统一输出
- **ID 生成**:新注册患者 ID 采用 `patient` + 8 位 UUID 十六进制(如 `patienta1b2c3d4`),与既有 `patient001` 风格兼容
- **用户名规则**:`[a-zA-Z0-9_-]{4,32}`,不区分大小写存储?本期按原样存储、精确匹配;如需忽略大小写,后续增加 `LOWER(username)` 唯一索引
- **种子账号**:保留 `patient001`~`patient005` 的原始 ID、姓名、生日、手机号、性别,补充用户名与 bcrypt 密码(10.3)
- **前端 store**:`Patient` 接口增加 `username`;移除 `state.patients` 与 `verifyPatient`;`currentPatient` 仅来自登录/注册接口
- **问诊页**:`Consultation.vue` 的 `auth-section` 改为 `a-tabs`(登录/注册),登录表单字段为用户名/密码,注册表单为用户名/密码/姓名/生日(必填)、手机号/性别(选填);日期使用 dayjs `YYYY-MM-DD`;成功后展示原问诊面板,「切换用户」调用 `logoutPatient`
- **数据清理**:前端 `src/data/patient-user.json` 在本期删除,历史问题 JSON 中的 `patientId` 引用保持不变
- **测试环境**:后端测试使用 H2(与医生接口测试一致),真实 MySQL 联调通过 `192.168.88.104` 完成

## 9. 风险与缓解(Risks and Mitigations)

| 风险 | 影响 | 缓解措施 |
| ---- | ---- | ---- |
| 既有问题 JSON 依赖 `patientId`,迁移后 ID 不一致导致历史问题丢失 | High | 种子数据保留原 ID(`patient001`~`patient005`),并在测试中验证 |
| 无登录限流,存在暴力破解风险 | Medium | 本期通过 bcrypt 增加成本;在 PRD 中记录后续限流/锁定增强项 |
| 刷新后登录态丢失 | Medium | 明确为本期范围(内存会话),后续可引入 JWT/localStorage |
| 用户名重复并发注册 | Medium | 数据库唯一索引 + 事务内校验,捕获唯一约束异常返回 409 |
| 密码策略过弱(仅长度 ≥6) | Medium | 按需求保持简单;后续可增加复杂度校验 |
| 前端删除患者 JSON 影响其他页面 | Low | 全局检索引用,仅 `store.verifyPatient` 使用;联调时回归问诊页 |

## 10. 附录(Appendix)

### 10.1 参考(References)

- 面试需求:[README.md](../README.md)
- PRD 模板:[PRD_TEMPLATE.md](./PRD_TEMPLATE.md)
- 医生数据与 API 设计:[doctor-data-api-design.md](./doctor-data-api-design.md)
- 开发规范:[server-coding-standards.md](./server-coding-standards.md)、[web-coding-standards.md](./web-coding-standards.md)
- 现有实现:`server/qa-service-user` 医生接口、`web/qa-web/src/views/Consultation.vue`

### 10.2 术语(Glossary)

- **问诊用户 / 患者**:在平台提交健康问题并查看医生回复的用户
- **bcrypt**:带盐的单向密码哈希算法,本项目 rounds=10
- **种子数据**:数据库首次初始化时写入的示例/既有业务数据
- **内存会话**:登录状态仅保存在前端运行时内存,刷新页面即失效

### 10.3 种子患者账号

| patientId | username | 姓名 | 生日 | 密码 |
| ---- | ---- | ---- | ---- | ---- |
| patient001 | patient-zhao-ming | 赵明 | 1985-03-15 | 123456 |
| patient002 | patient-sun-li | 孙丽 | 1990-07-22 | 123456 |
| patient003 | patient-zhou-jie | 周杰 | 1978-11-08 | 123456 |
| patient004 | patient-wu-fang | 吴芳 | 1995-05-20 | 123456 |
| patient005 | patient-zheng-hao | 郑浩 | 1988-09-12 | 123456 |

### 10.4 错误码

| 错误码 | HTTP 状态码 | 场景 |
| ---- | ---- | ---- |
| `VALIDATION_ERROR` | 400 | 必填字段缺失、密码长度不足、生日格式错误 |
| `UNAUTHORIZED` | 401 | 用户名或密码错误 |
| `CONFLICT` | 409 | 用户名已存在 |
| `INTERNAL_ERROR` | 500 | 服务端异常 |
