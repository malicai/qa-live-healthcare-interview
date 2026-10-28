# 医生数据清单与后端 API 接口设计

> 数据来源:`web/qa-web/src/data/doctor-user-list.json`(当前前端静态数据)
> 目标服务:`server/qa-service-user`(端口 8080)
> 遵循规范:[server-coding-standards.md](./server-coding-standards.md)(`/api/v1` 版本化、统一错误格式、密码只存哈希、查询接口分页)

## 1. 数据现状

- 医生数据共 **5 条**,以 JSON 数组存储于前端项目,由 `src/store/index.ts` 加载为内存状态
- 使用页面:`Home.vue`(开放诊室)、`Doctors.vue`(医生团队)、`Consultation.vue`(选择医生/按用户名进入诊室)、`DoctorLogin.vue`(登录)、`DoctorRoom.vue`(医生诊室)
- 数据为**中英双语字段**(第 10 次迭代新增英文字段),API 应完整返回双语数据,由前端按语言选择展示
- `password` 当前为明文 `123456`,迁移数据库时**必须改为 bcrypt 哈希存储**,API 响应**不得返回密码字段**

## 2. 医生数据清单

### 2.1 记录清单

| ID | 用户名(username) | 姓名 | 职称 | 科室 | 在线(isActive) |
| ---- | ---- | ---- | ---- | ---- | ---- |
| doc001 | dr-zhang-wei | 张伟医生 | 主任医师 | 心内科 | 是 |
| doc002 | dr-li-na | 李娜医生 | 副主任医师 | 儿科 | 是 |
| doc003 | dr-wang-qiang | 王强医生 | 主治医师 | 骨科 | 是 |
| doc004 | dr-liu-min | 刘敏医生 | 主任医师 | 妇产科 | 否 |
| doc005 | dr-chen-jie | 陈杰医生 | 副主任医师 | 消化内科 | 是 |

### 2.2 字段清单(数据字典)

| 字段 | 类型 | 必填 | 唯一 | 说明 | 示例 |
| ---- | ---- | ---- | ---- | ---- | ---- |
| id | string | 是 | 是 | 医生 ID,主键 | `doc001` |
| username | string | 是 | 是 | 登录用户名,小写字母/连字符 | `dr-zhang-wei` |
| password | string | 是 | 否 | 登录密码;入库为 bcrypt 哈希,API 不返回 | `123456`(仅现状) |
| name | string | 是 | 否 | 中文姓名 | `张伟医生` |
| nameEn | string | 是 | 否 | 英文姓名 | `Dr. Zhang Wei` |
| title | string | 是 | 否 | 中文职称 | `主任医师` |
| titleEn | string | 是 | 否 | 英文职称 | `Chief Physician` |
| department | string | 是 | 否 | 中文科室 | `心内科` |
| departmentEn | string | 是 | 否 | 英文科室 | `Cardiology` |
| avatar | string | 是 | 否 | 头像 URL | `https://...jpeg` |
| experience | string | 是 | 否 | 中文从业经验描述 | `15年临床经验` |
| experienceEn | string | 是 | 否 | 英文从业经验描述 | `15 years of clinical experience` |
| specialties | string[] | 是 | 否 | 中文擅长领域,至少 1 项 | `["高血压","冠心病"]` |
| specialtiesEn | string[] | 是 | 否 | 英文擅长领域,与中文一一对应 | `["Hypertension", ...]` |
| isActive | boolean | 是 | 否 | 是否在线/开放诊室 | `true` |

### 2.3 校验规则

- `id`、`username` 全局唯一;`username` 仅允许小写字母、数字与连字符
- `password` 长度 ≥ 6;入库前 bcrypt(salt rounds 10)哈希
- `name`、`title`、`department`、`avatar`、`experience` 非空;英文对应字段同样非空
- `specialties` 与 `specialtiesEn` 数量必须一致且至少 1 项
- `isActive` 缺省为 `true`
- `avatar` 必须为合法 URL

### 2.4 数据关系

- 医生 1 : N 问题(问题数据中的 `doctorId` → 医生 `id`);当前问题 JSON 中还冗余了 `doctorName`,迁移后应通过关联查询获取
- 前端通过 `username` 直达诊室路由 `/consultation/:doctorUsername`,因此需要按用户名查询的接口

### 2.5 MySQL 表设计建议

```sql
CREATE TABLE doctor (
  id            VARCHAR(32)  PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  name          VARCHAR(64)  NOT NULL,
  name_en       VARCHAR(128) NOT NULL,
  title         VARCHAR(64)  NOT NULL,
  title_en      VARCHAR(128) NOT NULL,
  department    VARCHAR(64)  NOT NULL,
  department_en VARCHAR(128) NOT NULL,
  avatar        VARCHAR(512) NOT NULL,
  experience    VARCHAR(128) NOT NULL,
  experience_en VARCHAR(255) NOT NULL,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_doctor_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctor_specialty (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  doctor_id  VARCHAR(32)  NOT NULL,
  name       VARCHAR(64)  NOT NULL,
  name_en    VARCHAR(128) NOT NULL,
  sort_order INT          NOT NULL DEFAULT 0,
  CONSTRAINT fk_specialty_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> 擅长领域使用独立表(而非 JSON 列),便于查询与后续扩展;`id` 保留字符串形式以兼容现有前端数据与路由。

## 3. 后端 API 拆分

### 3.1 拆分原则

- 以医生资源为中心:`/api/v1/doctors`
- 按前端实际使用场景拆分,不做当前无消费方的管理类接口(增删改留作后续扩展)
- 响应使用 `DoctorResponse` DTO,**不包含密码/哈希**
- 列表接口支持分页与在线状态筛选(遵循“查询接口必须分页或明确数量上限”规范)
- 错误响应统一为:

```json
{
  "error": { "code": "NOT_FOUND", "message": "医生不存在", "details": {} },
  "timestamp": "2026-10-28T12:00:00Z",
  "requestId": "..."
}
```

### 3.2 接口清单(总览)

| 序号 | 方法 | 路径 | 说明 | 消费页面 |
| ---- | ---- | ---- | ---- | ---- |
| 1 | GET | `/api/v1/doctors` | 分页查询医生列表,支持 `isActive` 筛选 | Doctors、Home、Consultation |
| 2 | GET | `/api/v1/doctors/{id}` | 按 ID 查询医生详情 | Consultation、DoctorRoom |
| 3 | GET | `/api/v1/doctors/username/{username}` | 按用户名查询医生详情 | Consultation(路由直达) |
| 4 | POST | `/api/v1/doctors/login` | 医生登录(用户名 + 密码) | DoctorLogin |

> 预留(本期不实现):`POST /api/v1/doctors`、`PUT /api/v1/doctors/{id}`、`DELETE /api/v1/doctors/{id}`(管理端维护医生数据)。

### 3.3 统一响应 DTO

```json
{
  "id": "doc001",
  "username": "dr-zhang-wei",
  "name": "张伟医生",
  "nameEn": "Dr. Zhang Wei",
  "title": "主任医师",
  "titleEn": "Chief Physician",
  "department": "心内科",
  "departmentEn": "Cardiology",
  "avatar": "https://images.pexels.com/.../5215024.jpeg",
  "experience": "15年临床经验",
  "experienceEn": "15 years of clinical experience",
  "specialties": ["高血压", "冠心病", "心律失常"],
  "specialtiesEn": ["Hypertension", "Coronary Heart Disease", "Arrhythmia"],
  "isActive": true
}
```

### 3.4 接口详情

#### 1) GET `/api/v1/doctors`

查询参数:

| 参数 | 类型 | 必填 | 默认 | 说明 |
| ---- | ---- | ---- | ---- | ---- |
| page | int | 否 | 1 | 页码,≥1 |
| size | int | 否 | 20 | 每页条数,1–100 |
| isActive | boolean | 否 | - | 按在线状态筛选;不传返回全部 |

成功响应 `200`:

```json
{
  "items": [ { "id": "doc001", "...": "..." } ],
  "total": 5,
  "page": 1,
  "size": 20
}
```

错误:`VALIDATION_ERROR`(参数非法)。

#### 2) GET `/api/v1/doctors/{id}`

- 路径参数:`id`(string,必填)
- 成功响应 `200`:DoctorResponse
- 错误:`NOT_FOUND`(医生不存在)

#### 3) GET `/api/v1/doctors/username/{username}`

- 路径参数:`username`(string,必填)
- 成功响应 `200`:DoctorResponse
- 错误:`NOT_FOUND`(用户名不存在)

#### 4) POST `/api/v1/doctors/login`

请求体:

```json
{ "username": "dr-zhang-wei", "password": "123456" }
```

成功响应 `200`:医生信息(不含密码)

```json
{
  "doctor": {
    "id": "doc001",
    "username": "dr-zhang-wei",
    "name": "张伟医生",
    "...": "..."
  }
}
```

错误:

| 错误码 | 状态码 | 场景 |
| ---- | ---- | ---- |
| `VALIDATION_ERROR` | 400 | 用户名或密码为空 |
| `UNAUTHORIZED` | 401 | 用户名或密码错误 |

> 本期登录仅返回医生信息(当前前端无鉴权体系);后续如需会话/权限,再引入 JWT 与 `Authorization: Bearer` 机制。

### 3.5 前端页面与接口映射

| 页面 | 接口调用 |
| ---- | ---- |
| Home(开放诊室 + 统计) | `GET /api/v1/doctors?size=100`,前端按 `isActive` 过滤卡片,并用 `total`/筛选项计算统计 |
| Doctors(医生团队) | `GET /api/v1/doctors?page=1&size=20` |
| Consultation(选择医生) | `GET /api/v1/doctors?isActive=true&size=100` |
| Consultation(路由 `/consultation/:doctorUsername`) | `GET /api/v1/doctors/username/{username}` |
| DoctorLogin | `POST /api/v1/doctors/login` |
| DoctorRoom | 登录响应中的医生信息(刷新时可用 `GET /api/v1/doctors/username/{username}`) |

### 3.6 后端实现位置建议(qa-service-user)

```
com.leansofx.qaserviceuser
├── controller/DoctorController.java
├── service/DoctorService.java
├── repository/DoctorRepository.java
├── model/Doctor.java              # 实体(含 specialties 关联)
├── dto/DoctorResponse.java
├── dto/DoctorLoginRequest.java
└── config/...                     # 既有 CORS 配置
```

- 数据访问建议 Spring Data JPA(或 MyBatis),实体字段与第 2.5 节表结构对应
- 分页使用 `Pageable`,响应封装为 `{ items, total, page, size }`
- 密码校验使用 `BCryptPasswordEncoder.matches`
- 初始化数据通过 Flyway 迁移脚本或 `data.sql` 导入 5 条医生记录

## 4. 后续实现步骤(对应面试需求 2)

1. 编写 `docker-compose.yml`(MySQL + phpMyAdmin)
2. 创建数据库表结构与初始化数据(Flyway 迁移)
3. 实现第 3.2 节的 4 个接口
4. 前端 store 改为调用 API(替换 JSON 导入)
5. 使用 curl 编写接口测试与测试报告
