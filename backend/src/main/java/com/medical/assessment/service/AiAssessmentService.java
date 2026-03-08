package com.medical.assessment.service;

import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;

import java.util.List;
import java.util.Map;

public interface AiAssessmentService {
    /**
     * 对话轮次：根据消息和模板字段抽取结构化数据，并返回下一问
     */
    Map<String, Object> processConversationRound(Patient patient,
                                                 AssessmentTemplate template,
                                                 List<AssessmentField> fields,
                                                 List<Map<String, String>> messages,
                                                 Map<String, Object> currentAssessmentData,
                                                 String latestUserMessage);

    /**
     * 当模板缺少合适规则时，使用 AI 兜底生成评估结果
     */
    Map<String, Object> calculateFallbackResult(Patient patient,
                                                AssessmentTemplate template,
                                                Map<String, Object> assessmentData);

    /**
     * 根据主诉自动生成可用于评估的模板草案（字段定义）
     */
    Map<String, Object> generateTemplateDraft(Patient patient,
                                              String symptomText,
                                              String departmentName);
}

