package com.medical.assessment.entity;

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

