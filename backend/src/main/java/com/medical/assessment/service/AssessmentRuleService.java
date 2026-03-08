package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.entity.AssessmentRule;

import java.util.List;
import java.util.Map;

public interface AssessmentRuleService extends IService<AssessmentRule> {
    /**
     * 根据模板ID获取规则列表
     */
    List<AssessmentRule> getRulesByTemplateId(Long templateId);
    
    /**
     * 执行评分规则
     */
    Double calculateScore(Long templateId, Map<String, Object> assessmentData);
    
    /**
     * 执行风险规则
     */
    Map<String, Object> calculateRisk(Long templateId, Map<String, Object> assessmentData);
    
    /**
     * 执行计算规则
     */
    Map<String, Object> executeCalculation(Long templateId, Map<String, Object> assessmentData);
    
    /**
     * 执行所有规则
     */
    Map<String, Object> executeAllRules(Long templateId, Map<String, Object> assessmentData);
    
    /**
     * 测试表达式（用于规则配置时的表达式验证）
     */
    Map<String, Object> testExpression(String conditionExpression, String resultExpression, Map<String, Object> testData);
    
    /**
     * 实时计算评估结果（不保存数据）
     */
    Map<String, Object> calculateRealtime(Long templateId, Map<String, Object> assessmentData);
}

