# 接口文档

## 1. 接口概述

后端采用 REST 风格接口设计，统一前缀为：

`/api`

本项目当前主要接口覆盖用户、患者、评估模板、评估规则、评估记录、报告、统计、AI 对话评估等模块。

## 2. 认证说明

### 2.1 Access Token

- 请求头：`Authorization: Bearer <token>`
- 默认有效期：2 小时

### 2.2 Refresh Token

- 通过 HttpOnly Cookie 保存
- 默认有效期：7 天

## 3. 统一返回格式

### 3.1 普通返回结构

```json
{
  "code": 200,
  "msg": "操作成功",
  "message": "操作成功",
  "data": {}
}
```

字段说明：

- `code`：状态码，成功通常为 `200`
- `msg`：后端返回消息
- `message`：与 `msg` 含义一致，便于前端兼容读取
- `data`：业务数据

### 3.2 分页返回结构

```json
{
  "total": 100,
  "records": []
}
```

字段说明：

- `total`：总记录数
- `records`：当前页数据列表

## 4. 登录返回示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "token": "xxxxxx",
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN",
    "department": "信息科",
    "departmentId": 1
  }
}
```

## 5. 接口模块说明

### 5.1 用户模块

基础路径：`/api/user`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/captcha` | 获取验证码 |
| POST | `/login` | 用户登录 |
| POST | `/refresh` | 刷新会话 |
| POST | `/logout` | 用户退出 |
| POST | `/register` | 用户注册 |
| GET | `/info` | 获取当前用户信息 |
| GET | `/list` | 用户列表 |
| POST | `/add` | 新增用户 |
| PUT | `/update` | 更新用户 |
| DELETE | `/delete/{id}` | 删除用户 |
| PUT | `/password` | 修改密码 |

### 5.2 科室模块

基础路径：`/api/department`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/list-all` | 查询全部科室 |
| GET | `/page` | 科室分页查询 |
| POST | `/add` | 新增科室 |
| PUT | `/update` | 更新科室 |
| DELETE | `/delete/{id}` | 删除科室 |

### 5.3 诊断模块

基础路径：`/api/diagnosis`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/by-department/{departmentId}` | 按科室查询诊断 |
| GET | `/page` | 诊断分页查询 |
| POST | `/add` | 新增诊断 |
| PUT | `/update` | 更新诊断 |
| DELETE | `/delete/{id}` | 删除诊断 |

### 5.4 患者模块

基础路径：`/api/patient`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/list` | 患者列表查询 |
| GET | `/{id}` | 查询患者详情 |
| GET | `/{id}/edit` | 查询患者编辑信息 |
| POST | `/add` | 新增患者 |
| PUT | `/update` | 更新患者 |
| PUT | `/{id}/diagnosis` | 更新患者诊断 |
| DELETE | `/delete/{id}` | 删除患者 |

### 5.5 评估模板模块

基础路径：`/api/assessment-template`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/list` | 模板列表 |
| GET | `/{id}` | 模板详情 |
| GET | `/{id}/fields` | 查询模板字段 |
| POST | `/add` | 新增模板 |
| PUT | `/update` | 更新模板 |
| DELETE | `/delete/{id}` | 删除模板 |
| PUT | `/{id}/status` | 修改模板状态 |
| GET | `/{templateCode}/versions` | 查询模板版本列表 |
| POST | `/{id}/create-version` | 基于模板创建新版本 |
| POST | `/field/add` | 新增模板字段 |
| PUT | `/field/update` | 更新模板字段 |
| DELETE | `/field/delete/{id}` | 删除模板字段 |

### 5.6 评估规则模块

基础路径：`/api/assessment-rule`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/list` | 规则列表 |
| GET | `/template/{templateId}` | 根据模板查询规则 |
| GET | `/{id}` | 规则详情 |
| POST | `/create` | 新增规则 |
| PUT | `/{id}` | 更新规则 |
| DELETE | `/{id}` | 删除规则 |
| PUT | `/{id}/status` | 修改规则状态 |
| POST | `/test` | 规则测试 |
| POST | `/calculate-realtime` | 实时计算 |

### 5.7 评估记录模块

基础路径：`/api/assessment-record`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/draft` | 创建评估草稿 |
| POST | `/save` | 保存评估数据 |
| POST | `/submit` | 提交评估 |
| GET | `/history/{patientId}` | 查询患者历史评估记录 |
| POST | `/compare` | 多条评估记录对比 |

### 5.8 对话式评估模块

基础路径：`/api/assessment-conversation`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/generate-template` | AI 生成模板草稿 |
| POST | `/start` | 开始对话式评估 |
| POST | `/reply` | 继续对话评估 |
| POST | `/calculate-realtime` | 对话流程实时计算 |
| POST | `/finalize` | 完成对话式评估 |
| POST | `/recommend-template` | AI 推荐模板 |

### 5.9 报告模块

基础路径：`/api/report`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/preview/pdf/{recordId}` | 预览 PDF 报告 |
| GET | `/preview/word/{recordId}` | 预览 Word 报告 |
| GET | `/pdf/{recordId}` | 导出 PDF 报告 |
| GET | `/word/{recordId}` | 导出 Word 报告 |

### 5.10 报告模板模块

基础路径：`/api/report-template`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/list` | 报告模板列表 |
| GET | `/assessment-template/{assessmentTemplateId}` | 查询指定评估模板对应报告模板 |
| GET | `/{id}` | 报告模板详情 |
| POST | `/create` | 新增报告模板 |
| PUT | `/{id}` | 更新报告模板 |
| DELETE | `/{id}` | 删除报告模板 |
| PUT | `/{id}/set-default` | 设置默认模板 |
| PUT | `/{id}/status` | 修改状态 |

### 5.11 统计模块

基础路径：`/api/statistics`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/time` | 时间维度统计 |
| GET | `/department` | 科室维度统计 |
| GET | `/template` | 模板维度统计 |
| GET | `/risk-level` | 风险等级统计 |
| GET | `/indicator/trend` | 指标趋势分析 |
| GET | `/indicator/distribution` | 指标分布分析 |
| GET | `/dashboard` | 首页看板数据 |
| GET | `/overview` | 综合概览 |
| GET | `/risk-alert` | 风险预警 |

### 5.12 操作日志模块

基础路径：`/api/operation-log`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 分页查询操作日志 |

### 5.13 诊疗建议模块

基础路径：`/api/treatment-suggestion`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/generate/{recordId}` | 生成诊疗建议 |
| POST | `/regenerate/{recordId}` | 重新生成诊疗建议 |
| GET | `/{id}` | 查询建议详情 |
| GET | `/patient/{patientId}` | 按患者查询建议 |
| GET | `/record/{recordId}` | 按评估记录查询建议 |
| GET | `/page` | 建议分页查询 |
| DELETE | `/{id}` | 删除建议 |

## 6. 常见接口调用顺序

### 6.1 登录与获取用户信息

1. 调用 `GET /api/user/captcha`
2. 调用 `POST /api/user/login`
3. 登录成功后携带 `Authorization` 请求头
4. 调用 `GET /api/user/info` 获取当前用户信息

### 6.2 患者评估流程

1. 查询患者列表 `GET /api/patient/list`
2. 查询模板列表 `GET /api/assessment-template/list`
3. 创建评估草稿 `POST /api/assessment-record/draft`
4. 保存或提交评估 `POST /api/assessment-record/save` / `POST /api/assessment-record/submit`
5. 预览或导出报告 `GET /api/report/...`

### 6.3 AI 建议生成流程

1. 先完成评估并生成记录
2. 调用 `POST /api/treatment-suggestion/generate/{recordId}`
3. 通过 `GET /api/treatment-suggestion/record/{recordId}` 查询结果

## 7. 错误处理说明

一般情况下：

- 成功返回 `code = 200`
- 业务异常通常返回错误提示消息
- 调用受保护接口时，未登录或无权限将返回鉴权相关错误

## 8. 说明

- 本文档基于当前控制器路由整理，适合作为项目展示版接口文档。
- 若用于正式对外开放，建议后续接入 Swagger / Knife4j 等自动化文档工具。
