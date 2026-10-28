# API 接口测试流程与 AI 执行指南

> 本文档描述本项目中医生、患者相关接口的 curl 测试流程,并指导 AI 按此流程执行测试并生成测试报告。
> 对应面试要求:「使用 curl 构建对于以上需求中所涉及的所有接口的测试」「提供 /docs/API_TEST.md 用于描述以上接口测试流程,并指导AI使用这份文档完成测试并生成测试报告」。

## 1. 测试目标

验证需求 2(医生数据 MySQL 化 + 医生列表 API)与需求 3(患者用户名密码登录)所涉及的全部接口,覆盖:

- 正常路径:查询、分页、筛选、登录、注册
- 异常路径:参数校验失败、资源不存在、认证失败、用户名冲突、请求体格式错误
- 安全约束:响应中不出现密码或密码哈希

## 2. 测试范围

| 服务 | 方法 | 路径 | 说明 |
| ---- | ---- | ---- | ---- |
| qa-service-user | GET | `/api/v1/doctors` | 医生分页列表,支持 `isActive` 筛选 |
| qa-service-user | GET | `/api/v1/doctors/{id}` | 按 ID 查询医生 |
| qa-service-user | GET | `/api/v1/doctors/username/{username}` | 按用户名查询医生 |
| qa-service-user | POST | `/api/v1/doctors/login` | 医生登录 |
| qa-service-user | POST | `/api/v1/patients/register` | 患者注册 |
| qa-service-user | POST | `/api/v1/patients/login` | 患者登录 |
| qa-service-user | GET | `/actuator/health` | 测试前置健康检查 |

## 3. 测试环境与前置条件

| 项目 | 要求 |
| ---- | ---- |
| 后端服务 | `qa-service-user` 已启动,默认 `http://localhost:8080` |
| 数据库 | MySQL 8 已启动(远程 `192.168.88.104:3306` 或本地 Docker Compose) |
| 数据 | Flyway 已执行 V1–V4,医生 5 条、患者种子 5 条 |
| 工具 | `curl`、`bash`(Git Bash / Linux / macOS)、Python 3(解析 JSON 断言) |
| 测试账号 | 医生 `dr-zhang-wei / 123456`;患者 `patient-zhao-ming / 123456` |

**启动检查:**

```bash
curl -s http://localhost:8080/actuator/health
# 期望: {"status":"UP", ...}
```

若后端不在 8080,可通过 `API_BASE_URL` 指定:

```bash
API_BASE_URL=http://192.168.88.104:8080 bash docs/api-test.sh
```

## 4. 测试用例清单

### 4.1 医生接口(12 条)

| 用例 ID | 场景 | 方法 / 路径 | 期望状态码 | 关键断言 |
| ---- | ---- | ---- | ---- | ---- |
| D01 | 医生列表默认分页 | GET `/api/v1/doctors` | 200 | total=5, items 长度=5, 无 passwordHash |
| D02 | 仅查询在线医生 | GET `/api/v1/doctors?isActive=true` | 200 | total=4, 所有 isActive=true |
| D03 | 分页 page=2,size=2 | GET `/api/v1/doctors?page=2&size=2` | 200 | page=2, size=2, items=2, total=5 |
| D04 | 页码非法 page=0 | GET `/api/v1/doctors?page=0` | 400 | code=VALIDATION_ERROR |
| D05 | 分页过大 size=101 | GET `/api/v1/doctors?size=101` | 400 | code=VALIDATION_ERROR |
| D06 | 按 ID 查询 | GET `/api/v1/doctors/doc001` | 200 | id=doc001, name=张伟医生, 无 passwordHash |
| D07 | 医生不存在 | GET `/api/v1/doctors/doc999` | 404 | code=NOT_FOUND |
| D08 | 按用户名查询 | GET `/api/v1/doctors/username/dr-zhang-wei` | 200 | username=dr-zhang-wei |
| D09 | 用户名不存在 | GET `/api/v1/doctors/username/dr-nobody` | 404 | code=NOT_FOUND |
| D10 | 医生登录成功 | POST `/api/v1/doctors/login` | 200 | doctor.id=doc001, 无 passwordHash |
| D11 | 医生登录密码错误 | POST `/api/v1/doctors/login` | 401 | code=UNAUTHORIZED |
| D12 | 医生登录参数为空 | POST `/api/v1/doctors/login` | 400 | code=VALIDATION_ERROR |

### 4.2 患者接口(10 条)

| 用例 ID | 场景 | 方法 / 路径 | 期望状态码 | 关键断言 |
| ---- | ---- | ---- | ---- | ---- |
| P01 | 种子患者登录 | POST `/api/v1/patients/login` | 200 | patient.id=patient001, 无 passwordHash |
| P02 | 患者注册成功 | POST `/api/v1/patients/register` | 201 | username=测试账号, 无 password/passwordHash |
| P03 | 用户名重复 | POST `/api/v1/patients/register` | 409 | code=CONFLICT |
| P04 | 密码长度不足 | POST `/api/v1/patients/register` | 400 | details 含 password |
| P05 | 用户名格式非法 | POST `/api/v1/patients/register` | 400 | details 含 username |
| P06 | 缺少生日 | POST `/api/v1/patients/register` | 400 | details 含 birthday |
| P07 | 新患者登录 | POST `/api/v1/patients/login` | 200 | username=测试账号 |
| P08 | 患者密码错误 | POST `/api/v1/patients/login` | 401 | code=UNAUTHORIZED |
| P09 | 患者用户名不存在 | POST `/api/v1/patients/login` | 401 | code=UNAUTHORIZED |
| P10 | 请求体格式非法 | POST `/api/v1/patients/login` | 400 | code=VALIDATION_ERROR |

> 注册用例使用时间戳生成唯一用户名(如 `patient-test-20261028180000`),避免重复执行时冲突。

## 5. 执行方式

在项目根目录执行:

```bash
bash docs/api-test.sh
```

指定其他后端地址:

```bash
API_BASE_URL=http://localhost:8082 bash docs/api-test.sh
```

脚本行为:

- 先检查 `/actuator/health`,不通过则退出(退出码 2)
- 逐条执行用例,输出 `[PASS]` / `[FAIL]`,失败时打印响应体前 300 字符
- 结束输出通过/失败/总数,并给出测试账号清理 SQL
- 全部通过时退出码 0,否则非 0

## 6. AI 执行指南

AI 按以下步骤使用本文档完成测试并生成报告:

1. **读取上下文**:阅读本文档与 `docs/patient-password-login-PRD.md`、`docs/doctor-data-api-design.md`,确认接口契约
2. **检查环境**:确认 MySQL 容器运行、后端已启动;执行 `curl -s http://localhost:8080/actuator/health`
3. **执行测试**:运行 `bash docs/api-test.sh`,完整保留终端输出(含每条用例状态)
4. **分析结果**:
   - 全部 PASS → 进入报告生成
   - 存在 FAIL → 区分「环境问题(服务未启动/数据库不可用)」与「代码缺陷」;环境问题修复后重跑
5. **生成报告**:使用第 8 节模板创建 `docs/TEST_REPORT.md`,逐条填写实际状态码与断言结果,统计通过率,记录缺陷与结论
6. **清理数据**:执行附录中的清理 SQL,删除本次测试创建的患者账号
7. **交付**:报告与脚本一并提交;报告中注明执行时间、环境、执行人(AI 或人工)

**可直接使用的提示词:**

```text
请阅读 docs/API_TEST.md,按文档要求执行 curl 接口测试:
1. 确认后端健康检查通过
2. 运行 docs/api-test.sh 并保存完整输出
3. 按文档第 8 节模板生成 docs/TEST_REPORT.md,包含环境、逐条用例结果、缺陷与结论
4. 执行附录中的清理 SQL 删除测试账号
5. 如有失败用例,请先判断是环境问题还是代码缺陷,并给出定位信息
```

## 7. 通过标准与缺陷判定

| 判定 | 条件 |
| ---- | ---- |
| 通过 | 用例状态码与关键断言全部符合预期,且响应不含密码字段 |
| 环境问题 | 健康检查失败、连接被拒绝、数据库不可用;修复环境后重跑,不计入代码缺陷 |
| 代码缺陷 | 状态码或响应结构不符合预期;需记录实际响应、复现步骤与期望结果 |

**通过标准:全部 22 条用例 PASS,通过率 100%。**

## 8. 测试报告模板

> AI 复制以下模板生成 `docs/TEST_REPORT.md`。

```markdown
# API 接口测试报告

| 项目 | 内容 |
| ---- | ---- |
| 测试日期 | YYYY-MM-DD HH:mm:ss |
| 测试环境 | BASE_URL;MySQL 版本/地址;后端版本 |
| 执行人 | AI(模型名)/ 人工 |
| 测试脚本 | docs/api-test.sh |
| 测试用例数 | 22 |
| 通过 / 失败 | X / Y |

## 1. 测试范围

<列出本次覆盖的接口与场景>

## 2. 测试结果汇总

| 用例 ID | 场景 | 期望 | 实际 | 结果 |
| ---- | ---- | ---- | ---- | ---- |
| D01 | ... | 200 | 200 | PASS |
| ... | ... | ... | ... | ... |

## 3. 失败用例详情

<无失败时写「无」;有失败时逐条记录:实际响应、请求、期望、初步定位>

## 4. 缺陷与风险

<发现的缺陷、严重程度、建议修复方式;无则写「无」>

## 5. 测试结论

<是否通过、通过率、是否可以进入下一阶段>

## 6. 清理记录

| 项目 | 内容 |
| ---- | ---- |
| 测试账号 | patient-test-YYYYMMDDHHmmss |
| 清理方式 | DELETE FROM patient WHERE username = '...'; |
| 清理结果 | 已删除 / 未执行 |
```

## 9. 附录

### 9.1 统一错误码

| 错误码 | HTTP 状态码 | 场景 |
| ---- | ---- | ---- |
| `VALIDATION_ERROR` | 400 | 参数缺失、格式错误、分页非法、请求体非 JSON |
| `UNAUTHORIZED` | 401 | 用户名或密码错误 |
| `NOT_FOUND` | 404 | 医生/资源不存在 |
| `CONFLICT` | 409 | 用户名已存在 |
| `INTERNAL_ERROR` | 500 | 服务端异常 |

### 9.2 测试数据清理

脚本会注册一个测试患者,执行后请清理:

```sql
DELETE FROM patient WHERE username = 'patient-test-<时间戳>';
```

远程 MySQL 可通过容器执行:

```bash
docker exec qa-mysql mysql -uroot -proot_password qa_healthcare \
  -e "DELETE FROM patient WHERE username LIKE 'patient-test-%';"
```

> 注意:`patient-%` 会匹配种子账号(`patient-zhao-ming` 等),请使用 `patient-test-%` 前缀精确清理。

### 9.3 常见问题

| 问题 | 处理 |
| ---- | ---- |
| `curl: (7) Failed to connect` | 后端未启动或端口错误;检查 `API_BASE_URL` |
| 健康检查返回 503 | 数据库连接失败;检查 MySQL 容器与连接配置 |
| 注册用例返回 409 | 用户名时间戳重复(同一秒内重跑);稍后重试或手动修改用户名 |
| Windows 终端中文乱码 | 仅显示问题,不影响测试;脚本以 UTF-8 发送请求体 |
| Python 命令不存在 | 设置 `PYTHON_BIN=python3` 或安装 Python 3 |
