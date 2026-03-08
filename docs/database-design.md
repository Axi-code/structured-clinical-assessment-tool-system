# 数据库设计文档

## 1. 数据库概述

项目当前使用 MySQL 8，默认数据库名为：

`structured_clinical_assessment_tool_system`

当前推荐导入的初始化脚本为：

`backend/src/main/resources/all.sql`

该脚本为纯建表脚本，不包含测试数据。

## 2. 设计原则

- 采用关系型数据库存储核心业务数据
- 通过主键 ID 进行表间关联
- 通过 `create_time`、`update_time` 保留时间信息
- 大部分核心业务表使用 `deleted` 进行逻辑删除
- 患者敏感字段支持加密存储
- 模板、规则、记录、日志等业务对象彼此分离，降低耦合

## 3. 核心表清单

当前数据库共包含 12 张核心业务表：

| 表名 | 说明 |
| --- | --- |
| `department` | 科室表 |
| `diagnosis` | 诊断表 |
| `sys_user` | 用户表 |
| `patient` | 患者表 |
| `assessment_template` | 评估模板表 |
| `template_department` | 模板适用科室关联表 |
| `assessment_field` | 评估字段表 |
| `assessment_rule` | 评估规则表 |
| `assessment_record` | 评估记录表 |
| `report_template` | 报告模板表 |
| `treatment_suggestion` | 诊疗建议表 |
| `operation_log` | 操作日志表 |

## 4. 主要实体关系

主要关系如下：

- 一个科室可对应多个诊断
- 一个科室可对应多个用户
- 一个科室可对应多个患者
- 一个患者可关联一个诊断
- 一个评估模板可配置多个字段
- 一个评估模板可配置多条规则
- 一个评估模板可适用于多个科室
- 一个患者可拥有多条评估记录
- 一条评估记录可对应多条诊疗建议
- 一个评估模板可关联多个报告模板

## 5. 主要数据表设计

### 5.1 `department`

用途：

- 存储医院科室基础信息

核心字段：

- `id`：主键
- `name`：科室名称
- `code`：科室编码
- `sort_order`：排序
- `remark`：备注

说明：

- `code` 为唯一键
- 供用户、患者、诊断、模板适用范围等模块引用

### 5.2 `diagnosis`

用途：

- 存储诊断信息，并归属到某个科室

核心字段：

- `id`：主键
- `name`：诊断名称
- `department_id`：所属科室 ID
- `icd_code`：ICD 编码
- `sort_order`：排序

说明：

- 同一诊断通过 `department_id` 绑定科室
- 用于患者诊断选择和业务联动

### 5.3 `sys_user`

用途：

- 存储系统用户账号信息

核心字段：

- `id`：主键
- `username`：用户名
- `password`：密码
- `real_name`：真实姓名
- `role`：角色
- `department_id`：所属科室 ID
- `status`：启用状态

说明：

- `username` 为唯一键
- 角色主要包括 `ADMIN`、`DOCTOR`、`NURSE`
- 密码采用 BCrypt，兼容历史 MD5 迁移逻辑

### 5.4 `patient`

用途：

- 存储患者基础信息和科室、诊断归属

核心字段：

- `id`：主键
- `patient_no`：患者编号
- `name`：姓名
- `gender`：性别
- `birth_date`：出生日期
- `age`：年龄
- `id_card`：身份证号
- `phone`：联系电话
- `address`：地址
- `emergency_contact`：紧急联系人
- `emergency_phone`：紧急联系电话
- `department_id`：所属科室 ID
- `diagnosis_id`：诊断 ID

说明：

- `patient_no` 为唯一键
- 多个敏感字段支持 AES 加密存储

### 5.5 `assessment_template`

用途：

- 存储评估模板主信息

核心字段：

- `id`：主键
- `template_name`：模板名称
- `template_code`：模板编码
- `category`：评估类别
- `template_content`：模板 JSON 内容
- `version`：版本号
- `status`：启停状态
- `min_score`：理论最低分
- `max_score`：理论最高分

说明：

- 使用 `(template_code, version)` 联合唯一
- 支持模板版本演进

### 5.6 `template_department`

用途：

- 维护模板与科室的多对多关系

核心字段：

- `id`：主键
- `template_id`：模板 ID
- `department_id`：科室 ID
- `create_time`：创建时间

说明：

- `(template_id, department_id)` 为联合唯一

### 5.7 `assessment_field`

用途：

- 存储评估模板中的结构化字段定义

核心字段：

- `id`：主键
- `template_id`：模板 ID
- `field_code`：字段编码
- `field_type`：字段类型
- `field_label`：字段标签
- `required`：是否必填
- `options`：选项 JSON
- `validation_rule`：校验规则 JSON
- `sort_order`：排序

说明：

- 一个模板可配置多个字段
- 字段类型支持文本、数字、日期、单选、多选等

### 5.8 `assessment_rule`

用途：

- 存储评估规则

核心字段：

- `id`：主键
- `template_id`：模板 ID
- `rule_name`：规则名称
- `rule_code`：规则编码
- `rule_type`：规则类型
- `condition_expression`：条件表达式
- `result_expression`：结果表达式
- `priority`：优先级
- `status`：状态

说明：

- 规则类型包括 `SCORE`、`RISK`、`CALCULATE`
- 支持模板对应多条规则

### 5.9 `assessment_record`

用途：

- 存储患者实际评估记录

核心字段：

- `id`：主键
- `patient_id`：患者 ID
- `template_id`：模板 ID
- `record_no`：记录编号
- `assessment_data`：评估数据 JSON
- `total_score`：总分
- `assessment_result`：评估结果
- `risk_level`：风险等级
- `risk_tips`：风险提示
- `status`：草稿或完成状态
- `assessor_id`：评估人 ID
- `department_id`：科室 ID

说明：

- `record_no` 为唯一键
- 提交后可用于历史查询、对比、报告导出和统计分析

### 5.10 `report_template`

用途：

- 存储 PDF / Word 报告模板

核心字段：

- `id`：主键
- `template_name`：模板名称
- `template_code`：模板编码
- `assessment_template_id`：关联评估模板 ID
- `report_type`：报告类型
- `is_default`：是否默认模板
- `status`：状态

说明：

- 一个评估模板可关联多个报告模板

### 5.11 `treatment_suggestion`

用途：

- 存储 AI 或业务逻辑生成的诊疗建议

核心字段：

- `id`：主键
- `patient_id`：患者 ID
- `assessment_record_id`：评估记录 ID
- `suggestion_no`：建议编号
- `suggestion_content`：建议内容
- `generator_id`：生成人 ID
- `status`：状态

说明：

- 可根据评估记录生成与重生成
- 与患者和评估记录保持关联

### 5.12 `operation_log`

用途：

- 存储关键业务操作日志

核心字段：

- `id`：主键
- `user_id`：操作人 ID
- `username`：操作人用户名
- `real_name`：操作人姓名
- `role`：角色
- `module`：所属模块
- `action`：动作
- `target_type`：目标类型
- `target_id`：目标对象 ID
- `description`：描述
- `ip`：操作来源 IP

说明：

- 用于审计、问题追溯和行为留痕

## 6. 索引与约束设计

系统当前主要采用以下约束与索引设计：

- 唯一约束：
  - `department.code`
  - `sys_user.username`
  - `patient.patient_no`
  - `assessment_template(template_code, version)`
  - `template_department(template_id, department_id)`
  - `assessment_record.record_no`
- 普通索引：
  - 科室、诊断、患者、评估记录等高频查询字段
  - 状态、时间、角色、模板、科室等常用筛选字段

## 7. 逻辑删除设计

除 `template_department` 外，大多数业务表都包含 `deleted` 字段：

- `0`：未删除
- `1`：已删除

这样可以避免物理删除后数据直接丢失，便于审计与恢复。

## 8. 安全相关设计

数据库层面的安全设计主要体现在：

- 患者敏感字段密文存储
- 密码字段使用安全哈希策略
- 通过日志表记录重要操作
- 尽量避免在仓库中提交真实数据和生产配置

## 9. 总结

当前数据库设计已经覆盖用户、患者、模板、规则、记录、报告、诊疗建议、操作日志等主要业务对象，结构完整，适合支撑毕业设计项目演示与后续功能扩展。
