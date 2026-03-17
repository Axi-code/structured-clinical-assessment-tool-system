package com.medical.assessment.entity;

/**
 * 患者实体，对应患者基础信息表。
 * 记录患者的身份信息、联系方式、就诊科室及诊断等，是整个评估与诊疗流程的核心主体。
 * 主要会被 `PatientMapper`、`PatientService`/`PatientServiceImpl`、`PatientController`，
 * 以及与患者相关的评估记录、诊疗建议、统计分析等业务模块调用。
 */
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import com.medical.assessment.handler.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient")
public class Patient extends BaseEntity {
    private String patientNo; // 患者编号
    private String name;
    private String gender; // 男/女
    private LocalDate birthDate;
    private Integer age;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String idCard; // 身份证号（L3 加密存储）

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone; // 联系电话（L3 加密存储）

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String address; // 地址（L3 加密存储）

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String emergencyContact; // 紧急联系人（L3 加密存储）

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String emergencyPhone; // 紧急联系电话（L3 加密存储）

    private Long departmentId; // 科室ID
    private Long diagnosisId;  // 诊断ID（须属于该科室）

    @TableField(exist = false)
    private String departmentName; // 关联展示用
    @TableField(exist = false)
    private String diagnosisName;  // 关联展示用
    private String remark; // 备注
}

