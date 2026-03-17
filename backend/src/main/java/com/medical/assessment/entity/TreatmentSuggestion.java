package com.medical.assessment.entity;

/**
 * 诊疗建议记录实体，对应基于评估结果生成的诊疗建议表。
 * 记录针对某次评估/某个患者生成的具体诊疗建议内容及生成者信息，用于临床决策与随访。
 * 主要会被 `TreatmentSuggestionMapper`、`TreatmentSuggestionService`/`TreatmentSuggestionServiceImpl`、
 * 以及 `TreatmentSuggestionController` 和评估/AI 建议生成相关业务逻辑调用。
 */
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 诊疗建议记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("treatment_suggestion")
public class TreatmentSuggestion extends BaseEntity {
    private Long patientId; // 患者ID
    private Long assessmentRecordId; // 评估记录ID
    private String suggestionNo; // 建议编号
    private String suggestionContent; // 诊疗建议内容
    private Long generatorId; // 生成人ID
    private String generatorName; // 生成人姓名
    private Integer status; // 状态：0-已删除，1-有效
    private String remark; // 备注
}
