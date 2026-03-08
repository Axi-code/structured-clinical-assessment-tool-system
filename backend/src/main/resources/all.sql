-- 项目当前版本数据库结构（纯建表，无测试数据）
-- 适用于 MySQL 8.x

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `structured_clinical_assessment_tool_system`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `structured_clinical_assessment_tool_system`;

DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `treatment_suggestion`;
DROP TABLE IF EXISTS `report_template`;
DROP TABLE IF EXISTS `assessment_record`;
DROP TABLE IF EXISTS `assessment_rule`;
DROP TABLE IF EXISTS `assessment_field`;
DROP TABLE IF EXISTS `template_department`;
DROP TABLE IF EXISTS `assessment_template`;
DROP TABLE IF EXISTS `patient`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `diagnosis`;
DROP TABLE IF EXISTS `department`;

CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '科室名称',
  `code` varchar(50) NOT NULL COMMENT '科室编码',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_code` (`code`),
  KEY `idx_department_name` (`name`),
  KEY `idx_department_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

CREATE TABLE `diagnosis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '诊断名称',
  `department_id` bigint NOT NULL COMMENT '所属科室ID',
  `icd_code` varchar(50) DEFAULT NULL COMMENT 'ICD编码',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_diagnosis_department_id` (`department_id`),
  KEY `idx_diagnosis_icd_code` (`icd_code`),
  KEY `idx_diagnosis_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊断表';

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `role` varchar(20) NOT NULL COMMENT '角色：ADMIN、DOCTOR、NURSE',
  `department_id` bigint DEFAULT NULL COMMENT '所属科室ID',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role` (`role`),
  KEY `idx_sys_user_department_id` (`department_id`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `patient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_no` varchar(50) NOT NULL COMMENT '患者编号',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别：男/女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `age` int DEFAULT NULL COMMENT '年龄',
  `id_card` varchar(100) DEFAULT NULL COMMENT '身份证号（L3加密存储）',
  `phone` varchar(100) DEFAULT NULL COMMENT '联系电话（L3加密存储）',
  `address` varchar(500) DEFAULT NULL COMMENT '地址（L3加密存储）',
  `emergency_contact` varchar(150) DEFAULT NULL COMMENT '紧急联系人（L3加密存储）',
  `emergency_phone` varchar(100) DEFAULT NULL COMMENT '紧急联系电话（L3加密存储）',
  `department_id` bigint NOT NULL COMMENT '所属科室ID',
  `diagnosis_id` bigint DEFAULT NULL COMMENT '诊断ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_patient_no` (`patient_no`),
  KEY `idx_patient_name` (`name`),
  KEY `idx_patient_phone` (`phone`),
  KEY `idx_patient_department_id` (`department_id`),
  KEY `idx_patient_diagnosis_id` (`diagnosis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者表';

CREATE TABLE `assessment_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(100) NOT NULL COMMENT '模板编码',
  `category` varchar(100) DEFAULT NULL COMMENT '评估类别',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `template_content` text COMMENT '模板内容（JSON格式）',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `min_score` decimal(10,2) DEFAULT NULL COMMENT '理论最低分',
  `max_score` decimal(10,2) DEFAULT NULL COMMENT '理论最高分',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code_version` (`template_code`, `version`),
  KEY `idx_assessment_template_code` (`template_code`),
  KEY `idx_assessment_template_category` (`category`),
  KEY `idx_assessment_template_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估模板表';

CREATE TABLE `template_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `department_id` bigint NOT NULL COMMENT '科室ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_department` (`template_id`, `department_id`),
  KEY `idx_template_department_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板适用科室关联表';

CREATE TABLE `assessment_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `field_name` varchar(100) DEFAULT NULL COMMENT '字段名称',
  `field_code` varchar(50) NOT NULL COMMENT '字段编码',
  `field_type` varchar(20) NOT NULL COMMENT '字段类型：TEXT、NUMBER、DATE、SELECT、RADIO、CHECKBOX、TEXTAREA',
  `field_label` varchar(100) NOT NULL COMMENT '字段标签',
  `required` int NOT NULL DEFAULT 0 COMMENT '是否必填：0-否，1-是',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `options` text COMMENT '选项值（JSON格式）',
  `validation_rule` text COMMENT '验证规则（JSON格式）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `group_name` varchar(100) DEFAULT NULL COMMENT '分组名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_assessment_field_template_id` (`template_id`),
  KEY `idx_assessment_field_field_code` (`field_code`),
  KEY `idx_assessment_field_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估字段表';

CREATE TABLE `assessment_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) DEFAULT NULL COMMENT '规则编码',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型：SCORE、RISK、CALCULATE',
  `rule_content` text COMMENT '规则内容（JSON格式）',
  `condition_expression` text COMMENT '条件表达式',
  `result_expression` text COMMENT '结果表达式',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_assessment_rule_template_id` (`template_id`),
  KEY `idx_assessment_rule_rule_code` (`rule_code`),
  KEY `idx_assessment_rule_rule_type` (`rule_type`),
  KEY `idx_assessment_rule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估规则表';

CREATE TABLE `assessment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `record_no` varchar(50) NOT NULL COMMENT '记录编号',
  `assessment_data` text COMMENT '评估数据（JSON格式）',
  `total_score` double DEFAULT NULL COMMENT '总分',
  `assessment_result` varchar(100) DEFAULT NULL COMMENT '评估结果',
  `risk_level` varchar(50) DEFAULT NULL COMMENT '风险等级',
  `risk_tips` text COMMENT '风险提示',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-已完成',
  `assessor_id` bigint NOT NULL COMMENT '评估人ID',
  `assessor_name` varchar(50) NOT NULL COMMENT '评估人姓名',
  `department_id` bigint DEFAULT NULL COMMENT '科室ID',
  `remark` text COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assessment_record_record_no` (`record_no`),
  KEY `idx_assessment_record_patient_id` (`patient_id`),
  KEY `idx_assessment_record_template_id` (`template_id`),
  KEY `idx_assessment_record_assessor_id` (`assessor_id`),
  KEY `idx_assessment_record_status` (`status`),
  KEY `idx_assessment_record_department_id` (`department_id`),
  KEY `idx_assessment_record_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估记录表';

CREATE TABLE `report_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) DEFAULT NULL COMMENT '模板编码',
  `assessment_template_id` bigint NOT NULL COMMENT '关联的评估模板ID',
  `report_type` varchar(20) NOT NULL COMMENT '报告类型：PDF/WORD',
  `title` varchar(200) DEFAULT NULL COMMENT '报告标题',
  `header_config` text COMMENT '页眉配置（JSON格式）',
  `footer_config` text COMMENT '页脚配置（JSON格式）',
  `sections` text COMMENT '报告章节配置（JSON格式）',
  `style_config` text COMMENT '样式配置（JSON格式）',
  `is_default` int NOT NULL DEFAULT 0 COMMENT '是否默认模板：0-否，1-是',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_template_assessment_template_id` (`assessment_template_id`),
  KEY `idx_report_template_report_type` (`report_type`),
  KEY `idx_report_template_status` (`status`),
  KEY `idx_report_template_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

CREATE TABLE `treatment_suggestion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_id` bigint NOT NULL COMMENT '患者ID',
  `assessment_record_id` bigint NOT NULL COMMENT '评估记录ID',
  `suggestion_no` varchar(50) NOT NULL COMMENT '建议编号',
  `suggestion_content` text NOT NULL COMMENT '诊疗建议内容',
  `generator_id` bigint DEFAULT NULL COMMENT '生成人ID',
  `generator_name` varchar(50) DEFAULT NULL COMMENT '生成人姓名',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0-已删除，1-有效',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_treatment_suggestion_patient_id` (`patient_id`),
  KEY `idx_treatment_suggestion_assessment_record_id` (`assessment_record_id`),
  KEY `idx_treatment_suggestion_create_time` (`create_time`),
  KEY `idx_treatment_suggestion_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊疗建议记录表';

CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作人用户名',
  `real_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `role` varchar(20) DEFAULT NULL COMMENT '操作人角色',
  `module` varchar(50) DEFAULT NULL COMMENT '模块，如 PATIENT / ASSESSMENT_RECORD',
  `action` varchar(50) DEFAULT NULL COMMENT '动作，如 CREATE / UPDATE / DELETE / SUBMIT',
  `target_type` varchar(50) DEFAULT NULL COMMENT '目标类型，如 PATIENT / ASSESSMENT_RECORD',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID',
  `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作者IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_module` (`module`),
  KEY `idx_operation_log_action` (`action`),
  KEY `idx_operation_log_target_type` (`target_type`),
  KEY `idx_operation_log_target_id` (`target_id`),
  KEY `idx_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
