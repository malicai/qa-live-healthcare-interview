# QA Live Healthcare 项目介绍与启动指南

## 1. 项目简介

QA Live Healthcare 是一个在线医疗问诊平台,连接医生与患者,提供在线咨询、问题提交与医生解答等能力。项目包含患者端与医生端两个使用视角,当前仓库为该平台的基础框架及面试任务载体。

当前实现状态:

- 前端为完整的 Vue 3 单页应用;**医生与患者账号数据已接入后端 API(MySQL)**,问题数据仍来自本地 JSON 文件
- 后端为两个 Spring Boot 服务:`qa-service-user`(用户服务,已提供医生数据 API 与 MySQL 持久化)、`qa-service-question`(问题服务,待开发);`qa-service-statistic` 为占位空目录
- 面试任务要求在此基础上完成:首页中英文切换、医生数据迁移至 MySQL、用户名密码登录及相关文档与测试(详见根目录 [README.md](./README.md))

## 2. 技术栈

| 层次 | 技术 | 版本 |
| ---- | ---- | ---- |
| 前端框架 | Vue | 3.5 |
| 前端语言 | TypeScript | 5.5 |
| 构建工具 | Vite | 5.4 |
| UI 组件库 | Ant Design Vue | 4.2 |
| 路由 | vue-router | 4.6 |
| 日期处理 | dayjs | 1.11 |
| 后端框架 | Spring Boot | 3.5.7 |
| 后端语言 | Java | 17 |
| 构建工具 | Maven Wrapper(mvnw) | 各服务自带 |
| 数据库(待接入) | MySQL + phpMyAdmin(面试需求) | - |

## 3. 目录结构

```
qa-live-healthcare-interview/
├── web/qa-web/                 # 前端 Vue 3 应用
│   ├── src/
│   │   ├── views/              # 页面组件(Home、Doctors、Consultation、DoctorLogin、DoctorRoom、About)
│   │   ├── components/         # 公共组件(AppHeader、AppFooter)
│   │   ├── store/index.ts      # 全局状态(基于 reactive 的内存 store)
│   │   ├── data/               # 当前数据源(医生/患者/问题 JSON)
│   │   └── router/index.ts     # 路由定义
│   ├── vite.config.ts
│   └── package.json
├── server/
│   ├── qa-service-user/        # 用户服务,端口 8080
│   ├── qa-service-question/    # 问题服务,端口 8081
│   └── qa-service-statistic/   # 统计服务(占位)
├── docs/                       # 交付文档(prompts.md 等)
├── .asdm/toolsets/             # ASDM 工具集(PRD 构建、上下文构建、基础工具)
├── _TRAINING_ASSETS/           # 培训素材(后端服务源码 zip)
└── README.md                   # 面试要求
```

## 4. 页面与功能

| 路由 | 页面 | 说明 |
| ---- | ---- | ---- |
| `/` | 首页 | 平台介绍、统计数据、开放诊室列表 |
| `/consultation` | 患者问诊 | 姓名 + 生日验证身份,提交问题、查看医生回复 |
| `/consultation/:doctorUsername` | 患者问诊 | 通过医生用户名直达对应诊室 |
| `/doctors` | 医生团队 | 展示全部医生及在线状态,在线医生可进入诊室 |
| `/about` | 关于我们 | 平台简介、特色与服务流程 |
| `/doctor/login` | 医生登录 | 用户名 + 密码登录 |
| `/doctor/room/:username` | 医生诊室 | 查看待响应问题,文字回复或标记已解答 |

**数据流**:医生数据由 `src/api/doctors.ts` 调用后端 `/api/v1/doctors`(MySQL),应用启动时经 `src/store/index.ts` 加载;患者注册/登录由 `src/api/patients.ts` 调用 `/api/v1/patients/*`,账号存 MySQL;问题数据仍来自 `src/data/question-list.json`。各页面通过 `computed` 读取 store 展示。医生与患者登录均调用后端接口;提交问题、回复问题仍只在浏览器内存中完成,刷新页面后重置。

## 5. 启动指南

### 5.1 环境要求

| 工具 | 要求 | 说明 |
| ---- | ---- | ---- |
| Node.js | ≥ 18 | Vite 5 要求;本机 v24.10.0 可用 |
| npm | 随 Node.js 安装 | 本机 11.6.1 可用 |
| JDK | **17** | Spring Boot 3.5 要求;本机默认 Java 为 11,启动后端前需安装并切换 JDK 17 |
| Maven | 无需单独安装 | 各服务自带 Maven Wrapper(mvnw) |
| Docker | 需要(启动 MySQL) | 通过根目录 `docker-compose.yml` 启动 MySQL 8 与 phpMyAdmin;本机当前未安装 |

### 5.2 启动前端

```bash
cd web/qa-web
npm install
npm run dev
```

启动后访问 http://localhost:5173。

其他命令:

```bash
npm run build     # 类型检查 + 生产构建(输出 dist/)
npm run preview   # 预览生产构建
```

### 5.3 启动数据库(MySQL)

```bash
# 在项目根目录执行
docker compose up -d
```

- MySQL:`localhost:3306`,数据库 `qa_healthcare`,应用账号 `qa_user / qa_password`(root 密码 `root_password`)
- phpMyAdmin:http://localhost:8082
- 首次启动时 Flyway 自动创建 `doctor`、`doctor_specialty` 表并写入 5 条医生数据

`qa-service-user` 默认使用上述连接配置,也可通过 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 环境变量覆盖。

### 5.4 启动后端

`qa-service-user`(端口 8080):

```bash
cd server/qa-service-user

# Windows(cmd / PowerShell)
mvnw.cmd spring-boot:run

# macOS / Linux / Git Bash
./mvnw spring-boot:run
```

`qa-service-question`(端口 8081):进入 `server/qa-service-question` 执行同样命令。

启动后可验证:

```bash
# 用户服务 CORS 测试接口
curl http://localhost:8080/api/test/cors

# Actuator 健康检查
curl http://localhost:8080/actuator/health

# 医生列表(分页,可按在线状态筛选)
curl "http://localhost:8080/api/v1/doctors?page=1&size=20&isActive=true"

# 按 ID / 用户名查询医生
curl http://localhost:8080/api/v1/doctors/doc001
curl http://localhost:8080/api/v1/doctors/username/dr-zhang-wei

# 医生登录
curl -X POST http://localhost:8080/api/v1/doctors/login \
  -H "Content-Type: application/json" \
  -d '{"username":"dr-zhang-wei","password":"123456"}'

# 患者登录 / 注册
curl -X POST http://localhost:8080/api/v1/patients/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patient-zhao-ming","password":"123456"}'

curl -X POST http://localhost:8080/api/v1/patients/register \
  -H "Content-Type: application/json" \
  -d '{"username":"patient-li-si","password":"123456","name":"李四","birthday":"1992-04-18"}'
```

> 前端医生数据已调用上述接口(地址由 `web/qa-web/.env` 的 `VITE_API_BASE_URL` 配置,默认 `http://localhost:8080`);启动前端前需先启动后端。患者/问题数据仍为本地 JSON。

### 5.5 常见问题

- **后端启动报 Java 版本错误**(如 `UnsupportedClassVersionError`):默认 `java` 为 11,请安装 JDK 17 并将 `JAVA_HOME` 指向 JDK 17
- **端口被占用**:8080 / 8081 分别由各服务的 `src/main/resources/application.properties` 中 `server.port` 配置,可自行修改
- **前端依赖安装慢**:可配置 npm 镜像源(如 `npm config set registry https://registry.npmmirror.com`)
- **刷新页面后数据丢失**:当前为内存 store,属预期行为,后端接入后由数据库持久化

## 6. 测试账号

- **医生端**:用户名 `dr-zhang-wei`,密码 `123456`;其余医生账号见 `web/qa-web/src/data/doctor-user-list.json`
- **患者端**:使用用户名 + 密码登录或注册;种子账号 `patient-zhao-ming` / `123456`(其余账号见 `docs/patient-password-login-PRD.md` 附录 10.3)

## 7. 面试任务概览

根目录 [README.md](./README.md) 定义了本次面试要求,核心需求:

1. **首页国际化**:右上角添加语言切换下拉菜单(中文 / English),语言资源放在 `web/qa-web/src/locales`,仅处理首页
2. **医生数据入库**:使用 docker-compose 创建 MySQL 并挂接 phpMyAdmin,在 `qa-service-user` 中提供医生列表 API,完成前后端联调
3. **用户名密码登录**:编写 PRD 模板与 PRD 文档,并指导 AI 按 PRD 完成开发
4. **测试与交付**:使用 curl 覆盖接口测试,产出 `docs/API_TEST.md` 与测试报告;在 `docs/prompts.md` 记录全部 AI 提示词;创建特性分支并向 main 提交 PR
