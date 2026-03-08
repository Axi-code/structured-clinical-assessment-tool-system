package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.service.AiAssessmentService;
import com.medical.assessment.service.QwenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssessmentServiceImpl implements AiAssessmentService {

    @Autowired
    private QwenService qwenService;

    @Override
    public Map<String, Object> processConversationRound(Patient patient,
                                                        AssessmentTemplate template,
                                                        List<AssessmentField> fields,
                                                        List<Map<String, String>> messages,
                                                        Map<String, Object> currentAssessmentData,
                                                        String latestUserMessage) {
        String lastAssistantQuestion = "";
        if (messages != null && !messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("assistant".equals(messages.get(i).get("role"))) {
                    lastAssistantQuestion = messages.get(i).get("content");
                    break;
                }
            }
        }
        String prompt = buildConversationPrompt(patient, template, fields, messages, currentAssessmentData, latestUserMessage, lastAssistantQuestion);
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.2);
        params.put("max_tokens", 1200);

        String raw = qwenService.generateText(prompt, params);
        JSONObject json = parseJsonSafely(raw);

        Map<String, Object> result = new HashMap<>();
        result.put("assistantMessage", json.getString("assistant_question"));
        result.put("completion", json.getDoubleValue("completion"));
        result.put("confidence", json.getDoubleValue("confidence"));
        result.put("needClarify", json.getBooleanValue("need_clarify"));
        result.put("clarifyQuestion", json.getString("clarify_question"));

        JSONObject mappedDataDelta = json.getJSONObject("mapped_data_delta");
        if (mappedDataDelta == null) {
            mappedDataDelta = new JSONObject();
        }
        result.put("mappedDataDelta", mappedDataDelta);

        JSONArray missingFields = json.getJSONArray("missing_fields");
        if (missingFields == null) {
            missingFields = new JSONArray();
        }
        result.put("missingFields", missingFields);
        return result;
    }

    @Override
    public Map<String, Object> calculateFallbackResult(Patient patient,
                                                       AssessmentTemplate template,
                                                       Map<String, Object> assessmentData) {
        String prompt = buildFallbackCalculationPrompt(patient, template, assessmentData);
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.1);
        params.put("max_tokens", 1000);

        String raw = qwenService.generateText(prompt, params);
        JSONObject json = parseJsonSafely(raw);

        Map<String, Object> result = new HashMap<>();
        if (json.containsKey("totalScore")) {
            result.put("totalScore", json.getDouble("totalScore"));
        }
        result.put("assessmentResult", json.getString("assessmentResult"));
        result.put("riskLevel", json.getString("riskLevel"));
        result.put("riskTips", json.getString("riskTips"));
        result.put("diagnosisName", json.getString("diagnosisName"));

        JSONArray abnormalDataTips = json.getJSONArray("abnormalDataTips");
        if (abnormalDataTips != null) {
            result.put("abnormalDataTips", abnormalDataTips.toJavaList(String.class));
        } else {
            result.put("abnormalDataTips", Collections.emptyList());
        }
        return result;
    }

    @Override
    public Map<String, Object> generateTemplateDraft(Patient patient, String symptomText, String departmentName) {
        String prompt = buildTemplateDraftPrompt(patient, symptomText, departmentName);
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.3);
        params.put("max_tokens", 1800);

        String raw = qwenService.generateText(prompt, params);
        JSONObject json = parseJsonSafely(raw);
        JSONObject normalized = normalizeTemplateDraft(json, symptomText);

        Map<String, Object> result = new HashMap<>();
        result.put("templateName", normalized.getString("templateName"));
        result.put("category", normalized.getString("category"));
        result.put("description", normalized.getString("description"));
        result.put("fields", normalized.getJSONArray("fields"));
        return result;
    }

    private String buildConversationPrompt(Patient patient,
                                           AssessmentTemplate template,
                                           List<AssessmentField> fields,
                                           List<Map<String, String>> messages,
                                           Map<String, Object> currentAssessmentData,
                                           String latestUserMessage,
                                           String lastAssistantQuestion) {
        String fieldSpec = fields.stream().map(f -> {
            String options = f.getOptions() == null ? "" : f.getOptions();
            return String.format(
                    Locale.ROOT,
                    "- fieldCode=%s, fieldLabel=%s, fieldType=%s, required=%s, options=%s",
                    safe(f.getFieldCode()), safe(f.getFieldLabel()), safe(f.getFieldType()),
                    f.getRequired() != null && f.getRequired() == 1 ? "true" : "false", safe(options)
            );
        }).collect(Collectors.joining("\n"));

        String history = (messages == null || messages.isEmpty())
                ? "[]"
                : JSON.toJSONString(messages);

        return "你是智安临评助手，服务于结构化临床评估工具系统，目标是把患者自由文本回答映射为结构化评估数据，并继续提问。\n"
                + "必须严格输出 JSON，不要输出 markdown，不要输出额外说明。\n"
                + "输出 JSON schema:\n"
                + "{\n"
                + "  \"assistant_question\": \"string\",\n"
                + "  \"mapped_data_delta\": {\"fieldCode\":\"value\"},\n"
                + "  \"missing_fields\": [\"fieldCode\"],\n"
                + "  \"completion\": 0.0,\n"
                + "  \"confidence\": 0.0,\n"
                + "  \"need_clarify\": false,\n"
                + "  \"clarify_question\": \"string\"\n"
                + "}\n\n"
                + "规则:\n"
                + "1) 只允许映射到给定 fieldCode；\n"
                + "2) 对 SELECT/RADIO/CHECKBOX 字段，只能输出 options 内值；\n"
                + "3) NUMBER 输出数字；DATE 输出 yyyy-MM-dd；\n"
                + "4) assistant_question 用自然口吻继续问缺失且关键字段，一次问 1-2 个；\n"
                + "5) 若用户回答不明确，need_clarify=true 并给 clarify_question；\n"
                + "6) completion 在 0~1 之间，按 required 字段完成度估计；\n"
                + "7) 严禁重复提问已经问过的问题，必须推进到下一个缺失字段；\n"
                + "8) 对'有/没有/是/否/无'等口语回答做标准化映射；\n"
                + "9) 高度关注患者对上一轮提问的直接回答！如果患者回答'无'、'没有'、'否'，必须明确将其提取为上一轮提问所对应字段的值（如填入'无'或'否'），绝不能留空！\n\n"
                + "患者基本信息(可用作上下文，不可添加可识别敏感信息):\n"
                + "- 性别: " + safe(patient != null ? patient.getGender() : null) + "\n"
                + "- 年龄: " + safe(patient != null && patient.getAge() != null ? String.valueOf(patient.getAge()) : null) + "\n"
                + "- 模板: " + safe(template != null ? template.getTemplateName() : null) + "\n\n"
                + "字段定义:\n" + fieldSpec + "\n\n"
                + "当前已采集数据(JSON):\n" + JSON.toJSONString(currentAssessmentData == null ? Collections.emptyMap() : currentAssessmentData) + "\n\n"
                + "对话历史(JSON数组):\n" + history + "\n\n"
                + "上一轮助手提问:\n" + safe(lastAssistantQuestion) + "\n\n"
                + "本轮患者最新回答:\n" + safe(latestUserMessage);
    }

    private String buildFallbackCalculationPrompt(Patient patient, AssessmentTemplate template, Map<String, Object> assessmentData) {
        return "你是临床评估助手。当前模板规则不足，请基于评估数据给出结构化评估结果。\n"
                + "要求：严格输出 JSON，不要输出其他文本。\n"
                + "JSON schema:\n"
                + "{\n"
                + "  \"totalScore\": 0.0,\n"
                + "  \"assessmentResult\": \"string\",\n"
                + "  \"riskLevel\": \"低风险/中风险/高风险/重度 等\",\n"
                + "  \"riskTips\": \"string\",\n"
                + "  \"diagnosisName\": \"用于写入诊断库的简洁诊断名称\",\n"
                + "  \"abnormalDataTips\": [\"string\"]\n"
                + "}\n\n"
                + "约束:\n"
                + "1) riskTips 为临床可执行建议，避免夸大；\n"
                + "2) diagnosisName 15字以内，医学表达规范；\n"
                + "3) 若无法可靠估计 totalScore，给 0。\n\n"
                + "上下文:\n"
                + "- 模板名称: " + safe(template != null ? template.getTemplateName() : null) + "\n"
                + "- 患者性别: " + safe(patient != null ? patient.getGender() : null) + "\n"
                + "- 患者年龄: " + safe(patient != null && patient.getAge() != null ? String.valueOf(patient.getAge()) : null) + "\n"
                + "- 评估数据(JSON): " + JSON.toJSONString(assessmentData == null ? Collections.emptyMap() : assessmentData);
    }

    private String buildTemplateDraftPrompt(Patient patient, String symptomText, String departmentName) {
        return "你是临床评估模板设计助手，需要根据患者主诉自动生成一个可执行的评估模板草案。\n"
                + "必须严格输出 JSON，不要 markdown。\n"
                + "JSON schema:\n"
                + "{\n"
                + "  \"templateName\": \"string\",\n"
                + "  \"category\": \"string\",\n"
                + "  \"description\": \"string\",\n"
                + "  \"fields\": [\n"
                + "    {\n"
                + "      \"fieldCode\": \"snake_case\",\n"
                + "      \"fieldLabel\": \"string\",\n"
                + "      \"fieldType\": \"TEXT|TEXTAREA|NUMBER|DATE|SELECT|RADIO|CHECKBOX\",\n"
                + "      \"required\": 0,\n"
                + "      \"options\": [\"选项1\", \"选项2\"]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "约束:\n"
                + "1) fields 至少 6 个，最多 20 个；\n"
                + "2) 至少包含 age、gender、chief_complaint、duration、severity 相关字段；\n"
                + "3) SELECT/RADIO/CHECKBOX 必须提供 options；其他类型 options 为空数组；\n"
                + "4) required 只能是 0 或 1；\n"
                + "5) fieldCode 仅小写字母/数字/下划线；\n"
                + "6) 语言使用中文医学语义，适合临床初筛场景。\n\n"
                + "上下文:\n"
                + "- 科室: " + safe(departmentName) + "\n"
                + "- 患者性别: " + safe(patient != null ? patient.getGender() : null) + "\n"
                + "- 患者年龄: " + safe(patient != null && patient.getAge() != null ? String.valueOf(patient.getAge()) : null) + "\n"
                + "- 主诉: " + safe(symptomText);
    }

    private JSONObject normalizeTemplateDraft(JSONObject raw, String symptomText) {
        JSONObject result = new JSONObject();
        String templateName = safe(raw != null ? raw.getString("templateName") : null);
        if (templateName.isEmpty()) {
            templateName = "AI自动评估模板-" + (symptomText == null ? "通用" : symptomText.substring(0, Math.min(8, symptomText.length())));
        }
        if (templateName.length() > 50) {
            templateName = templateName.substring(0, 50);
        }
        result.put("templateName", templateName);

        String category = safe(raw != null ? raw.getString("category") : null);
        result.put("category", category.isEmpty() ? "AI自动生成" : category);

        String description = safe(raw != null ? raw.getString("description") : null);
        if (description.isEmpty()) {
            description = "由AI根据主诉自动生成，用于对话式动态评估";
        }
        result.put("description", description);

        JSONArray rawFields = raw == null ? null : raw.getJSONArray("fields");
        JSONArray fields = new JSONArray();
        Set<String> usedCodes = new HashSet<>();

        if (rawFields != null) {
            for (int i = 0; i < rawFields.size(); i++) {
                JSONObject f = rawFields.getJSONObject(i);
                JSONObject normalized = normalizeField(f, usedCodes);
                if (normalized != null) {
                    fields.add(normalized);
                }
            }
        }

        ensureBaseField(fields, usedCodes, "age", "年龄", "NUMBER", 1, Collections.emptyList());
        ensureBaseField(fields, usedCodes, "gender", "性别", "RADIO", 1, Arrays.asList("男", "女", "其他"));
        ensureBaseField(fields, usedCodes, "chief_complaint", "主要症状", "TEXTAREA", 1, Collections.emptyList());
        ensureBaseField(fields, usedCodes, "duration", "症状持续时间", "TEXT", 1, Collections.emptyList());
        ensureBaseField(fields, usedCodes, "severity", "症状严重程度", "RADIO", 1, Arrays.asList("轻度", "中度", "重度"));

        // 移除强制补充无意义字段的逻辑，如果AI生成得少，就只用基础字段
        if (fields.size() > 20) {
            JSONArray truncated = new JSONArray();
            for (int i = 0; i < 20; i++) {
                truncated.add(fields.getJSONObject(i));
            }
            fields = truncated;
        }

        result.put("fields", fields);
        return result;
    }

    private JSONObject normalizeField(JSONObject field, Set<String> usedCodes) {
        if (field == null) {
            return null;
        }
        String fieldCode = safe(field.getString("fieldCode")).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (fieldCode.isEmpty()) {
            fieldCode = "field_" + (usedCodes.size() + 1);
        }
        if (usedCodes.contains(fieldCode)) {
            return null;
        }

        String fieldLabel = safe(field.getString("fieldLabel"));
        if (fieldLabel.isEmpty()) {
            fieldLabel = fieldCode;
        }
        String fieldType = safe(field.getString("fieldType")).toUpperCase(Locale.ROOT);
        Set<String> allowTypes = new HashSet<>(Arrays.asList("TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "RADIO", "CHECKBOX"));
        if (!allowTypes.contains(fieldType)) {
            fieldType = "TEXT";
        }

        int required = field.getIntValue("required");
        required = required == 1 ? 1 : 0;

        List<String> options = new ArrayList<>();
        if ("SELECT".equals(fieldType) || "RADIO".equals(fieldType) || "CHECKBOX".equals(fieldType)) {
            JSONArray optionArr = field.getJSONArray("options");
            if (optionArr != null) {
                for (int i = 0; i < optionArr.size(); i++) {
                    String opt = safe(optionArr.getString(i));
                    if (!opt.isEmpty()) {
                        options.add(opt);
                    }
                }
            }
            if (options.isEmpty()) {
                options.add("是");
                options.add("否");
            }
        }

        usedCodes.add(fieldCode);
        JSONObject normalized = new JSONObject();
        normalized.put("fieldCode", fieldCode);
        normalized.put("fieldLabel", fieldLabel);
        normalized.put("fieldType", fieldType);
        normalized.put("required", required);
        normalized.put("options", options);
        return normalized;
    }

    private void ensureBaseField(JSONArray fields, Set<String> usedCodes, String fieldCode, String fieldLabel,
                                 String fieldType, int required, List<String> options) {
        if (usedCodes.contains(fieldCode)) {
            return;
        }
        JSONObject f = new JSONObject();
        f.put("fieldCode", fieldCode);
        f.put("fieldLabel", fieldLabel);
        f.put("fieldType", fieldType);
        f.put("required", required);
        f.put("options", options == null ? Collections.emptyList() : options);
        fields.add(f);
        usedCodes.add(fieldCode);
    }

    private JSONObject parseJsonSafely(String raw) {
        if (raw == null) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ignored) {
        }
        int start = raw.indexOf("{");
        int end = raw.lastIndexOf("}");
        if (start >= 0 && end > start) {
            String candidate = raw.substring(start, end + 1);
            try {
                return JSON.parseObject(candidate);
            } catch (Exception ignored) {
            }
        }
        return new JSONObject();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

