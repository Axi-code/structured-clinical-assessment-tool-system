package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.Department;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.entity.TreatmentSuggestion;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.DepartmentService;
import com.medical.assessment.service.PatientService;
import com.medical.assessment.service.QwenService;
import com.medical.assessment.service.TreatmentSuggestionRecordService;
import com.medical.assessment.service.TreatmentSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 诊疗建议服务实现
 */
@Service
public class TreatmentSuggestionServiceImpl implements TreatmentSuggestionService {
    
    @Autowired
    private AssessmentRecordService recordService;
    
    @Autowired
    private PatientService patientService;
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private AssessmentTemplateService templateService;
    
    @Autowired
    private AssessmentFieldService fieldService;
    
    @Autowired
    private QwenService qwenService;
    
    @Autowired
    private TreatmentSuggestionRecordService suggestionRecordService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public TreatmentSuggestion generateTreatmentSuggestion(Long recordId, Long generatorId, String generatorName) {
        AssessmentRecord record = recordService.getById(recordId);
        if (record == null) {
            throw new RuntimeException("评估记录不存在");
        }
        enrichRecordDepartment(record);
        return generateTreatmentSuggestion(record, generatorId, generatorName);
    }

    private void enrichRecordDepartment(AssessmentRecord record) {
        if (record != null && record.getDepartmentId() != null) {
            Department d = departmentService.getById(record.getDepartmentId());
            record.setDepartmentName(d != null ? d.getName() : "未知");
        }
    }
    
    @Override
    public TreatmentSuggestion generateTreatmentSuggestion(AssessmentRecord record, Long generatorId, String generatorName) {
        try {
            enrichRecordDepartment(record);
            // 获取患者信息
            Patient patient = patientService.getById(record.getPatientId());
            patientService.enrichPatient(patient);
            
            // 获取评估模板
            AssessmentTemplate template = templateService.getById(record.getTemplateId());
            
            // 获取评估字段
            List<AssessmentField> fields = fieldService.getFieldsByTemplateId(record.getTemplateId());
            
            // 解析评估数据
            Map<String, Object> assessmentData = JSON.parseObject(record.getAssessmentData(), Map.class);
            
            // 获取患者历史评估记录
            List<AssessmentRecord> historyRecords = recordService.getPatientHistory(record.getPatientId());
            
            // 构建提示词
            String prompt = buildPrompt(patient, template, record, fields, assessmentData, historyRecords);
            
            // 调用Qwen模型生成建议
            String suggestionContent = qwenService.generateText(prompt);
            
            // 保存诊疗建议记录
            TreatmentSuggestion suggestion = new TreatmentSuggestion();
            suggestion.setPatientId(record.getPatientId());
            suggestion.setAssessmentRecordId(record.getId());
            suggestion.setSuggestionContent(suggestionContent);
            suggestion.setGeneratorId(generatorId);
            suggestion.setGeneratorName(generatorName);
            suggestion.setStatus(1);
            
            return suggestionRecordService.saveSuggestion(suggestion);
        } catch (Exception e) {
            throw new RuntimeException("生成诊疗建议失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(Patient patient, AssessmentTemplate template, 
                              AssessmentRecord record, List<AssessmentField> fields,
                              Map<String, Object> assessmentData, List<AssessmentRecord> historyRecords) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位经验丰富的临床医生。请根据以下患者信息和评估数据，生成个性化的诊疗建议或治疗方案。\n\n");
        
        // 患者基本信息（不传姓名、身份证、手机、地址等可识别个人的信息，仅传临床所需字段）
        prompt.append("## 患者基本信息\n");
        if (patient != null) {
            prompt.append(String.format("- 性别：%s\n", patient.getGender() != null ? patient.getGender() : "未知"));
            prompt.append(String.format("- 年龄：%s\n", patient.getAge() != null ? patient.getAge() : "未知"));
            prompt.append(String.format("- 科室：%s\n", patient.getDepartmentName() != null ? patient.getDepartmentName() : "未知"));
            prompt.append(String.format("- 诊断：%s\n", patient.getDiagnosisName() != null ? patient.getDiagnosisName() : "未明确"));
        }
        prompt.append("\n");
        
        // 当前评估信息
        prompt.append("## 当前评估信息\n");
        prompt.append(String.format("- 评估模板：%s\n", template != null ? template.getTemplateName() : "未知"));
        prompt.append(String.format("- 评估时间：%s\n", record.getCreateTime() != null ? record.getCreateTime().format(DATE_FORMATTER) : "未知"));
        prompt.append(String.format("- 评估人：%s\n", record.getAssessorName() != null ? record.getAssessorName() : "未知"));
        prompt.append(String.format("- 评估科室：%s\n", record.getDepartmentName() != null ? record.getDepartmentName() : "未知"));
        prompt.append("\n");
        
        // 评估数据详情
        prompt.append("## 评估数据详情\n");
        if (assessmentData != null && !assessmentData.isEmpty()) {
            for (AssessmentField field : fields) {
                Object value = assessmentData.get(field.getFieldCode());
                if (value != null) {
                    String valueStr = formatFieldValue(value);
                    prompt.append(String.format("- %s：%s\n", field.getFieldLabel(), valueStr));
                }
            }
        }
        prompt.append("\n");
        
        // 评估结果
        prompt.append("## 评估结果\n");
        if (record.getTotalScore() != null) {
            prompt.append(String.format("- 总分：%.1f分\n", record.getTotalScore()));
        }
        if (record.getAssessmentResult() != null && !record.getAssessmentResult().isEmpty()) {
            prompt.append(String.format("- 评估结果：%s\n", record.getAssessmentResult()));
        }
        if (record.getRiskLevel() != null && !record.getRiskLevel().isEmpty()) {
            prompt.append(String.format("- 风险等级：%s\n", record.getRiskLevel()));
        }
        if (record.getRiskTips() != null && !record.getRiskTips().isEmpty()) {
            prompt.append(String.format("- 风险提示：%s\n", record.getRiskTips()));
        }
        prompt.append("\n");
        
        // 历史评估记录
        if (historyRecords != null && !historyRecords.isEmpty()) {
            prompt.append("## 历史评估记录\n");
            prompt.append(String.format("患者共有 %d 次历史评估记录：\n", historyRecords.size()));
            for (int i = 0; i < Math.min(historyRecords.size(), 5); i++) {
                AssessmentRecord history = historyRecords.get(i);
                prompt.append(String.format("\n### 第%d次评估（%s）\n", i + 1, 
                    history.getCreateTime() != null ? history.getCreateTime().format(DATE_FORMATTER) : "未知"));
                if (history.getTotalScore() != null) {
                    prompt.append(String.format("- 总分：%.1f分\n", history.getTotalScore()));
                }
                if (history.getRiskLevel() != null && !history.getRiskLevel().isEmpty()) {
                    prompt.append(String.format("- 风险等级：%s\n", history.getRiskLevel()));
                }
                if (history.getAssessmentResult() != null && !history.getAssessmentResult().isEmpty()) {
                    prompt.append(String.format("- 评估结果：%s\n", history.getAssessmentResult()));
                }
            }
            prompt.append("\n");
        }
        
        // 生成建议的要求
        prompt.append("## 请根据以上信息生成诊疗建议\n\n");
        prompt.append("请按照以下结构生成个性化的诊疗建议或治疗方案：\n");
        prompt.append("1. **病情分析**：简要分析患者的当前状况和评估结果\n");
        prompt.append("2. **治疗建议**：提供具体的治疗建议，包括药物治疗、物理治疗、生活方式调整等\n");
        prompt.append("3. **注意事项**：列出需要注意的事项和禁忌\n");
        prompt.append("4. **随访建议**：建议的随访时间和复查项目\n");
        prompt.append("5. **预后评估**：对患者预后的评估\n\n");
        prompt.append("请确保建议：\n");
        prompt.append("- 基于患者的具体情况，具有针对性\n");
        prompt.append("- 考虑患者的历史评估数据，体现动态调整\n");
        prompt.append("- 符合临床诊疗规范\n");
        prompt.append("- 语言专业但易懂\n");
        prompt.append("- 建议具体可操作\n\n");
        prompt.append("请开始生成诊疗建议：");
        
        return prompt.toString();
    }
    
    /**
     * 格式化字段值
     */
    private String formatFieldValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(Object::toString)
                .collect(Collectors.joining("、"));
        }
        if (value instanceof Map) {
            return JSON.toJSONString(value);
        }
        return value.toString();
    }
}
