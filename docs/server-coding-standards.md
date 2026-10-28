# Server 端开发规范(Java / Spring Boot)

> 适用范围:`server/` 下全部服务(`qa-service-user`、`qa-service-question`、`qa-service-statistic`)。
> 规则来源:`.asdm/toolsets/context-builder/spec/standard-coding-style.md`、`standard-project-structure.md`、`api.md`、`data-models.md`、`architecture.md`,并结合本项目现有代码落地。
> 与 ASDM 通用模板冲突处以本文「规范裁定」章节为准。

## 1. 技术基线

| 项 | 约定 |
| ---- | ---- |
| JDK | Java 17(Spring Boot 3.5.x 最低要求) |
| 框架 | Spring Boot 3.5.7 |
| 构建 | Maven Wrapper(`mvnw` / `mvnw.cmd`),不依赖全局 Maven |
| 测试 | JUnit 5 + Spring Boot Test;集成测试可用 Testcontainers |
| 数据库(待接入) | MySQL;迁移工具使用 Flyway 或 Liquibase |

## 2. 命名规范

| 元素 | 规则 | 示例 |
| ---- | ---- | ---- |
| 类 / 接口 | PascalCase | `DoctorController` |
| 方法 / 变量 / 参数 | camelCase | `findDoctorByUsername` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 包名 | 全小写 | `com.leansofx.qaserviceuser.controller` |
| 目录 / 资源 | kebab-case | `qa-service-user` |
| Java 文件 | PascalCase,与类名一致 | `CorsConfig.java` |
| 测试类 | 被测类名 + `Test` 后缀 | `DoctorServiceTest.java` |
| 数据库列 | snake_case | `password_hash`、`created_at` |
| 实体字段 | camelCase | `passwordHash`、`createdAt` |
| 枚举值 | 全大写 | `PENDING`、`ANSWERED` |

## 3. 代码格式

- 缩进:**4 空格**(禁止 Tab)。
- 左大括号与语句同行,右大括号独占一行。
- 一行一条语句。
- 修饰符顺序:`public static final`;重写方法必须加 `@Override`。
- 可空性:公共方法参数/返回值使用 `@NotNull` / `@Nullable` 标注。
- 注释解释「为什么」而非「做什么」;公共 API 使用 Javadoc。

## 4. 包结构与分层

```
com.leansofx.<service>
├── controller/     # REST 入口,只做参数校验与编排
├── service/        # 业务逻辑
├── repository/     # 数据访问
├── model/          # 实体
├── dto/            # 请求/响应对象
└── config/         # 配置类(CORS、安全等)
```

- 业务逻辑不得写在 Controller 中;Controller 不直接访问 Repository。
- 嵌套目录不超过 3–4 层;偏离本结构的安排需在 README 中说明。
- 测试目录结构与 `src/main/java` 镜像。

## 5. Controller 与 REST 约定

- 使用 `@RestController` + `@RequestMapping("/api/...")`。
- **新增接口统一使用版本化前缀 `/api/v1/`**(如 `/api/v1/doctors`);既有 `/api/test/*` 保留不动。
- 新接口使用 DTO 定义请求/响应,不返回裸 `Map`;仅测试/健康类接口可使用 `Map`。
- 资源命名使用复数名词:`/api/v1/doctors`、`/api/v1/patients`。
- HTTP 方法语义:`GET` 查询、`POST` 创建、`PUT` 全量更新、`DELETE` 删除。
- 统一错误响应格式:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "医生不存在",
    "details": {}
  },
  "timestamp": "2026-10-28T12:00:00Z",
  "requestId": "..."
}
```

- 错误码约定:

| 错误码 | HTTP 状态码 |
| ---- | ---- |
| `VALIDATION_ERROR` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |
| `RATE_LIMIT_EXCEEDED` | 429 |
| `INTERNAL_ERROR` | 500 |

- 接口文档按 Controller 组织:端点表(Endpoint / Method / Description / Security / Status Codes)+ 请求参数 + 成功响应示例 + curl 示例 + 错误响应列表。
- 认证(如需要)使用 `Authorization: Bearer <jwt_token>`。
- 查询接口必须分页或明确数据量上限。

## 6. 数据与持久化

- 每个实体必须定义主键;时间字段统一用 `createdAt` / `updatedAt`(DB 列 `created_at` / `updated_at`)。
- 密码只存哈希:**bcrypt(salt rounds 10)**,严禁明文或可逆加密。
- 敏感字段(手机号等)按需脱敏,日志与 API 响应不得输出密码哈希。
- Repository 标准方法名:`findById`、`findByUsername`、`findByEmail`、`save`、`update`、`delete`。
- 表结构变更必须通过迁移脚本完成(Flyway `V<version>__<description>.sql` 或 Liquibase changelog),禁止手工改库;迁移需可回滚(含 `up()` / `down()` 或等价方案)。
- 文档同步:实体或迁移变更时,同步更新 API 文档与数据模型说明。

## 7. 配置与密钥

- 配置集中在 `src/main/resources/application.properties`。
- 端口:`qa-service-user` = 8080,`qa-service-question` = 8081,`qa-service-statistic` 待定。
- 数据库账号、密码等敏感配置使用环境变量占位(如 `${DB_PASSWORD}`),禁止写死在仓库中。
- 环境相关配置不要提交;本地配置使用 `application-local.properties` 并加入 `.gitignore`。

## 8. 异常处理与日志

- 禁止空 `catch`;捕获后必须记录日志并处理或重新抛出。
- 全局异常处理使用 `@RestControllerAdvice`,输出统一错误响应格式。
- 日志分级:debug / info / warn / error;关键日志带上下文(如 doctorId、requestId)。
- 禁止在日志中记录密码、token、身份证号等敏感信息。

## 9. 测试规范

- 框架:JUnit 5;Spring 上下文测试用 `@SpringBootTest`。
- 测试命名:`should <expected behavior> when <condition>`(如 `should return doctor when valid username is provided`)。
- 结构:Arrange–Act–Assert。
- 数据库集成测试使用 Testcontainers(`TestcontainersConfiguration` + `Test<Application>Application`)。
- 新增/修改接口必须补充对应测试;测试必须可重复运行且不依赖外部环境。

## 10. 验证命令

```bash
cd server/qa-service-user
./mvnw clean compile     # 编译检查
./mvnw test              # 单元/集成测试
```

Windows(cmd / PowerShell)使用 `mvnw.cmd clean compile`、`mvnw.cmd test`。
`qa-service-question` 同理,进入对应目录执行。

## 11. 规范裁定(ASDM 模板冲突处理)

| 冲突点 | 裁定 |
| ---- | ---- |
| Java 文件命名:模板一处写 camelCase,示例为 `UserController.java` | 采用 **PascalCase**(与 Java 通用规范及现有代码一致) |
| 目录结构:模板给出三种互不一致的分层方案 | 采用 `controller/service/repository/model/dto/config` 技术分层(与现有包结构一致) |
| API 版本化:模板元数据要求 `/api/v1`,示例端点未带版本 | 新增接口采用 **`/api/v1/`**,既有 `/api/test/*` 保留 |
| 提交类型清单:7 种 / 9 种不一致 | 采用 **9 种**(含 `ci`、`perf`),见 `git-commit-message-spec.md` |
| 缩进:现有 Spring Initializr 生成文件使用 Tab | 统一为 **4 空格**;修改既有文件时顺带规范化 |

## 12. 规范来源

- `.asdm/toolsets/context-builder/spec/standard-coding-style.md`(Java 命名/格式/通用模式/测试)
- `.asdm/toolsets/context-builder/spec/standard-project-structure.md`(Java 变体结构)
- `.asdm/toolsets/context-builder/spec/api.md`(REST、错误格式、错误码、版本化)
- `.asdm/toolsets/context-builder/spec/data-models.md`(字段命名、Repository、密码哈希)
- `.asdm/toolsets/context-builder/spec/architecture.md`(迁移工具、安全)
- `.asdm/toolsets/basic-tools/spec/git-commit-spec.md`、`git-commit-message-spec.md`(提交规范)
