package com.medical.assessment.service;

import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.TreatmentSuggestion;

/**
 * 诊疗建议服务接口
 */
public interface TreatmentSuggestionService {
    /**
     * 生成诊疗建议
     * @param recordId 评估记录ID
     * @param generatorId 生成人ID
     * @param generatorName 生成人姓名
     * @return 诊疗建议记录
     */
    TreatmentSuggestion generateTreatmentSuggestion(Long recordId, Long generatorId, String generatorName);
    
    /**
     * 生成诊疗建议（使用自定义记录）
     * @param record 评估记录
     * @param generatorId 生成人ID
     * @param generatorName 生成人姓名
     * @return 诊疗建议记录
     */
    TreatmentSuggestion generateTreatmentSuggestion(AssessmentRecord record, Long generatorId, String generatorName);
}
