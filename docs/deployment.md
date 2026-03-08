# 部署说明

## 1. 部署概述

本项目采用前后端分离部署方式：

- 前端：Vue 3 + Vite
- 后端：Spring Boot
- 数据库：MySQL 8

本说明以本地开发部署和基础演示部署为主，适合毕业设计项目使用。

## 2. 环境要求

建议环境如下：

- JDK 8
- Maven 3.6+
- Node.js 16+
- npm 8+
- MySQL 8.x

## 3. 项目结构

部署时重点关注以下目录：

- `frontend/`：前端项目
- `backend/`：后端项目
- `backend/src/main/resources/all.sql`：数据库建表脚本
- `backend/src/main/resources/application-example.yml`：后端配置模板
- `frontend/.env.example`：前端环境变量模板

## 4. 数据库部署

### 4.1 创建数据库

在 MySQL 中创建数据库，推荐名称与当前项目配置保持一致：

`structured_clinical_assessment_tool_system`

### 4.2 导入建表脚本

导入以下脚本：

`backend/src/main/resources/all.sql`

说明：

- 该脚本为纯建表脚本
- 不包含测试数据
- 首次部署时直接导入即可

## 5. 后端部署

### 5.1 配置文件准备

后端示例配置文件：

`backend/src/main/resources/application-example.yml`

你可以参考该文件，填写自己的真实配置。

建议重点配置：

- 数据库连接地址
- 数据库用户名和密码
- AES 加密密钥
- Qwen API Key

示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/structured_clinical_assessment_tool_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password

medical:
  encryption:
    aes-key: your_16_or_32_char_aes_key

qwen:
  api-key: your_qwen_api_key
```

### 5.2 启动后端

进入后端目录并启动：

```bash
cd backend
mvn spring-boot:run
```

或先打包再运行：

```bash
cd backend
mvn clean package
java -jar target/zhian-clinical-assessment-system-1.0.0.jar
```

默认访问地址：

`http://localhost:8080/api`

## 6. 前端部署

### 6.1 环境变量准备

前端示例环境变量文件：

`frontend/.env.example`

当前主要变量如下：

```env
VITE_API_BASE_URL=/api
```

本地开发通常可直接使用 `/api`，再通过 Vite 代理转发到后端。

### 6.2 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

`http://localhost:3000`

## 7. 生产构建

### 7.1 前端构建

```bash
cd frontend
npm run build
```

构建完成后，生成目录通常为：

`frontend/dist`

### 7.2 后端构建

```bash
cd backend
mvn clean package -DskipTests
```

构建完成后，可执行包通常位于：

`backend/target/`

## 8. 部署顺序建议

推荐部署顺序如下：

1. 安装并启动 MySQL
2. 创建数据库并导入 `all.sql`
3. 配置后端数据库连接与密钥
4. 启动后端服务
5. 配置前端环境变量
6. 启动前端服务
7. 在浏览器中联调验证

## 9. 验证建议

部署完成后，建议至少验证以下内容：

- 后端接口是否可访问
- 前端页面是否可正常打开
- 登录接口是否正常
- 患者列表是否可查询
- 模板列表是否可查询
- 评估保存与提交是否正常
- 报告预览是否正常

## 10. 常见问题

### 10.1 前端能打开，但接口访问失败

请检查：

- 后端是否成功启动
- 后端端口是否为 `8080`
- 后端上下文路径是否为 `/api`
- 前端代理是否生效

### 10.2 数据库连接失败

请检查：

- MySQL 服务是否已启动
- 数据库名是否正确
- 用户名密码是否正确
- 数据源 URL 是否与本地环境一致

### 10.3 AI 功能不可用

请检查：

- 是否配置 `QWEN_API_KEY`
- 当前网络是否可访问对应大模型接口
- API 地址和模型名是否填写正确

## 11. 后续优化建议

- 增加 Docker 部署文件
- 增加 Nginx 前端静态资源部署说明
- 增加 HTTPS、域名与反向代理说明
- 将配置改为环境变量或配置中心统一管理
