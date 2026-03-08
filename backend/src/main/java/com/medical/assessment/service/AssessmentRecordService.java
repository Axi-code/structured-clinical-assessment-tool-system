package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.entity.AssessmentRecord;

import java.util.List;
import java.util.Map;

public interface AssessmentRecordService extends IService<AssessmentRecord> {
    /**
     * 创建评估记录（草稿）
     */
    AssessmentRecord createDraft(Long patientId, Long templateId, Long assessorId, String assessorName, Long departmentId);
    
    /**
     * 保存评估数据（草稿或完成）
     */
    AssessmentRecord saveAssessmentData(Long recordId, Map<String, Object> assessmentData, Integer status);
    
    /**
     * 提交评估记录（完成评估）
     */
    AssessmentRecord submitAssessment(Long recordId, Map<String, Object> assessmentData);

    /**
     * 对话式评估：直接创建并完成评估记录
     */
    AssessmentRecord finalizeConversationAssessment(Long patientId, Long templateId, Map<String, Object> assessmentData,
                                                    Long assessorId, String assessorName, Long departmentId);
    
    /**
     * 验证评估数据
     */
    void validateAssessmentData(Long templateId, Map<String, Object> assessmentData);
    
    /**
     * 自动计算评估结果
     */
    void calculateAssessmentResult(AssessmentRecord record);
    
    /**
     * 获取患者的历史评估记录
     */
    List<AssessmentRecord> getPatientHistory(Long patientId);
    
    /**
     * 获取评估记录对比数据
     */
    Map<String, Object> compareRecords(List<Long> recordIds);
}

