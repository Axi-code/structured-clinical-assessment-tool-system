package com.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {
    /** 操作人ID */
    private Long userId;
    /** 操作人用户名 */
    private String username;
    /** 操作人姓名 */
    private String realName;
    /** 操作人角色 */
    private String role;
    /** 模块，如 PATIENT / ASSESSMENT_RECORD */
    private String module;
    /** 动作，如 CREATE / UPDATE / DELETE / SUBMIT */
    private String action;
    /** 目标类型，如 PATIENT / ASSESSMENT_RECORD */
    private String targetType;
    /** 目标ID */
    private Long targetId;
    /** 操作描述 */
    private String description;
    /** 操作者IP */
    private String ip;
}
