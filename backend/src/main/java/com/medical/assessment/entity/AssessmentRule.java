package com.medical.assessment.entity;

/**
 * 评估规则实体，对应某个评估模板下的评分/风险/计算规则表。
 * 通过规则内容与表达式，定义如何根据表单字段计算得分、风险等级或其他派生结果。
 * 主要会被 `AssessmentRuleMapper`、`AssessmentRuleService`/`AssessmentRuleServiceImpl`、
 * 以及实时计算接口 `AssessmentRuleController`、评估会话服务和报告生成等业务逻辑调用。
 */
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

