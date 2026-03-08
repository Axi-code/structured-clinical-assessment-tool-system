package com.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_rule")
public class AssessmentRule extends BaseEntity {
    private Long templateId; // 模板ID
    private String ruleName; // 规则名称
    private String ruleCode; // 规则编码
    private String ruleType; // 规则类型: SCORE(评分), RISK(风险), CALCULATE(计算)
    private String ruleContent; // 规则内容(JSON格式)
    private String conditionExpression; // 条件表达式
    private String resultExpression; // 结果表达式
    private Integer priority; // 优先级
    private Integer status; // 0-停用 1-启用
    private String remark; // 备注
}

