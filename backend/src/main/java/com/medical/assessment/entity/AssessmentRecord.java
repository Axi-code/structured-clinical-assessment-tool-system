package com.medical.assessment.entity;

/**
 * 评估记录实体，对应一次完整的评估过程及其结果。
 * 保存患者在某个评估模板下的填报数据、评分结果、风险等级等信息。
 * 主要会被 `AssessmentRecordMapper`、`AssessmentRecordService`/`AssessmentRecordServiceImpl`、
 * 以及对外提供评估记录查询/保存能力的 `AssessmentRecordController` 和统计报表相关业务调用。
 */
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_record")
public class AssessmentRecord extends BaseEntity {
    public static final String AI_DIAGNOSIS_REMARK_PREFIX = "AI建议诊断: ";

    private Long patientId; // 患者ID
    private Long templateId; // 模板ID
    private String recordNo; // 评估记录编号
    private String assessmentData; // 评估数据(JSON格式)
    private Double totalScore; // 总分
    private String assessmentResult; // 评估结果

    @TableField(exist = false)
    private String aiDiagnosisName; // AI建议诊断名称
    private String riskLevel; // 风险等级
    private String riskTips; // 风险提示
    private Integer status; // 0-草稿 1-已完成
    private Long assessorId; // 评估人ID
    private String assessorName; // 评估人姓名
    private Long departmentId; // 科室ID
    private String remark; // 备注

    @TableField(exist = false)
    private String departmentName; // 关联展示用
}

