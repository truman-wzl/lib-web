# lib-web

图书管理系统（课程设计）实践项目：**Spring Boot + Oracle + 静态前端页面**。

## 技术栈
- **后端**：Spring Boot 3、Java 17、Spring Web、Spring Data JPA、Spring Mail、Scheduling
- **数据库**：Oracle（`jdbc:oracle:thin`）
- **构建**：Maven（含 `mvnw` / `mvnw.cmd`）
- **前端**：`src/main/resources/static/`（HTML + 原生 JS，页面直接由后端静态资源提供）

## 主要功能（概览）
- **用户/认证**：登录、注册
- **图书管理**：增删改查、分类等
- **借阅管理**：借阅/归还、借阅记录
- **消息/通知**：站内消息、邮件发送（基于 QQ SMTP）
- **统计与定时任务**：统计接口、定时检查（如逾期检查）

> 具体页面入口见：`src/main/resources/static/index.html`（以及 `login.html` / `register.html`）。

## 本地运行

### 1) 环境要求
- JDK **17**
- Maven（可选，项目自带 Maven Wrapper，推荐直接用 `mvnw`）
- Oracle 数据库可访问（本地或远程）

### 2) 配置文件
本项目运行依赖 `src/main/resources/application.properties`。

仓库中提供了示例模板：
- `src/main/resources/application.example.properties`

请按下面步骤准备你本机的真实配置：
1. 复制 `application.example.properties` 为 `application.properties`
2. 在 `application.properties` 中填写你的真实配置（数据库账号密码、邮箱 SMTP 授权码等）

> 说明：为避免把密码提交到仓库，`.gitignore` 已忽略 `application.properties`，请不要把真实密码提交到 git。

### 3) 启动项目
在项目根目录执行（Windows）：

```bash
./mvnw.cmd spring-boot:run
```

或打包运行：

```bash
./mvnw.cmd -DskipTests package
java -jar target/*.jar
```

默认端口（可在 `application.properties` 修改）：
- `server.port=8081`

启动后访问：
- `http://localhost:8081/`

## 项目结构（简述）
- **后端代码**：`src/main/java/com/example/libweb/`
  - `controller/`：接口层
  - `service/`、`service/impl/`：业务逻辑
  - `repository/`：JPA 数据访问
  - `entity/`：实体
  - `dto/`：请求/传输对象
  - `task/`：定时任务
  - `config/`：配置类
- **静态前端**：`src/main/resources/static/`
  - 页面：`index.html`、`login.html`、`register.html`
  - 脚本：`static/js/app/`、`static/js/modules/`

## 常见问题
- **启动时报 DataSource 错误/连不上数据库**：检查 `spring.datasource.*` 是否填写正确、Oracle 服务是否可访问。
- **邮件发送失败**：检查 `spring.mail.*` 配置，QQ 邮箱一般需要使用 SMTP 授权码而不是登录密码。

