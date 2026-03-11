package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.entity.AssessmentRule;
import com.medical.assessment.service.AiAssessmentService;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.AssessmentRuleService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.PatientService;
import com.medical.assessment.service.TemplateDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/assessment-conversation")
public class AssessmentConversationController {

    @Autowired
    private AssessmentTemplateService templateService;
    @Autowired
    private AssessmentFieldService fieldService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private AiAssessmentService aiAssessmentService;
    @Autowired
    private AssessmentRecordService assessmentRecordService;
    @Autowired
    private AssessmentRuleService assessmentRuleService;
    @Autowired
    private TemplateDepartmentService templateDepartmentService;

    @PostMapping("/generate-template")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> generateTemplate(@RequestBody GenerateTemplateRequest body) {
        if (body.getPatientId() == null) {
            return Result.error("patientId 不能为空");
        }
        if (body.getSymptomText() == null || body.getSymptomText().trim().isEmpty()) {
            return Result.error("symptomText 不能为空");
        }

        Patient patient = patientService.getById(body.getPatientId());
        if (patient == null || patient.getDeleted() == 1) {
            return Result.error("患者不存在");
        }
        patientService.enrichPatient(patient);

        Map<String, Object> templateDraft = aiAssessmentService.generateTemplateDraft(
                patient, body.getSymptomText(), patient.getDepartmentName()
        );

        String templateName = String.valueOf(templateDraft.get("templateName"));
        String category = String.valueOf(templateDraft.get("category"));
        String description = String.valueOf(templateDraft.get("description"));
        List<Map<String, Object>> fieldsDraft = castListMap(templateDraft.get("fields"));
        if (fieldsDraft.isEmpty()) {
            return Result.error("AI未生成有效模板字段");
        }

        AssessmentTemplate template = new AssessmentTemplate();
        template.setTemplateName(templateName);
        template.setTemplateCode("AI_" + System.currentTimeMillis());
        template.setCategory(category);
        template.setDescription(description);
        template.setTemplateContent("{}");
        template.setVersion(1);
        template.setStatus(1);
        template.setRemark("AI自动生成模板");
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);
        templateService.save(template);

        if (patient.getDepartmentId() != null) {
            templateDepartmentService.saveBindings(template.getId(), Collections.singletonList(patient.getDepartmentId()));
        }

        int sort = 1;
        for (Map<String, Object> fieldMap : fieldsDraft) {
            AssessmentField field = new AssessmentField();
            field.setTemplateId(template.getId());
            field.setFieldName(String.valueOf(fieldMap.get("fieldCode")));
            field.setFieldCode(String.valueOf(fieldMap.get("fieldCode")));
            field.setFieldType(String.valueOf(fieldMap.get("fieldType")));
            field.setFieldLabel(String.valueOf(fieldMap.get("fieldLabel")));
            field.setRequired(parseRequired(fieldMap.get("required")));
            field.setDefaultValue(null);
            field.setOptions(serializeOptions(fieldMap.get("options")));
            field.setValidationRule(null);
            field.setSortOrder(sort++);
            field.setGroupName("AI评估");
            field.setRemark("AI自动生成字段");
            field.setCreateTime(LocalDateTime.now());
            field.setUpdateTime(LocalDateTime.now());
            field.setDeleted(0);
            fieldService.save(field);
        }

        saveGeneratedRules(template.getId(), templateDraft);

        List<AssessmentField> savedFields = fieldService.getFieldsByTemplateId(template.getId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("templateId", template.getId());
        resp.put("templateName", template.getTemplateName());
        resp.put("category", template.getCategory());
        resp.put("description", template.getDescription());
        resp.put("fields", savedFields);
        return Result.success(resp);
    }

    private void saveGeneratedRules(Long templateId, Map<String, Object> templateDraft) {
        List<Map<String, Object>> scoringRules = castListMap(templateDraft.get("scoringRules"));
        List<Map<String, Object>> riskRules = castListMap(templateDraft.get("riskRules"));

        int priority = 1;

        for (Map<String, Object> sr : scoringRules) {
            String fieldCode = String.valueOf(sr.get("fieldCode"));
            Map<String, Object> scoreMap = castMap(sr.get("scoreMap"));
            List<Map<String, Object>> ranges = castListMap(sr.get("ranges"));

            if (!scoreMap.isEmpty()) {
                for (Map.Entry<String, Object> entry : scoreMap.entrySet()) {
                    String optionValue = entry.getKey();
                    double score = parseDoubleValue(entry.getValue(), 0);
                    if (score == 0) {
                        continue;
                    }

                    AssessmentRule rule = new AssessmentRule();
                    rule.setTemplateId(templateId);
                    rule.setRuleName(fieldCode + "=" + optionValue + " 得" + (int) score + "分");
                    rule.setRuleCode("SCORE_" + fieldCode + "_" + priority);
                    rule.setRuleType("SCORE");
                    rule.setConditionExpression("${" + fieldCode + "} == '" + escapeQuote(optionValue) + "'");
                    com.alibaba.fastjson2.JSONObject content = new com.alibaba.fastjson2.JSONObject();
                    content.put("score", score);
                    rule.setRuleContent(content.toJSONString());
                    rule.setPriority(priority++);
                    rule.setStatus(1);
                    rule.setRemark("AI自动生成评分规则");
                    rule.setCreateTime(LocalDateTime.now());
                    rule.setUpdateTime(LocalDateTime.now());
                    rule.setDeleted(0);
                    assessmentRuleService.save(rule);
                }
            } else if (!ranges.isEmpty()) {
                for (Map<String, Object> range : ranges) {
                    double min = parseDoubleValue(range.get("min"), 0);
                    double max = parseDoubleValue(range.get("max"), 999);
                    double score = parseDoubleValue(range.get("score"), 0);

                    AssessmentRule rule = new AssessmentRule();
                    rule.setTemplateId(templateId);
                    rule.setRuleName(fieldCode + " [" + (int) min + "-" + (int) max + ") 得" + (int) score + "分");
                    rule.setRuleCode("SCORE_" + fieldCode + "_" + priority);
                    rule.setRuleType("SCORE");
                    rule.setConditionExpression("${" + fieldCode + "} >= " + (int) min + " && ${" + fieldCode + "} < " + (int) max);
                    com.alibaba.fastjson2.JSONObject content = new com.alibaba.fastjson2.JSONObject();
                    content.put("score", score);
                    rule.setRuleContent(content.toJSONString());
                    rule.setPriority(priority++);
                    rule.setStatus(1);
                    rule.setRemark("AI自动生成评分规则");
                    rule.setCreateTime(LocalDateTime.now());
                    rule.setUpdateTime(LocalDateTime.now());
                    rule.setDeleted(0);
                    assessmentRuleService.save(rule);
                }
            }
        }

        for (Map<String, Object> rr : riskRules) {
            int minScore = (int) parseDoubleValue(rr.get("minScore"), 0);
            int maxScore = (int) parseDoubleValue(rr.get("maxScore"), 999);
            String riskLevel = String.valueOf(rr.get("riskLevel"));
            String riskTip = rr.get("riskTip") != null ? String.valueOf(rr.get("riskTip")) : "";

            String condition;
            if (maxScore >= 999) {
                condition = "totalScore >= " + minScore;
            } else {
                condition = "totalScore >= " + minScore + " && totalScore < " + maxScore;
            }

            AssessmentRule rule = new AssessmentRule();
            rule.setTemplateId(templateId);
            rule.setRuleName("风险等级-" + riskLevel);
            rule.setRuleCode("RISK_" + priority);
            rule.setRuleType("RISK");
            rule.setConditionExpression(condition);
            com.alibaba.fastjson2.JSONObject content = new com.alibaba.fastjson2.JSONObject();
            content.put("riskLevel", riskLevel);
            content.put("riskTip", riskTip);
            rule.setRuleContent(content.toJSONString());
            rule.setPriority(priority++);
            rule.setStatus(1);
            rule.setRemark("AI自动生成风险规则");
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            rule.setDeleted(0);
            assessmentRuleService.save(rule);
        }
    }

    private double parseDoubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String escapeQuote(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "\\'");
    }

    @PostMapping("/start")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> start(@RequestBody StartRequest body) {
        if (body.getTemplateId() == null) {
            return Result.error("templateId 不能为空");
        }
        if (body.getPatientId() == null) {
            return Result.error("patientId 不能为空");
        }

        AssessmentTemplate template = templateService.getById(body.getTemplateId());
        if (template == null || template.getDeleted() == 1) {
            return Result.error("模板不存在");
        }
        Patient patient = patientService.getById(body.getPatientId());
        if (patient == null || patient.getDeleted() == 1) {
            return Result.error("患者不存在");
        }
        List<AssessmentField> fields = fieldService.getFieldsByTemplateId(body.getTemplateId());

        Map<String, Object> currentData = body.getAssessmentData() == null ? new HashMap<>() : new HashMap<>(body.getAssessmentData());
        if (patient.getAge() != null && !currentData.containsKey("age")) {
            currentData.put("age", patient.getAge());
        }
        if (patient.getGender() != null && !currentData.containsKey("gender")) {
            currentData.put("gender", patient.getGender());
        }

        Map<String, Object> aiResult = aiAssessmentService.processConversationRound(
                patient, template, fields, Collections.emptyList(), currentData, "请开始评估对话并提出第一问"
        );
        mergeMappedData(currentData, castMap(aiResult.get("mappedDataDelta")), fields);
        List<String> missingFields = deriveMissingFields(fields, currentData);
        double completion = deriveCompletion(fields, missingFields);
        String assistantMessage = resolveAssistantQuestion(
                String.valueOf(aiResult.get("assistantMessage")),
                Collections.emptyList(),
                fields,
                currentData
        );

        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", UUID.randomUUID().toString());
        resp.put("assistantMessage", assistantMessage);
        resp.put("mappedData", currentData);
        resp.put("missingFields", missingFields);
        resp.put("completion", completion);
        resp.put("confidence", aiResult.get("confidence"));
        resp.put("needClarify", aiResult.get("needClarify"));
        resp.put("clarifyQuestion", aiResult.get("clarifyQuestion"));
        return Result.success(resp);
    }

    @PostMapping("/reply")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> reply(@RequestBody ReplyRequest body) {
        if (body.getTemplateId() == null) {
            return Result.error("templateId 不能为空");
        }
        if (body.getPatientId() == null) {
            return Result.error("patientId 不能为空");
        }
        if (body.getPatientMessage() == null || body.getPatientMessage().trim().isEmpty()) {
            return Result.error("patientMessage 不能为空");
        }

        AssessmentTemplate template = templateService.getById(body.getTemplateId());
        if (template == null || template.getDeleted() == 1) {
            return Result.error("模板不存在");
        }
        Patient patient = patientService.getById(body.getPatientId());
        if (patient == null || patient.getDeleted() == 1) {
            return Result.error("患者不存在");
        }
        List<AssessmentField> fields = fieldService.getFieldsByTemplateId(body.getTemplateId());

        Map<String, Object> currentData = body.getAssessmentData() == null ? new HashMap<>() : new HashMap<>(body.getAssessmentData());

        // 强行提取当前缺失字段，防止AI漏掉"无/没有"等简单回答
        AssessmentField currentTarget = null;
        for (AssessmentField field : fields) {
            if (field != null && field.getRequired() != null && field.getRequired() == 1
                    && isEmptyValue(currentData.get(field.getFieldCode()))) {
                currentTarget = field;
                break;
            }
        }
        if (currentTarget == null) {
            for (AssessmentField field : fields) {
                if (field != null && isEmptyValue(currentData.get(field.getFieldCode()))) {
                    currentTarget = field;
                    break;
                }
            }
        }

        Map<String, Object> aiResult = aiAssessmentService.processConversationRound(
                patient, template, fields, body.getMessages(), currentData, body.getPatientMessage()
        );
        Map<String, Object> mappedDelta = castMap(aiResult.get("mappedDataDelta"));
        
        // 强制保障补底：如果AI没有提取出目标字段的值，并且用户的回答很像直接回答
        if (currentTarget != null && !mappedDelta.containsKey(currentTarget.getFieldCode())) {
            String msg = body.getPatientMessage().trim();
            if (msg.length() <= 10) { // 简短回答直接赋值
                mappedDelta.put(currentTarget.getFieldCode(), msg);
            } else if (msg.contains("无") || msg.contains("没有") || msg.contains("正常") || msg.contains("否")) {
                mappedDelta.put(currentTarget.getFieldCode(), "无/正常");
            } else {
                // 如果还是很长，直接把原话填进去，强制推进状态机
                mappedDelta.put(currentTarget.getFieldCode(), msg);
            }
        }

        mergeMappedData(currentData, mappedDelta, fields);
        List<String> missingFields = deriveMissingFields(fields, currentData);
        double completion = deriveCompletion(fields, missingFields);
        String assistantMessage = resolveAssistantQuestion(
                String.valueOf(aiResult.get("assistantMessage")),
                body.getMessages(),
                fields,
                currentData
        );

        Map<String, Object> resp = new HashMap<>();
        resp.put("assistantMessage", assistantMessage);
        resp.put("mappedDataDelta", mappedDelta);
        resp.put("mappedData", currentData);
        resp.put("missingFields", missingFields);
        resp.put("completion", completion);
        resp.put("confidence", aiResult.get("confidence"));
        resp.put("needClarify", aiResult.get("needClarify"));
        resp.put("clarifyQuestion", aiResult.get("clarifyQuestion"));
        return Result.success(resp);
    }

    @PostMapping("/calculate-realtime")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> calculateRealtimeByAi(@RequestBody RealtimeCalculateRequest body) {
        if (body.getTemplateId() == null) {
            return Result.error("templateId 不能为空");
        }
        if (body.getPatientId() == null) {
            return Result.error("patientId 不能为空");
        }
        if (body.getAssessmentData() == null || body.getAssessmentData().isEmpty()) {
            return Result.error("assessmentData 不能为空");
        }
        Patient patient = patientService.getById(body.getPatientId());
        if (patient == null || patient.getDeleted() == 1) {
            return Result.error("患者不存在");
        }
        AssessmentTemplate template = templateService.getById(body.getTemplateId());
        if (template == null || template.getDeleted() == 1) {
            return Result.error("模板不存在");
        }
        Map<String, Object> result = aiAssessmentService.calculateFallbackResult(patient, template, body.getAssessmentData());
        return Result.success(result);
    }

    @PostMapping("/finalize")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<AssessmentRecord> finalizeConversation(@RequestBody FinalizeRequest body, HttpServletRequest request) {
        if (body.getTemplateId() == null) {
            return Result.error("templateId 不能为空");
        }
        if (body.getPatientId() == null) {
            return Result.error("patientId 不能为空");
        }
        if (body.getAssessmentData() == null || body.getAssessmentData().isEmpty()) {
            return Result.error("assessmentData 不能为空");
        }
        try {
            Long assessorId = (Long) request.getAttribute("userId");
            String assessorName = (String) request.getAttribute("realName");
            if (assessorName == null) {
                assessorName = (String) request.getAttribute("username");
            }
            Long departmentId = request.getAttribute("departmentId") != null
                    ? Long.valueOf(request.getAttribute("departmentId").toString()) : null;

            AssessmentRecord record = assessmentRecordService.finalizeConversationAssessment(
                    body.getPatientId(), body.getTemplateId(), body.getAssessmentData(), assessorId, assessorName, departmentId
            );
            return Result.success(record);
        } catch (Exception e) {
            return Result.error("提交对话评估失败: " + e.getMessage());
        }
    }

    @PostMapping("/recommend-template")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> recommendTemplate(@RequestBody RecommendTemplateRequest body) {
        if (body.getSymptomText() == null || body.getSymptomText().trim().isEmpty()) {
            return Result.error("symptomText 不能为空");
        }

        LambdaQueryWrapper<AssessmentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentTemplate::getDeleted, 0);
        wrapper.eq(AssessmentTemplate::getStatus, 1);
        List<AssessmentTemplate> templates = templateService.list(wrapper);
        if (templates == null || templates.isEmpty()) {
            return Result.error("当前暂无可用模板");
        }

        String text = body.getSymptomText().toLowerCase(Locale.ROOT);
        AssessmentTemplate best = null;
        int bestScore = Integer.MIN_VALUE;
        for (AssessmentTemplate t : templates) {
            int score = 0;
            score += tokenMatchScore(text, t.getTemplateName(), 5);
            score += tokenMatchScore(text, t.getCategory(), 3);
            score += tokenMatchScore(text, t.getDescription(), 2);
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("templateId", best != null ? best.getId() : null);
        resp.put("templateName", best != null ? best.getTemplateName() : null);
        resp.put("confidence", bestScore <= 0 ? 0.3 : Math.min(0.95, 0.5 + bestScore * 0.05));
        resp.put("reason", "已根据症状描述匹配最相关评估模板");
        return Result.success(resp);
    }

    private int tokenMatchScore(String text, String candidate, int weight) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return 0;
        }
        int score = 0;
        String candidateLower = candidate.toLowerCase(Locale.ROOT);
        if (text.contains(candidateLower) || candidateLower.contains(text)) {
            score += 2 * weight;
        }
        for (String token : splitTokens(candidateLower)) {
            if (token.length() < 2) {
                continue;
            }
            if (text.contains(token)) {
                score += weight;
            }
        }
        return score;
    }

    private int parseRequired(Object required) {
        if (required == null) {
            return 0;
        }
        String v = String.valueOf(required).trim();
        return "1".equals(v) || "true".equalsIgnoreCase(v) ? 1 : 0;
    }

    private String serializeOptions(Object optionsObj) {
        if (optionsObj == null) {
            return null;
        }
        if (optionsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) optionsObj;
            List<String> textList = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    String v = String.valueOf(o).trim();
                    if (!v.isEmpty()) {
                        textList.add(v);
                    }
                }
            }
            if (textList.isEmpty()) {
                return null;
            }
            return com.alibaba.fastjson2.JSON.toJSONString(textList);
        }
        String raw = String.valueOf(optionsObj).trim();
        return raw.isEmpty() ? null : raw;
    }

    private List<String> splitTokens(String text) {
        String cleaned = text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", " ").trim();
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(cleaned.split("\\s+"));
    }

    private void mergeMappedData(Map<String, Object> currentData, Map<String, Object> mappedDelta, List<AssessmentField> fields) {
        if (mappedDelta == null || mappedDelta.isEmpty()) {
            return;
        }
        Map<String, AssessmentField> fieldByCode = new HashMap<>();
        for (AssessmentField f : fields) {
            fieldByCode.put(f.getFieldCode(), f);
        }
        for (Map.Entry<String, Object> entry : mappedDelta.entrySet()) {
            AssessmentField field = fieldByCode.get(entry.getKey());
            if (field == null) {
                continue;
            }
            Object normalized = normalizeValue(entry.getValue(), field);
            if (normalized != null) {
                currentData.put(entry.getKey(), normalized);
            }
        }
    }

    private Object normalizeValue(Object value, AssessmentField field) {
        if (value == null) {
            return null;
        }
        String fieldType = field.getFieldType();
        if ("NUMBER".equals(fieldType)) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (Exception e) {
                return null;
            }
        }
        if ("CHECKBOX".equals(fieldType)) {
            if (value instanceof List) {
                return value;
            }
            String raw = String.valueOf(value).trim();
            if (raw.isEmpty()) {
                return Collections.emptyList();
            }
            if (raw.startsWith("[") && raw.endsWith("]")) {
                try {
                    return com.alibaba.fastjson2.JSON.parseArray(raw, String.class);
                } catch (Exception ignored) {
                }
            }
            return Arrays.asList(raw.split("[,，、]"));
        }
        if ("SELECT".equals(fieldType) || "RADIO".equals(fieldType)) {
            List<String> options = parseFieldOptions(field.getOptions());
            if (options.isEmpty()) {
                return String.valueOf(value).trim();
            }
            String normalized = normalizeSelectLikeValue(String.valueOf(value), options);
            // 如果没匹配上选项，强制返回原值，以免一直卡在同一个问题死循环
            return normalized.isEmpty() ? String.valueOf(value).trim() : normalized;
        }
        return value;
    }

    private String normalizeSelectLikeValue(String rawValue, List<String> options) {
        if (rawValue == null) {
            return "";
        }
        String v = rawValue.trim();
        if (v.isEmpty()) {
            return "";
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(v)) {
                return option;
            }
        }
        String compact = v.replaceAll("\\s+", "");
        if (containsOption(options, "是") || containsOption(options, "否")) {
            if (containsAny(compact, Arrays.asList("是", "有", "存在", "阳性", "会", "会的", "是的"))) {
                return findOption(options, "是");
            }
            if (containsAny(compact, Arrays.asList("否", "没有", "无", "不存在", "阴性", "不会", "不是"))) {
                return findOption(options, "否");
            }
        }
        for (String option : options) {
            String optCompact = option.replaceAll("\\s+", "");
            if (compact.contains(optCompact) || optCompact.contains(compact)) {
                return option;
            }
        }
        return "";
    }

    private boolean containsOption(List<String> options, String target) {
        for (String option : options) {
            if (option != null && option.trim().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private String findOption(List<String> options, String target) {
        for (String option : options) {
            if (option != null && option.trim().equals(target)) {
                return option;
            }
        }
        return "";
    }

    private boolean containsAny(String text, List<String> tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseFieldOptions(String optionsRaw) {
        if (optionsRaw == null || optionsRaw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> arr = com.alibaba.fastjson2.JSON.parseArray(optionsRaw, String.class);
            if (arr != null && !arr.isEmpty()) {
                return arr;
            }
        } catch (Exception ignored) {
        }
        List<String> result = new ArrayList<>();
        String[] parts = optionsRaw.split("[,，、]");
        for (String part : parts) {
            String v = part.trim();
            if (!v.isEmpty()) {
                result.add(v);
            }
        }
        return result;
    }

    private List<String> deriveMissingFields(List<AssessmentField> fields, Map<String, Object> currentData) {
        List<String> missing = new ArrayList<>();
        if (fields == null || fields.isEmpty()) {
            return missing;
        }
        for (AssessmentField field : fields) {
            if (field == null || field.getRequired() == null || field.getRequired() != 1) {
                continue;
            }
            Object value = currentData.get(field.getFieldCode());
            if (isEmptyValue(value)) {
                missing.add(field.getFieldCode());
            }
        }
        return missing;
    }

    private double deriveCompletion(List<AssessmentField> fields, List<String> missingRequiredFields) {
        if (fields == null || fields.isEmpty()) {
            return 0.0;
        }
        int requiredCount = 0;
        for (AssessmentField field : fields) {
            if (field != null && field.getRequired() != null && field.getRequired() == 1) {
                requiredCount++;
            }
        }
        if (requiredCount == 0) {
            return 1.0;
        }
        int done = requiredCount - (missingRequiredFields == null ? 0 : missingRequiredFields.size());
        if (done < 0) {
            done = 0;
        }
        return Math.round((done * 100.0 / requiredCount)) / 100.0;
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }

    private String resolveAssistantQuestion(String aiQuestion, List<Map<String, String>> history,
                                            List<AssessmentField> fields, Map<String, Object> currentData) {
        // 严格的状态机：找到第一个缺失的必填字段，直接提问。不再跳过刚刚问过的字段，依靠强制数据提取来避免死循环。
        AssessmentField target = null;
        for (AssessmentField field : fields) {
            if (field != null && field.getRequired() != null && field.getRequired() == 1
                    && isEmptyValue(currentData.get(field.getFieldCode()))) {
                target = field;
                break;
            }
        }
        if (target == null) {
            for (AssessmentField field : fields) {
                if (field != null && isEmptyValue(currentData.get(field.getFieldCode()))) {
                    target = field;
                    break;
                }
            }
        }
        if (target == null) {
            return "感谢配合，当前信息已基本完整。请确认是否完成本次评估。";
        }
        
        String label = target.getFieldLabel() == null ? target.getFieldCode() : target.getFieldLabel();
        
        // 尝试使用AI生成的润色提问，如果AI提问包含了当前目标字段，则使用AI提问以增加自然感
        if (aiQuestion != null && !aiQuestion.trim().isEmpty() && aiQuestion.contains(label) && aiQuestion.length() < 100) {
            return aiQuestion.trim();
        }

        if ("SELECT".equals(target.getFieldType()) || "RADIO".equals(target.getFieldType()) || "CHECKBOX".equals(target.getFieldType())) {
            List<String> options = parseFieldOptions(target.getOptions());
            if (!options.isEmpty()) {
                return "请问您的" + label + "是？可选：" + String.join("、", options);
            }
        }
        if (label.endsWith("情况")) {
            return "请描述一下您的" + label + "。";
        }
        return "请描述一下您的" + label + "情况。";
    }

    private boolean isRepeatedAssistantQuestion(String question, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        String normalized = normalizeSentence(question);
        int checked = 0;
        for (int i = history.size() - 1; i >= 0 && checked < 4; i--) {
            Map<String, String> msg = history.get(i);
            if (msg == null || !"assistant".equalsIgnoreCase(msg.get("role"))) {
                continue;
            }
            checked++;
            String content = normalizeSentence(msg.get("content"));
            if (!content.isEmpty() && content.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSentence(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s\\p{Punct}，。！？；：、“”‘’]", "").trim();
    }

    private String buildFallbackQuestion(List<AssessmentField> fields, Map<String, Object> currentData, List<Map<String, String>> history) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        String lastAssistant = lastAssistantQuestion(history);
        AssessmentField target = null;
        for (AssessmentField field : fields) {
            if (field != null && field.getRequired() != null && field.getRequired() == 1
                    && isEmptyValue(currentData.get(field.getFieldCode()))
                    && !containsLabel(lastAssistant, field.getFieldLabel())) {
                target = field;
                break;
            }
        }
        if (target == null) {
            for (AssessmentField field : fields) {
                if (field != null
                        && isEmptyValue(currentData.get(field.getFieldCode()))
                        && !containsLabel(lastAssistant, field.getFieldLabel())) {
                    target = field;
                    break;
                }
            }
        }
        if (target == null) {
            for (AssessmentField field : fields) {
                if (field != null && field.getRequired() != null && field.getRequired() == 1
                        && isEmptyValue(currentData.get(field.getFieldCode()))) {
                    target = field;
                    break;
                }
            }
        }
        if (target == null) {
            return "";
        }
        String label = target.getFieldLabel() == null ? target.getFieldCode() : target.getFieldLabel();
        if ("SELECT".equals(target.getFieldType()) || "RADIO".equals(target.getFieldType()) || "CHECKBOX".equals(target.getFieldType())) {
            List<String> options = parseFieldOptions(target.getOptions());
            if (!options.isEmpty()) {
                return "请问您的" + label + "是？可选：" + String.join("、", options);
            }
        }
        return "请继续描述一下您的" + label + "情况。";
    }

    private String lastAssistantQuestion(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, String> msg = history.get(i);
            if (msg != null && "assistant".equalsIgnoreCase(msg.get("role"))) {
                return msg.get("content") == null ? "" : msg.get("content");
            }
        }
        return "";
    }

    private boolean containsLabel(String question, String label) {
        if (question == null || label == null) {
            return false;
        }
        String q = normalizeSentence(question);
        String l = normalizeSentence(label);
        return !q.isEmpty() && !l.isEmpty() && q.contains(l);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListMap(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<?> raw = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    public static class StartRequest {
        private Long patientId;
        private Long templateId;
        private Map<String, Object> assessmentData;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public Map<String, Object> getAssessmentData() {
            return assessmentData;
        }

        public void setAssessmentData(Map<String, Object> assessmentData) {
            this.assessmentData = assessmentData;
        }
    }

    public static class ReplyRequest {
        private Long patientId;
        private Long templateId;
        private String patientMessage;
        private List<Map<String, String>> messages;
        private Map<String, Object> assessmentData;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getPatientMessage() {
            return patientMessage;
        }

        public void setPatientMessage(String patientMessage) {
            this.patientMessage = patientMessage;
        }

        public List<Map<String, String>> getMessages() {
            return messages;
        }

        public void setMessages(List<Map<String, String>> messages) {
            this.messages = messages;
        }

        public Map<String, Object> getAssessmentData() {
            return assessmentData;
        }

        public void setAssessmentData(Map<String, Object> assessmentData) {
            this.assessmentData = assessmentData;
        }
    }

    public static class FinalizeRequest {
        private Long patientId;
        private Long templateId;
        private Map<String, Object> assessmentData;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public Map<String, Object> getAssessmentData() {
            return assessmentData;
        }

        public void setAssessmentData(Map<String, Object> assessmentData) {
            this.assessmentData = assessmentData;
        }
    }

    public static class RecommendTemplateRequest {
        private String symptomText;

        public String getSymptomText() {
            return symptomText;
        }

        public void setSymptomText(String symptomText) {
            this.symptomText = symptomText;
        }
    }

    public static class GenerateTemplateRequest {
        private Long patientId;
        private String symptomText;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public String getSymptomText() {
            return symptomText;
        }

        public void setSymptomText(String symptomText) {
            this.symptomText = symptomText;
        }
    }

    public static class RealtimeCalculateRequest {
        private Long patientId;
        private Long templateId;
        private Map<String, Object> assessmentData;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public Map<String, Object> getAssessmentData() {
            return assessmentData;
        }

        public void setAssessmentData(Map<String, Object> assessmentData) {
            this.assessmentData = assessmentData;
        }
    }
}

