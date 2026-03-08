package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.entity.TreatmentSuggestion;

import java.util.List;

/**
 * 诊疗建议记录服务接口
 */
public interface TreatmentSuggestionRecordService {
    
    /**
     * 保存诊疗建议记录
     */
    TreatmentSuggestion saveSuggestion(TreatmentSuggestion suggestion);
    
    /**
     * 根据ID获取诊疗建议
     */
    TreatmentSuggestion getById(Long id);
    
    /**
     * 根据患者ID获取诊疗建议列表
     */
    List<TreatmentSuggestion> getByPatientId(Long patientId);
    
    /**
     * 根据评估记录ID获取诊疗建议列表
     */
    List<TreatmentSuggestion> getByAssessmentRecordId(Long assessmentRecordId);
    
    /**
     * 分页查询诊疗建议
     */
    Page<TreatmentSuggestion> getPage(Long patientId, Integer current, Integer size);
    
    /**
     * 删除诊疗建议（逻辑删除）
     */
    boolean deleteById(Long id);
}
