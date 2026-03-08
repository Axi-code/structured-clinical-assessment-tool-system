package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.entity.AssessmentRule;
import com.medical.assessment.mapper.AssessmentRuleMapper;
import com.medical.assessment.service.AssessmentRuleService;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AssessmentRuleServiceImpl extends ServiceImpl<AssessmentRuleMapper, AssessmentRule> implements AssessmentRuleService {
    
    private final ScriptEngine scriptEngine;
    
    public AssessmentRuleServiceImpl() {
        this.scriptEngine = initScriptEngine();
    }

    /**
     * 初始化脚本引擎，兼容 JDK 15+ 需显式引入 Nashorn 的情况
     */
    private ScriptEngine initScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        if (engine == null) {
            engine = manager.getEngineByName("JavaScript");
        }
        if (engine == null) {
            engine = manager.getEngineByName("js");
        }
        if (engine == null) {
            throw new IllegalStateException("未找到可用的 JavaScript ScriptEngine，请检查是否已引入 org.openjdk.nashorn:nashorn-core 依赖");
        }
        return engine;
    }
    
    @Override
    public List<AssessmentRule> getRulesByTemplateId(Long templateId) {
        LambdaQueryWrapper<AssessmentRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRule::getTemplateId, templateId);
        wrapper.eq(AssessmentRule::getStatus, 1); // 只查询启用的规则
        wrapper.eq(AssessmentRule::getDeleted, 0);
        wrapper.orderByAsc(AssessmentRule::getPriority);
        wrapper.orderByAsc(AssessmentRule::getCreateTime);
        return list(wrapper);
    }
    
    @Override
    public Double calculateScore(Long templateId, Map<String, Object> assessmentData) {
        List<AssessmentRule> rules = getRulesByTemplateId(templateId);
        double totalScore = 0.0;
        
        for (AssessmentRule rule : rules) {
            if (!"SCORE".equals(rule.getRuleType())) {
                continue;
            }
            
            // 为表达式准备上下文（补齐 score / totalScore 等变量，避免脚本中引用报错）
            Map<String, Object> ctx = new HashMap<>();
            if (assessmentData != null) {
                ctx.putAll(assessmentData);
            }
            ctx.putIfAbsent("score", 0);
            ctx.put("totalScore", totalScore);

            // 检查条件表达式
            if (rule.getConditionExpression() != null && !rule.getConditionExpression().isEmpty()) {
                if (!evaluateCondition(rule.getConditionExpression(), ctx)) {
                    continue;
                }
            }
            
            // 执行结果表达式
            if (rule.getResultExpression() != null && !rule.getResultExpression().isEmpty()) {
                try {
                    Object result = evaluateExpression(rule.getResultExpression(), ctx);
                    if (result instanceof Number) {
                        totalScore += ((Number) result).doubleValue();
                    }
                } catch (Exception e) {
                    // 忽略计算错误
                }
            } else if (rule.getRuleContent() != null && !rule.getRuleContent().isEmpty()) {
                // 从规则内容中计算分数
                Map<String, Object> ruleContent = JSON.parseObject(rule.getRuleContent(), Map.class);
                if (ruleContent.containsKey("score")) {
                    totalScore += Double.parseDouble(ruleContent.get("score").toString());
                }
            }
        }
        
        return totalScore;
    }
    
    @Override
    public Map<String, Object> calculateRisk(Long templateId, Map<String, Object> assessmentData) {
        List<AssessmentRule> rules = getRulesByTemplateId(templateId);
        Map<String, Object> riskResult = new HashMap<>();
        List<String> riskTips = new ArrayList<>();
        String riskLevel = "低风险"; // 默认低风险，匹配规则后会更新为对应等级
        
        for (AssessmentRule rule : rules) {
            if (!"RISK".equals(rule.getRuleType())) {
                continue;
            }

            // 风险规则也需要基础上下文（score/totalScore/riskLevel）
            Map<String, Object> ctx = new HashMap<>();
            if (assessmentData != null) {
                ctx.putAll(assessmentData);
            }
            ctx.putIfAbsent("score", 0);
            ctx.putIfAbsent("totalScore", 0);
            ctx.put("riskLevel", riskLevel);
            
            // 检查条件表达式
            if (rule.getConditionExpression() != null && !rule.getConditionExpression().isEmpty()) {
                if (!evaluateCondition(rule.getConditionExpression(), ctx)) {
                    continue;
                }
            }
            
            // 结果表达式：允许返回风险等级字符串，或设置 riskLevel 变量（evaluateExpression 会同步 riskLevel 回 ctx）
            String ruleRiskLevel = null;
            if (rule.getResultExpression() != null && !rule.getResultExpression().isEmpty()) {
                try {
                    Object exprResult = evaluateExpression(rule.getResultExpression(), ctx);
                    if (exprResult instanceof String && !((String) exprResult).isEmpty()) {
                        ruleRiskLevel = exprResult.toString();
                    } else if (ctx.get("riskLevel") != null && !ctx.get("riskLevel").toString().isEmpty()) {
                        ruleRiskLevel = ctx.get("riskLevel").toString();
                    }
                } catch (Exception ignored) {
                }
            }

            // 条件匹配时直接更新风险等级（规则按 priority 升序处理，后匹配的覆盖前者；PHQ-9 等量表条件互斥，仅一条匹配）
            String levelToUse = null;
            if (rule.getRuleContent() != null && !rule.getRuleContent().isEmpty()) {
                Map<String, Object> ruleContent = JSON.parseObject(rule.getRuleContent(), Map.class);
                if (ruleContent.containsKey("riskLevel")) {
                    levelToUse = ruleContent.get("riskLevel").toString();
                }
                if (ruleContent.containsKey("riskTip")) {
                    riskTips.add(ruleContent.get("riskTip").toString());
                }
            }
            if (levelToUse == null && ruleRiskLevel != null && !ruleRiskLevel.isEmpty()) {
                levelToUse = ruleRiskLevel;
            }
            if (levelToUse != null) {
                riskLevel = levelToUse;
            }
        }
        
        riskResult.put("riskLevel", riskLevel);
        riskResult.put("riskTips", String.join("; ", riskTips));
        return riskResult;
    }
    
    @Override
    public Map<String, Object> executeCalculation(Long templateId, Map<String, Object> assessmentData) {
        List<AssessmentRule> rules = getRulesByTemplateId(templateId);
        Map<String, Object> calculationResult = new HashMap<>();
        
        for (AssessmentRule rule : rules) {
            if (!"CALCULATE".equals(rule.getRuleType())) {
                continue;
            }
            
            // 检查条件表达式
            if (rule.getConditionExpression() != null && !rule.getConditionExpression().isEmpty()) {
                if (!evaluateCondition(rule.getConditionExpression(), assessmentData)) {
                    continue;
                }
            }
            
            // 执行结果表达式
            if (rule.getResultExpression() != null && !rule.getResultExpression().isEmpty()) {
                try {
                    Object result = evaluateExpression(rule.getResultExpression(), assessmentData);
                    calculationResult.put(rule.getRuleCode(), result);
                } catch (Exception e) {
                    // 忽略计算错误
                }
            }
        }
        
        return calculationResult;
    }
    
    @Override
    public Map<String, Object> executeAllRules(Long templateId, Map<String, Object> assessmentData) {
        Map<String, Object> result = new HashMap<>();
        
        // 检测异常数据
        List<String> abnormalDataTips = detectAbnormalData(templateId, assessmentData);
        if (!abnormalDataTips.isEmpty()) {
            result.put("abnormalDataTips", abnormalDataTips);
        }
        
        // 计算总分
        Double totalScore = calculateScore(templateId, assessmentData);
        result.put("totalScore", totalScore);

        // 将 totalScore 放入上下文，供 RISK / CALCULATE 规则引用
        Map<String, Object> ctx = new HashMap<>();
        if (assessmentData != null) {
            ctx.putAll(assessmentData);
        }
        ctx.put("totalScore", totalScore);
        ctx.putIfAbsent("score", 0);
        
        // 计算风险（风险等级由 totalScore 决定，RISK 规则根据总分区间匹配）
        Map<String, Object> riskResult = calculateRisk(templateId, ctx);
        result.putAll(riskResult);
        
        // 执行计算规则
        Map<String, Object> calculationResult = executeCalculation(templateId, ctx);
        result.putAll(calculationResult);
        
        // 评估结果：有风险等级时优先使用风险等级作为评估结果，否则按总分判定
        if (result.containsKey("riskLevel") && result.get("riskLevel") != null) {
            String riskLevel = result.get("riskLevel").toString();
            if (!riskLevel.isEmpty()) {
                result.put("assessmentResult", riskLevel);
            } else {
                result.put("assessmentResult", determineAssessmentResult(templateId, totalScore));
            }
        } else if (totalScore != null) {
            result.put("assessmentResult", determineAssessmentResult(templateId, totalScore));
        }
        
        // 合并异常数据提示到风险提示中
        if (!abnormalDataTips.isEmpty()) {
            String existingRiskTips = (String) result.get("riskTips");
            if (existingRiskTips == null || existingRiskTips.isEmpty()) {
                result.put("riskTips", String.join("; ", abnormalDataTips));
            } else {
                result.put("riskTips", existingRiskTips + "; " + String.join("; ", abnormalDataTips));
            }
        }
        
        return result;
    }
    
    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String conditionExpression, Map<String, Object> assessmentData) {
        try {
            String expression = replaceVariables(conditionExpression, assessmentData);
            Object result = scriptEngine.eval(expression, buildBindings(assessmentData));
            return result instanceof Boolean && (Boolean) result;
        } catch (ScriptException e) {
            return false;
        }
    }
    
    /**
     * 评估表达式
     * 执行后会将 bindings 中的 riskLevel 同步回 assessmentData，确保 riskLevel = "xxx" 类赋值能被正确获取
     */
    private Object evaluateExpression(String expression, Map<String, Object> assessmentData) throws ScriptException {
        String replacedExpression = replaceVariables(expression, assessmentData);
        javax.script.Bindings bindings = buildBindings(assessmentData);
        Object result = scriptEngine.eval(replacedExpression, bindings);
        // 同步 riskLevel 回 ctx，避免部分引擎对赋值表达式返回值不一致时无法获取
        if (assessmentData != null && bindings.containsKey("riskLevel")) {
            Object updated = bindings.get("riskLevel");
            if (updated != null) {
                assessmentData.put("riskLevel", updated);
            }
        }
        return result;
    }
    
    /**
     * 替换表达式中的变量
     */
    private String replaceVariables(String expression, Map<String, Object> assessmentData) {
        String result = expression;
        for (Map.Entry<String, Object> entry : assessmentData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueStr;
            if (value instanceof String) {
                valueStr = "'" + value + "'";
            } else {
                valueStr = value != null ? value.toString() : "0";
            }
            result = result.replace("${" + key + "}", valueStr);
            result = result.replace("$" + key, valueStr);
        }

        // 未提供的变量默认替换为 0，避免 ReferenceError
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}|\\$([A-Za-z0-9_]+)");
        Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // 未替换到值的变量用 0 兜底
            matcher.appendReplacement(sb, "0");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 为脚本引擎构造绑定，支持直接使用变量名（如 score）而非占位符
     */
    private javax.script.Bindings buildBindings(Map<String, Object> assessmentData) {
        javax.script.SimpleBindings bindings = new javax.script.SimpleBindings();
        if (assessmentData != null) {
            for (Map.Entry<String, Object> entry : assessmentData.entrySet()) {
                Object value = entry.getValue();
                bindings.put(entry.getKey(), value == null ? 0 : value);
            }
        }
        // 常用变量兜底，避免表达式里引用时报 ReferenceError
        if (!bindings.containsKey("score")) {
            bindings.put("score", 0);
        }
        if (!bindings.containsKey("totalScore")) {
            bindings.put("totalScore", 0);
        }
        if (!bindings.containsKey("riskLevel")) {
            bindings.put("riskLevel", "");
        }
        return bindings;
    }
    
    /**
     * 获取风险等级优先级（数值越大风险越高）
     * 支持英文、中文风险等级，以及各类量表的严重程度（如PHQ-9抑郁程度、认知障碍等）
     * 注意：匹配到任意规则时都应覆盖默认值，故最低有效等级（无/正常/低风险）也需 >= 1
     */
    private int getRiskPriority(String riskLevel) {
        if (riskLevel == null || riskLevel.trim().isEmpty()) {
            return 0;
        }
        String v = riskLevel.trim();
        // 中文风险等级（数值越大表示风险/严重程度越高）
        switch (v) {
            case "极高风险":
                return 5;
            case "高风险":
            case "高危":
            case "重度":
            case "中重度障碍":
                return 4;
            case "中风险":
            case "中度风险":
            case "中危":
            case "中度":
                return 3;
            case "低风险":
            case "轻度风险":
            case "低危":
            case "轻度":
            case "轻度障碍":
            case "无":
            case "正常":
                return 2;
            default:
                break;
        }
        // 英文
        switch (v.toUpperCase()) {
            case "CRITICAL":
                return 5;
            case "HIGH":
                return 4;
            case "MEDIUM":
                return 3;
            case "LOW":
                return 2;
            default:
                return 0;
        }
    }
    
    /**
     * 根据总分确定评估结果
     */
    private String determineAssessmentResult(Long templateId, Double totalScore) {
        // 这里可以根据模板配置的评分标准来确定结果
        // 暂时使用简单的逻辑
        if (totalScore >= 80) {
            return "优秀";
        } else if (totalScore >= 60) {
            return "良好";
        } else if (totalScore >= 40) {
            return "一般";
        } else {
            return "较差";
        }
    }
    
    /**
     * 检测异常数据
     * 包括：数值范围异常、逻辑一致性检查、必填项缺失等
     */
    private List<String> detectAbnormalData(Long templateId, Map<String, Object> assessmentData) {
        List<String> tips = new ArrayList<>();
        
        // 获取模板的所有规则，查找异常检测规则
        List<AssessmentRule> rules = getRulesByTemplateId(templateId);
        
        for (AssessmentRule rule : rules) {
            if (!"ABNORMAL".equals(rule.getRuleType())) {
                continue;
            }
            
            // 检查条件表达式
            if (rule.getConditionExpression() != null && !rule.getConditionExpression().isEmpty()) {
                if (evaluateCondition(rule.getConditionExpression(), assessmentData)) {
                    // 条件满足，说明检测到异常
                    if (rule.getRuleContent() != null && !rule.getRuleContent().isEmpty()) {
                        Map<String, Object> ruleContent = JSON.parseObject(rule.getRuleContent(), Map.class);
                        if (ruleContent.containsKey("tip")) {
                            tips.add(ruleContent.get("tip").toString());
                        } else if (rule.getRemark() != null && !rule.getRemark().isEmpty()) {
                            tips.add(rule.getRemark());
                        } else {
                            tips.add(rule.getRuleName() + "：检测到异常数据");
                        }
                    } else if (rule.getRemark() != null && !rule.getRemark().isEmpty()) {
                        tips.add(rule.getRemark());
                    } else {
                        tips.add(rule.getRuleName() + "：检测到异常数据");
                    }
                }
            }
        }
        
        // 通用异常检测：数值范围检查
        for (Map.Entry<String, Object> entry : assessmentData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // 检查数值是否在合理范围内
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                
                // 检查是否为负数（某些指标不应该为负）
                if (numValue < 0 && !key.toLowerCase().contains("difference") && 
                    !key.toLowerCase().contains("change")) {
                    // 某些字段允许负数，这里可以根据实际需求调整
                    // tips.add(key + "的值不应为负数");
                }
                
                // 检查是否过大（可能是输入错误）
                if (numValue > 10000) {
                    tips.add(key + "的值异常大（" + numValue + "），请确认是否正确");
                }
            }
        }
        
        // 逻辑一致性检查：检查相关字段的逻辑关系
        // 例如：如果A > B，但B的值应该大于A，则提示异常
        // 这里可以根据具体业务需求添加更多逻辑检查
        
        return tips;
    }
    
    @Override
    public Map<String, Object> testExpression(String conditionExpression, String resultExpression, Map<String, Object> testData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试条件表达式
            if (conditionExpression != null && !conditionExpression.isEmpty()) {
                boolean conditionResult = evaluateCondition(conditionExpression, testData != null ? testData : new HashMap<>());
                result.put("conditionResult", conditionResult);
                result.put("conditionValid", true);
            } else {
                result.put("conditionValid", true);
                result.put("conditionResult", true);
            }
            
            // 测试结果表达式
            if (resultExpression != null && !resultExpression.isEmpty()) {
                Object expressionResult = evaluateExpression(resultExpression, testData != null ? testData : new HashMap<>());
                result.put("expressionResult", expressionResult);
                result.put("expressionValid", true);
            } else {
                result.put("expressionValid", true);
            }
            
            result.put("success", true);
            result.put("message", "表达式测试通过");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "表达式测试失败: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> calculateRealtime(Long templateId, Map<String, Object> assessmentData) {
        // 实时计算，不保存数据
        return executeAllRules(templateId, assessmentData);
    }
}

