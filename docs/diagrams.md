# 系统架构图与工作流程图

本文档提供智安临评系统的架构图与核心工作流程的 Mermaid 源码，便于在文档、答辩或导出为图片时使用。

> 更详细的 10 张核心数据流图（评估流程、规则引擎、AI 能力、安全机制）见 [core-data-flows.md](core-data-flows.md)。

## 1. 系统架构图

```mermaid
flowchart TB
    subgraph 用户端
        Browser[浏览器]
    end

    subgraph 前端层["前端 (Vue 3 + Vite)"]
        Vue[Vue 3 应用]
        Router[Vue Router]
        Pinia[Pinia 状态]
        Element[Element Plus]
        Vue --> Router
        Vue --> Pinia
        Vue --> Element
    end

    subgraph 后端层["后端 (Spring Boot)"]
        Controller[Controller 层]
        Service[Service 层]
        Mapper[Mapper 层]
        AOP[AOP 日志/鉴权]
        Controller --> Service
        Service --> Mapper
        AOP -.-> Controller
    end

    subgraph 数据层["数据与外部服务"]
        MySQL[(MySQL 8)]
        Redis[(Redis)]
        Qwen[Qwen 大模型 API]
    end

    Browser <-->|HTTP/HTTPS| Vue
    Vue <-->|REST API /api/*| Controller
    Mapper <-->|MyBatis-Plus| MySQL
    Service <-->|Refresh Token / Sa-Token| Redis
    Service -->|诊疗建议/模板推荐/对话评估| Qwen
```

## 2. 患者评估与诊断确认流程

```mermaid
flowchart TD
    A[患者建档] --> B[选择患者与模板]
    B --> C{评估模式}
    C -->|表单模式| D[填写评估表单]
    C -->|对话模式| E[AI 对话式采集]
    D --> F[提交评估]
    E --> F
    F --> G[规则/AI 计算评估结果]
    G --> H[AI 产出 diagnosisName]
    H --> I{诊断字典匹配}
    I -->|匹配成功| J[自动回填患者当前诊断]
    I -->|未匹配| K[高亮 AI 建议诊断]
    J --> L[医生可确认/修正]
    K --> M[一键加入诊断字典并采用]
    M --> J
    L --> N[完成]
```

## 3. AI 诊断与诊断字典联动流程

```mermaid
flowchart LR
    subgraph 评估完成
        A1[评估提交] --> A2[AI 计算]
        A2 --> A3[产出 diagnosisName]
    end

    subgraph 自动匹配
        A3 --> B1{科室诊断字典}
        B1 -->|精确/模糊匹配| B2[匹配成功]
        B1 -->|无匹配| B3[未匹配]
        B2 --> B4[更新 patient.diagnosis_id]
    end

    subgraph 医生操作
        B3 --> C1[诊断详情页高亮提示]
        C1 --> C2[点击 加入诊断字典并采用]
        C2 --> C3[新建诊断字典项]
        C3 --> B4
    end
```

## 4. 登录与会话续期流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant B as 后端
    participant R as Redis

    U->>F: 输入账号密码
    F->>B: POST /user/login
    B->>B: 校验验证码/限流
    B->>B: 校验密码
    B->>R: 存储 Refresh Token
    B-->>F: Access Token + Set-Cookie
    F->>F: 存储 Token，跳转首页

    loop 后续请求
        F->>B: Authorization: Bearer {token}
        B->>B: 校验 Access Token
        alt Token 有效
            B-->>F: 正常响应
        else Token 过期 401
            F->>B: POST /user/refresh (带 Cookie)
            B->>R: 校验 Refresh Token
            B-->>F: 新 Access Token
            F->>B: 重试原请求
        end
    end
```

## 5. 诊疗建议生成流程

```mermaid
flowchart TD
    A[医生选择评估记录] --> B[点击 生成建议]
    B --> C[后端获取患者/模板/评估数据]
    C --> D[构建提示词]
    D --> E[调用 Qwen API]
    E --> F[解析 AI 返回]
    F --> G[保存诊疗建议记录]
    G --> H[返回前端展示]
    H --> I[支持重新生成/复制]
```

## 使用说明

- 上述 Mermaid 图可在支持 Mermaid 的 Markdown 渲染器中直接显示（如 GitHub、GitLab、Typora、VS Code 插件等）。
- 如需导出为 PNG/SVG，可使用 [Mermaid Live Editor](https://mermaid.live/) 或 VS Code 的 Mermaid 插件。
- 导出图片后可放入 `docs/images/` 目录，供 README 或答辩材料引用。
