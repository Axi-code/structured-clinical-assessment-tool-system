# 核心数据流图

> 本文档覆盖 `docs/study/` 全部技术文档中描述的数据流转逻辑，按 **评估流程 → 规则引擎 → AI 能力 → 安全机制** 四大主题分组绘制。

---

## 1. 评估数据全局流转

用户操作产出 `assessmentData`，经后端校验、规则引擎计算后将评估结果落库的完整端到端链路。

```mermaid
flowchart TD
    U["用户（医生 / 护士）"]

    subgraph FE["前端 · Vue 3"]
        F1["选择患者与评估模板"]
        F2{"评估模式?"}
        F3["表单模式 — 逐项填写"]
        F4["对话模式 — AI 多轮采集"]
        F5["统一产物 assessmentData\n键值对集合：fieldCode → 用户值"]
    end

    subgraph BE["后端 · Spring Boot"]
        B1["数据校验\n必填 · 类型 · 范围"]
        B2["存入 assessment_record\nassessment_data = JSON, status = 1"]
        B3["executeAllRules\ntemplateId, assessmentData"]
        B4["applyResultToRecord\n写回 totalScore · riskLevel\nassessmentResult · riskTips"]
    end

    subgraph DATA["数据层"]
        DB[("assessment_record 表")]
        RULE[("assessment_rule 表")]
    end

    U --> F1 --> F2
    F2 -->|表单| F3
    F2 -->|对话| F4
    F3 --> F5
    F4 --> F5
    F5 -->|"POST /submit 或 /finalize"| B1
    B1 --> B2 --> B3
    RULE -.->|读取可用规则| B3
    B3 --> B4 --> DB

    classDef feStyle fill:#e3f2fd,stroke:#1565c0
    classDef beStyle fill:#fff3e0,stroke:#e65100
    classDef dbStyle fill:#e8f5e9,stroke:#2e7d32
    class F1,F2,F3,F4,F5 feStyle
    class B1,B2,B3,B4 beStyle
    class DB,RULE dbStyle
```

---

## 2. 表单模式评估时序

从创建草稿到提交评估、规则计算并写回结果的完整交互时序，含实时计算预览。

```mermaid
sequenceDiagram
    actor U as 医生 / 护士
    participant F as 前端
    participant B as 后端
    participant R as assessment_rule
    participant DB as assessment_record

    rect rgb(232, 245, 233)
    Note over U,DB: 第一步 · 创建草稿
    U->>F: 选择患者 + 评估模板
    F->>B: POST /assessment-record/draft
    B->>DB: INSERT record（status=0）
    DB-->>B: record_id
    B-->>F: 返回 record_id
    end

    rect rgb(227, 242, 253)
    Note over U,DB: 第二步 · 填写表单 + 实时预览
    U->>F: 逐项填写表单字段
    Note over F: form.assessmentData 持续累积
    loop 实时预览（前端防抖 500ms）
        F->>B: POST /assessment-rule/calculate-realtime
        B->>R: 读取 SCORE + RISK 规则
        B->>B: executeAllRules（不落库）
        B-->>F: 返回 totalScore / riskLevel 预览
    end
    end

    rect rgb(255, 243, 224)
    Note over U,DB: 第三步 · 提交评估
    U->>F: 点击「提交」
    F->>B: POST /assessment-record/submit
    B->>B: 校验数据（必填 / 类型 / 范围）
    B->>DB: UPDATE assessment_data, status = 1
    B->>R: 读取全部规则
    B->>B: executeAllRules → 计算结果
    B->>DB: UPDATE totalScore / riskLevel / assessmentResult
    B-->>F: 提交成功
    end
```

---

## 3. 对话式评估与 LLM 结构化解析

AI 通过多轮对话采集评估数据，每轮由 LLM 解析用户回答为 `mapped_data_delta` 增量数据并合并，最终走同一套规则引擎计算。

```mermaid
sequenceDiagram
    actor U as 患者 / 医生
    participant F as 前端
    participant B as 后端
    participant LLM as 通义千问
    participant DB as 数据库

    U->>F: 选择患者，开始对话评估
    F->>B: POST /assessment-conversation/start
    B-->>F: 首轮提问（第一个缺失必填字段）

    loop 多轮对话采集
        U->>F: 自然语言回答（如「睡不好」「无」）
        F->>B: POST /assessment-conversation/reply
        B->>LLM: prompt = 对话历史 + 缺失字段 + 本轮回答
        LLM-->>B: JSON: mapped_data_delta + assistant_question + completion
        Note over B: 强制补底：若 LLM 漏提取目标字段，<br/>程序按关键词兜底填入
        B->>B: 合并 delta → currentData
        B->>B: 状态机推进 → 找下一个缺失字段
        opt 实时计算
            B->>B: executeAllRules（不落库）
        end
        B-->>F: assistantMessage + mappedData + completion
        F-->>U: 展示 AI 提问 + 实时评分预览
    end

    U->>F: 结束对话并提交
    F->>B: POST /assessment-conversation/finalize
    B->>DB: 创建 assessment_record（status=1）
    B->>B: executeAllRules → 计算结果
    opt 规则引擎完成后
        B->>LLM: 获取 diagnosisName
    end
    B->>DB: 写回 totalScore / riskLevel / assessmentResult
    B-->>F: 评估完成
```

---

## 4. 单轮对话详细处理流程

一轮对话内部的完整处理链：LLM 解析 → 强制补底 → 合并数据 → 状态机推进 → 生成下轮提问。

```mermaid
flowchart TD
    A["患者发送消息\n（如「无」「睡不好」）"]
    B["确定当前目标字段\n按模板顺序第一个缺失必填字段"]
    C["调用通义千问\n传入对话历史 + 缺失字段列表 + 本轮回答"]
    D["LLM 返回 JSON\nmapped_data_delta · assistant_question\nmissing_fields · completion · confidence"]
    E{"mapped_data_delta\n包含目标字段?"}
    G["合并 delta → currentData"]
    F["强制补底逻辑"]
    F1{"回答长度与关键词"}
    F2["≤10 字：直接赋值"]
    F3["含「无/没有/正常/否」：赋值「无/正常」"]
    F4["其他：用原话赋值"]
    H["状态机推进\n遍历必填字段 → 第一个仍为空的"]
    I{"所有必填字段已填?"}
    J{"AI assistant_question\n针对下一字段?"}
    K["返回「信息已基本完整」"]
    L["采用 AI 生成的自然提问"]
    M["模板生成提问\n如「请问您的食欲是？可选：好/一般/差」"]
    N["返回前端\nassistantMessage + mappedData + completion"]

    A --> B --> C --> D --> E
    E -->|是| G
    E -->|否| F --> F1
    F1 -->|≤10 字| F2 --> G
    F1 -->|含否定词| F3 --> G
    F1 -->|其他| F4 --> G
    G --> H --> I
    I -->|否| J
    I -->|是| K --> N
    J -->|是| L --> N
    J -->|否| M --> N

    classDef llm fill:#f3e5f5,stroke:#7b1fa2
    classDef fallback fill:#fce4ec,stroke:#c62828
    classDef merge fill:#e8f5e9,stroke:#2e7d32
    class C,D llm
    class F,F1,F2,F3,F4 fallback
    class G,H merge
```

---

## 5. 规则引擎执行管线（executeAllRules）

按固定顺序执行四类规则，每阶段的输入依赖上阶段的输出。规则存储在数据库中，通过 Nashorn 脚本引擎执行 `${fieldCode}` 变量替换后的 JavaScript 表达式。

```mermaid
flowchart TD
    IN["输入\ntemplateId + assessmentData"]

    subgraph P1["① 异常检测 · ABNORMAL"]
        A1["遍历 ABNORMAL 规则\n变量替换 → Nashorn 执行条件\n条件为 true → 收集 abnormalDataTips"]
    end

    subgraph P2["② 量表计算 · SCORE"]
        S1["初始化 totalScore = 0"]
        S2["遍历 SCORE 规则（按 priority 升序）\n变量替换 → 条件判断 → 结果表达式\n返回值累加至 totalScore"]
        S1 --> S2
    end

    subgraph P3["③ 风险分级 · RISK"]
        R1["将 totalScore 注入上下文 ctx"]
        R2["遍历 RISK 规则（按 priority 升序）\n条件判断（如 totalScore >= 20）\n匹配 → 覆盖 riskLevel，追加 riskTip"]
        R1 --> R2
    end

    subgraph P4["④ 衍生计算 · CALCULATE"]
        C1["基于完整 ctx（含 totalScore + riskLevel）\n遍历 CALCULATE 规则\n执行结果表达式\n以 ruleCode 为 key 存入 calculationResult"]
    end

    OUT["合并输出\ntotalScore · riskLevel · riskTips\nassessmentResult · abnormalDataTips · 衍生指标"]

    IN --> A1
    A1 --> S1
    S2 --> R1
    R2 --> C1
    C1 --> OUT

    classDef abnormal fill:#fff9c4,stroke:#f9a825
    classDef score fill:#e3f2fd,stroke:#1565c0
    classDef risk fill:#fce4ec,stroke:#c62828
    classDef calc fill:#f3e5f5,stroke:#7b1fa2
    class A1 abnormal
    class S1,S2 score
    class R1,R2 risk
    class C1 calc
```

---

## 6. 规则模块数据依赖与执行顺序

四类规则之间的数据流向与依赖关系，决定了不可颠倒的执行顺序：SCORE 不依赖 RISK，RISK 依赖 totalScore，CALCULATE 可依赖前面所有输出。

```mermaid
flowchart TD
    AD["assessmentData\n用户填写的原始评估数据"]

    AD --> ABN["ABNORMAL\n异常检测"]
    AD --> SCO["SCORE\n量表计算"]

    SCO --> TS["totalScore\n总分"]

    AD --> CTX["ctx\nassessmentData + totalScore"]
    TS --> CTX

    CTX --> RSK["RISK\n风险分级"]
    CTX --> CAL["CALCULATE\n衍生计算"]

    ABN --> ATIP["abnormalDataTips\n异常提示"]
    RSK --> RL["riskLevel + riskTips"]
    CAL --> CI["衍生指标"]

    RL --> AR["assessmentResult\n优先取 riskLevel\n无 RISK 规则时按总分判定"]

    classDef input fill:#e3f2fd,stroke:#1565c0
    classDef score fill:#fff3e0,stroke:#e65100
    classDef risk fill:#fce4ec,stroke:#c62828
    classDef calc fill:#f3e5f5,stroke:#7b1fa2
    classDef output fill:#e8f5e9,stroke:#2e7d32
    class AD input
    class SCO,TS score
    class RSK,RL risk
    class CAL,CI calc
    class AR output
```

---

## 7. 实时计算与提交计算对比

两种计算场景使用同一套 `executeAllRules` 逻辑，区别仅在于是否将结果持久化到数据库。

```mermaid
flowchart TD
    subgraph RT["实时计算 · 预览 · 不落库"]
        RT1["用户每次输入\n前端防抖 500ms"]
        RT2["POST /calculate-realtime"]
        RT3["executeAllRules"]
        RT4["返回 totalScore / riskLevel"]
        RT5["前端展示预览\n提升用户填写体验"]
        RT1 --> RT2 --> RT3 --> RT4 --> RT5
    end

    subgraph SM["提交计算 · 落库"]
        SM1["用户点击提交"]
        SM2["POST /submit 或 /finalize"]
        SM3["校验 + 存 assessmentData\nstatus = 1"]
        SM4["executeAllRules"]
        SM5["结果写回 assessment_record\ntotalScore · riskLevel · assessmentResult"]
        SM1 --> SM2 --> SM3 --> SM4 --> SM5
    end

    classDef rt fill:#e3f2fd,stroke:#1565c0
    classDef sm fill:#fff3e0,stroke:#e65100
    class RT1,RT2,RT3,RT4,RT5 rt
    class SM1,SM2,SM3,SM4,SM5 sm
```

---

## 8. AI 自动生成模板与规则

当用户选择「AI 根据主诉生成模板」时，LLM 同步输出模板字段、评分规则和风险规则，程序负责标准化与兜底补充，确保 AI 模板也能走规则引擎评分。

```mermaid
flowchart TD
    A["用户输入主诉\n请求 AI 生成模板"]
    B["调用通义千问 LLM"]
    C["LLM 返回 JSON"]
    D["fields\n模板字段定义"]
    E["scoringRules\n评分规则"]
    F["riskRules\n风险规则"]
    G["保存 AssessmentTemplate\n+ AssessmentField"]
    H{"LLM 生成了\nscoringRules?"}
    I["解析 scoreMap / ranges\nSELECT: 选项→分值\nNUMBER: 区间→分值"]
    J["程序自动推导默认规则\n严重程度→1/3/5\n是否→0/2\n年龄→分段"]
    K["生成 SCORE 类型 AssessmentRule"]
    L{"LLM 生成了\nriskRules?"}
    M["解析总分区间\n→ 风险等级 + 临床建议"]
    N["按最大可能总分\n30% / 60% 阈值自动划分"]
    O["生成 RISK 类型 AssessmentRule"]
    P[("保存至 assessment_rule 表")]
    Q["AI 模板走规则引擎评分\n结果确定可解释"]

    A --> B --> C
    C --> D --> G
    C --> E --> H
    C --> F --> L
    H -->|是| I --> K
    H -->|否| J --> K
    L -->|是| M --> O
    L -->|否| N --> O
    K --> P
    O --> P
    G --> P
    P --> Q

    classDef ai fill:#f3e5f5,stroke:#7b1fa2
    classDef rule fill:#fff3e0,stroke:#e65100
    classDef db fill:#e8f5e9,stroke:#2e7d32
    class A,B,C ai
    class I,J,K,M,N,O rule
    class P,G db
```

---

## 9. 敏感字段加密与脱敏三层数据流

患者敏感信息（身份证、手机、地址等）在存储、业务、展示三层采用不同形态，通过 MyBatis TypeHandler + AES 实现对业务代码零侵入。

```mermaid
flowchart TD
    subgraph WRITE["写入 — 加密落库"]
        W1["业务代码 setIdCard\n（明文）"]
        W2["MyBatis INSERT 触发\nEncryptedStringTypeHandler"]
        W3["AES 加密 + Base64 编码"]
        W4[("数据库存储密文")]
        W1 --> W2 --> W3 --> W4
    end

    subgraph READ["读取 — 解密出库"]
        R1[("数据库密文")]
        R2["MyBatis SELECT 触发\nEncryptedStringTypeHandler"]
        R3["Base64 解码 + AES 解密"]
        R4["业务代码 getIdCard\n（明文）"]
        R1 --> R2 --> R3 --> R4
    end

    subgraph SHOW["展示 — 脱敏输出"]
        D1["业务层明文"]
        D2["PatientDesensitizationUtil"]
        D3["PatientVO 返回前端\n身份证 → 1101**********34\n手机号 → 138****8001\n地址 → 北京市朝阳区***"]
        D1 --> D2 --> D3
    end

    W4 -.->|"查询时"| R1
    R4 -.->|"返回前端时"| D1

    classDef enc fill:#fff3e0,stroke:#e65100
    classDef dec fill:#e3f2fd,stroke:#1565c0
    classDef mask fill:#fce4ec,stroke:#c62828
    class W1,W2,W3,W4 enc
    class R1,R2,R3,R4 dec
    class D1,D2,D3 mask
```

---

## 10. 审计日志 AOP 记录流程

通过 `@OperationLogRecord` 注解 + AOP 切面实现声明式审计日志，方法执行成功后自动解析 SpEL 表达式并记录操作日志，业务方法无需手动编写日志代码。

```mermaid
sequenceDiagram
    actor U as 用户
    participant C as Controller
    participant AOP as OperationLogAspect
    participant SpEL as SpEL 解析器
    participant S as OperationLogService
    participant DB as operation_log 表

    U->>C: 调用业务接口（如 POST /patient/add）
    Note over C: 方法标注 @OperationLogRecord<br/>(module, action, targetId, description)
    C->>C: 执行业务逻辑并返回
    C-->>AOP: 方法返回后 @AfterReturning 触发
    AOP->>SpEL: 解析 targetId = "#createDTO.name"
    SpEL-->>AOP: 解析结果 → "张三"
    AOP->>SpEL: 解析 description = "'新增患者：' + #createDTO.name"
    SpEL-->>AOP: 解析结果 → "新增患者：张三"
    AOP->>AOP: 获取当前用户 userId / username / role / IP
    AOP->>S: 组装 OperationLog 并保存
    S->>DB: INSERT INTO operation_log
    Note over C,DB: 全程对业务代码透明，无侵入
    C-->>U: 返回正常响应
```
