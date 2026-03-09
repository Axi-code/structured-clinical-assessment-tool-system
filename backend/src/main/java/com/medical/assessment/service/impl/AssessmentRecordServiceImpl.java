package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.AssessmentRule;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.entity.TemplateDepartment;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.AssessmentRecordMapper;
import com.medical.assessment.mapper.TemplateDepartmentMapper;
import com.medical.assessment.service.AiAssessmentService;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.AssessmentRuleService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentRecordServiceImpl extends ServiceImpl<AssessmentRecordMapper, AssessmentRecord> implements AssessmentRecordService {
    
    @Autowired
    private AssessmentFieldService fieldService;
    
    @Autowired
    private AssessmentRuleService ruleService;
    @Autowired
    private AssessmentTemplateService templateService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private AiAssessmentService aiAssessmentService;
    @Autowired
    private TemplateDepartmentMapper templateDepartmentMapper;
    
    @Override
    @Transactional
    public AssessmentRecord createDraft(Long patientId, Long templateId, Long assessorId, String assessorName, Long departmentId) {
        Patient patient = patientService.getById(patientId);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        if (departmentId != null && patient.getDepartmentId() != null && !departmentId.equals(patient.getDepartmentId())) {
            throw new BusinessException("您只能评估本科室的患者");
        }
        Long checkDeptId = departmentId != null ? departmentId : patient.getDepartmentId();
        if (checkDeptId != null) {
            long tdCount = templateDepartmentMapper.selectCount(
                    new LambdaQueryWrapper<TemplateDepartment>()
                            .eq(TemplateDepartment::getTemplateId, templateId)
                            .eq(TemplateDepartment::getDepartmentId, checkDeptId));
            if (tdCount == 0) {
                throw new BusinessException("该模板不适用于您的科室");
            }
        }
        AssessmentRecord record = new AssessmentRecord();
        record.setPatientId(patientId);
        record.setTemplateId(templateId);
        record.setRecordNo("AR" + System.currentTimeMillis());
        record.setAssessorId(assessorId);
        record.setAssessorName(assessorName);
        record.setDepartmentId(departmentId);
        record.setStatus(0); // 草稿
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        record.setDeleted(0);
        save(record);
        return record;
    }
    
    @Override
    @Transactional
    public AssessmentRecord saveAssessmentData(Long recordId, Map<String, Object> assessmentData, Integer status) {
        AssessmentRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException("评估记录不存在");
        }
        
        // 验证数据
        if (status == 1) { // 完成状态需要验证
            validateAssessmentData(record.getTemplateId(), assessmentData);
        }
        
        record.setAssessmentData(JSON.toJSONString(assessmentData));
        record.setStatus(status);
        record.setUpdateTime(LocalDateTime.now());
        
        // 如果是完成状态，计算评估结果
        if (status == 1) {
            calculateAssessmentResult(record);
        }
        
        updateById(record);
        return record;
    }
    
    @Override
    @Transactional
    public AssessmentRecord submitAssessment(Long recordId, Map<String, Object> assessmentData) {
        return saveAssessmentData(recordId, assessmentData, 1);
    }

    @Override
    @Transactional
    public AssessmentRecord finalizeConversationAssessment(Long patientId, Long templateId, Map<String, Object> assessmentData,
                                                           Long assessorId, String assessorName, Long departmentId) {
        if (patientId == null) {
            throw new BusinessException("patientId 不能为空");
        }
        if (templateId == null) {
            throw new BusinessException("templateId 不能为空");
        }
        AssessmentRecord draft = createDraft(patientId, templateId, assessorId, assessorName, departmentId);
        return saveAssessmentData(draft.getId(), assessmentData, 1);
    }
    
    @Override
    public void validateAssessmentData(Long templateId, Map<String, Object> assessmentData) {
        List<AssessmentField> fields = fieldService.getFieldsByTemplateId(templateId);
        List<String> errors = new ArrayList<>();
        
        for (AssessmentField field : fields) {
            String fieldCode = field.getFieldCode();
            Object value = assessmentData.get(fieldCode);
            
            // 必填验证
            if (field.getRequired() == 1 && (value == null || value.toString().trim().isEmpty())) {
                errors.add(field.getFieldLabel() + "为必填项");
                continue;
            }
            
            if (value == null) {
                continue;
            }
            
            // 类型验证
            String fieldType = field.getFieldType();
            switch (fieldType) {
                case "NUMBER":
                    try {
                        Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        errors.add(field.getFieldLabel() + "必须是数字");
                    }
                    break;
                case "DATE":
                    // 日期格式验证可以在这里扩展
                    break;
                case "SELECT":
                case "RADIO":
                    if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                        List<String> options = JSON.parseArray(field.getOptions(), String.class);
                        if (!options.contains(value.toString())) {
                            errors.add(field.getFieldLabel() + "的值不在允许的选项中");
                        }
                    }
                    break;
            }
            
            // 自定义验证规则
            if (field.getValidationRule() != null && !field.getValidationRule().isEmpty()) {
                Map<String, Object> rules = JSON.parseObject(field.getValidationRule());
                // 这里可以扩展更多验证规则
                if (rules.containsKey("min") && Double.parseDouble(value.toString()) < Double.parseDouble(rules.get("min").toString())) {
                    errors.add(field.getFieldLabel() + "的值不能小于" + rules.get("min"));
                }
                if (rules.containsKey("max") && Double.parseDouble(value.toString()) > Double.parseDouble(rules.get("max").toString())) {
                    errors.add(field.getFieldLabel() + "的值不能大于" + rules.get("max"));
                }
            }
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }
    }
    
    @Override
    public void calculateAssessmentResult(AssessmentRecord record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> assessmentData = (Map<String, Object>) JSON.parseObject(record.getAssessmentData(), Map.class);
            Map<String, Object> result = calculateByRuleOrAi(record, assessmentData);
            applyResultToRecord(record, result);
        } catch (Exception e) {
            // 计算失败不影响保存，记录错误即可
            record.setRemark("计算评估结果时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public List<AssessmentRecord> getPatientHistory(Long patientId) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getPatientId, patientId);
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1); // 只查询已完成的记录
        wrapper.orderByDesc(AssessmentRecord::getCreateTime);
        List<AssessmentRecord> records = list(wrapper);
        records.forEach(this::hydrateAiDiagnosisName);
        return records;
    }

    @Override
    public AssessmentRecord getLatestCompletedRecord(Long patientId) {
        if (patientId == null) {
            return null;
        }
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getPatientId, patientId);
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.orderByDesc(AssessmentRecord::getCreateTime);
        wrapper.last("limit 1");
        AssessmentRecord record = getOne(wrapper, false);
        hydrateAiDiagnosisName(record);
        return record;
    }
    
    @Override
    public Map<String, Object> compareRecords(List<Long> recordIds) {
        Map<String, Object> result = new HashMap<>();
        List<AssessmentRecord> records = listByIds(recordIds);
        
        if (records.isEmpty()) {
            return result;
        }
        
        // 按时间排序
        records.sort(Comparator.comparing(AssessmentRecord::getCreateTime));
        
        // 提取对比数据
        List<Map<String, Object>> comparisonData = new ArrayList<>();
        for (AssessmentRecord record : records) {
            Map<String, Object> data = new HashMap<>();
            data.put("recordId", record.getId());
            data.put("recordNo", record.getRecordNo());
            data.put("createTime", record.getCreateTime());
            data.put("totalScore", record.getTotalScore());
            data.put("assessmentResult", record.getAssessmentResult());
            data.put("riskLevel", record.getRiskLevel());
            data.put("assessmentData", JSON.parseObject(record.getAssessmentData(), Map.class));
            comparisonData.add(data);
        }
        
        result.put("records", comparisonData);
        result.put("count", records.size());
        
        return result;
    }

    private Map<String, Object> calculateByRuleOrAi(AssessmentRecord record, Map<String, Object> assessmentData) {
        Long templateId = record.getTemplateId();
        AssessmentTemplate template = templateId != null ? templateService.getById(templateId) : null;
        Patient patient = patientService.getById(record.getPatientId());

        boolean useRuleEngine = hasUsableRules(templateId);
        if (useRuleEngine) {
            try {
                return ruleService.executeAllRules(templateId, assessmentData);
            } catch (Exception ignored) {
                // 规则执行异常时降级走 AI 兜底
            }
        }
        return aiAssessmentService.calculateFallbackResult(patient, template, assessmentData);
    }

    private boolean hasUsableRules(Long templateId) {
        if (templateId == null) {
            return false;
        }
        List<AssessmentRule> rules = ruleService.getRulesByTemplateId(templateId);
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (AssessmentRule rule : rules) {
            if (rule == null || rule.getRuleType() == null) {
                continue;
            }
            String type = rule.getRuleType().trim().toUpperCase(Locale.ROOT);
            if ("SCORE".equals(type) || "RISK".equals(type) || "CALCULATE".equals(type)) {
                return true;
            }
        }
        return false;
    }

    private void applyResultToRecord(AssessmentRecord record, Map<String, Object> result) {
        if (result == null) {
            return;
        }
        if (result.containsKey("totalScore") && result.get("totalScore") != null) {
            try {
                record.setTotalScore(Double.parseDouble(result.get("totalScore").toString()));
            } catch (Exception ignored) {
                record.setTotalScore(0.0);
            }
        }
        if (result.containsKey("assessmentResult") && result.get("assessmentResult") != null) {
            record.setAssessmentResult(result.get("assessmentResult").toString());
        }
        if (result.containsKey("diagnosisName") && result.get("diagnosisName") != null) {
            String aiDiagnosisName = result.get("diagnosisName").toString();
            record.setAiDiagnosisName(aiDiagnosisName);
            record.setRemark(upsertAiDiagnosisRemark(record.getRemark(), aiDiagnosisName));
        }
        if (result.containsKey("riskLevel") && result.get("riskLevel") != null) {
            record.setRiskLevel(result.get("riskLevel").toString());
        }
        if (result.containsKey("riskTips") && result.get("riskTips") != null) {
            record.setRiskTips(result.get("riskTips").toString());
        }

        if (result.containsKey("abnormalDataTips")) {
            Object abnormalObj = result.get("abnormalDataTips");
            List<String> abnormalTips = new ArrayList<>();
            if (abnormalObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) abnormalObj;
                abnormalTips = items.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
            }
            if (!abnormalTips.isEmpty()) {
                String existingRemark = record.getRemark();
                String abnormalRemark = "异常数据提示: " + String.join("; ", abnormalTips);
                if (existingRemark != null && !existingRemark.isEmpty()) {
                    record.setRemark(existingRemark + "\n" + abnormalRemark);
                } else {
                    record.setRemark(abnormalRemark);
                }
            }
        }

        if (record.getStatus() != null && record.getStatus() == 1 && record.getPatientId() != null) {
            patientService.autoApplyDiagnosisByName(record.getPatientId(), record.getAiDiagnosisName());
        }
    }

    private void hydrateAiDiagnosisName(AssessmentRecord record) {
        if (record == null) {
            return;
        }
        record.setAiDiagnosisName(extractAiDiagnosisName(record.getRemark()));
    }

    private String extractAiDiagnosisName(String remark) {
        if (remark == null || remark.trim().isEmpty()) {
            return null;
        }
        String[] lines = remark.split("\\r?\\n");
        for (String line : lines) {
            if (line != null && line.startsWith(AssessmentRecord.AI_DIAGNOSIS_REMARK_PREFIX)) {
                String value = line.substring(AssessmentRecord.AI_DIAGNOSIS_REMARK_PREFIX.length()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private String upsertAiDiagnosisRemark(String remark, String aiDiagnosisName) {
        if (aiDiagnosisName == null || aiDiagnosisName.trim().isEmpty()) {
            return remark;
        }
        String aiRemark = AssessmentRecord.AI_DIAGNOSIS_REMARK_PREFIX + aiDiagnosisName.trim();
        if (remark == null || remark.trim().isEmpty()) {
            return aiRemark;
        }
        List<String> lines = Arrays.stream(remark.split("\\r?\\n"))
                .filter(line -> line != null && !line.startsWith(AssessmentRecord.AI_DIAGNOSIS_REMARK_PREFIX))
                .collect(Collectors.toList());
        lines.add(0, aiRemark);
        return String.join("\n", lines);
    }

}

