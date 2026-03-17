package com.medical.assessment.entity;

/**
 * 诊断实体，对应诊断基础信息表。
 * 描述某个科室下的诊断项目、ICD 编码等，用于患者主诊断/初诊等场景的选择与展示。
 * 主要会被 `DiagnosisMapper`、`DiagnosisService`/`DiagnosisServiceImpl` 和 `DiagnosisController`
 * 以及涉及患者诊断信息的评估、统计等业务逻辑调用。
 */
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis")
public class Diagnosis extends BaseEntity {
    private String name;
    private Long departmentId;
    private String icdCode;
    private Integer sortOrder;
    private String remark;

    @TableField(exist = false)
    private String departmentName; // 关联查询用
}
