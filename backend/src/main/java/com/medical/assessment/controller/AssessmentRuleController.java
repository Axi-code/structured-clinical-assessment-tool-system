package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.AssessmentRule;
import com.medical.assessment.service.AssessmentRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assessment-rule")
public class AssessmentRuleController {
    
    @Autowired
    private AssessmentRuleService ruleService;
    
    /**
     * 获取规则列表（分页）
     */
    @GetMapping("/list")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    public Result<PageResult<AssessmentRule>> getRuleList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer status) {
        Page<AssessmentRule> page = new Page<>(current, size);
        LambdaQueryWrapper<AssessmentRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRule::getDeleted, 0);
        if (templateId != null) {
            wrapper.eq(AssessmentRule::getTemplateId, templateId);
        }
        if (ruleName != null && !ruleName.isEmpty()) {
            wrapper.like(AssessmentRule::getRuleName, ruleName);
        }
        if (ruleType != null && !ruleType.isEmpty()) {
            wrapper.eq(AssessmentRule::getRuleType, ruleType);
        }
        if (status != null) {
            wrapper.eq(AssessmentRule::getStatus, status);
        }
        wrapper.orderByAsc(AssessmentRule::getPriority);
        wrapper.orderByDesc(AssessmentRule::getCreateTime);
        
        Page<AssessmentRule> result = ruleService.page(page, wrapper);
        return Result.success(PageResult.of(result));
    }
    
    /**
     * 根据模板ID获取规则列表
     */
    @GetMapping("/template/{templateId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<AssessmentRule>> getRulesByTemplateId(@PathVariable Long templateId) {
        List<AssessmentRule> rules = ruleService.getRulesByTemplateId(templateId);
        return Result.success(rules);
    }
    
    /**
     * 获取规则详情
     */
    @GetMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    public Result<AssessmentRule> getRuleById(@PathVariable Long id) {
        AssessmentRule rule = ruleService.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }
    
    /**
     * 创建规则
     */
    @PostMapping("/create")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RULE",
            action = "CREATE",
            targetType = "ASSESSMENT_RULE",
            targetId = "#rule.id",
            description = "'创建规则：' + #rule.ruleName"
    )
    public Result<AssessmentRule> createRule(@RequestBody AssessmentRule rule) {
        if (rule.getTemplateId() == null) {
            return Result.error("模板ID不能为空");
        }
        if (rule.getRuleName() == null || rule.getRuleName().isEmpty()) {
            return Result.error("规则名称不能为空");
        }
        if (rule.getRuleType() == null || rule.getRuleType().isEmpty()) {
            return Result.error("规则类型不能为空");
        }
        
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(0);
        if (rule.getStatus() == null) {
            rule.setStatus(1); // 默认启用
        }
        if (rule.getPriority() == null) {
            rule.setPriority(0);
        }
        
        ruleService.save(rule);
        return Result.success(rule);
    }
    
    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RULE",
            action = "UPDATE",
            targetType = "ASSESSMENT_RULE",
            targetId = "#id",
            description = "'更新规则ID：' + #id"
    )
    public Result<AssessmentRule> updateRule(@PathVariable Long id, @RequestBody AssessmentRule rule) {
        AssessmentRule existingRule = ruleService.getById(id);
        if (existingRule == null || existingRule.getDeleted() == 1) {
            return Result.error("规则不存在");
        }
        
        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());
        ruleService.updateById(rule);
        
        AssessmentRule updatedRule = ruleService.getById(id);
        return Result.success(updatedRule);
    }
    
    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_RULE",
            action = "DELETE",
            targetType = "ASSESSMENT_RULE",
            targetId = "#id",
            description = "'删除规则ID：' + #id"
    )
    public Result<Void> deleteRule(@PathVariable Long id) {
        AssessmentRule rule = ruleService.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            return Result.error("规则不存在");
        }
        // 使用 removeById 进行逻辑删除（MyBatis-Plus 会正确更新 deleted 字段）
        ruleService.removeById(id);
        return Result.success();
    }
    
    /**
     * 更新规则状态
     */
    @PutMapping("/{id}/status")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RULE",
            action = "STATUS",
            targetType = "ASSESSMENT_RULE",
            targetId = "#id",
            description = "'更新规则状态 ID=' + #id + ' status=' + #status"
    )
    public Result<Void> updateRuleStatus(@PathVariable Long id, @RequestParam Integer status) {
        AssessmentRule rule = ruleService.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            return Result.error("规则不存在");
        }
        
        rule.setStatus(status);
        rule.setUpdateTime(LocalDateTime.now());
        ruleService.updateById(rule);
        return Result.success();
    }
    
    /**
     * 测试规则表达式
     */
    @PostMapping("/test")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    public Result<Map<String, Object>> testRule(@RequestBody TestRuleRequest request) {
        try {
            Map<String, Object> testData = request.getTestData();
            if (testData == null) {
                testData = new java.util.HashMap<>();
            }
            
            Map<String, Object> result = ruleService.testExpression(
                request.getConditionExpression(),
                request.getResultExpression(),
                testData
            );
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("表达式测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 实时计算评估结果（不保存数据）
     */
    @PostMapping("/calculate-realtime")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> calculateRealtime(@RequestBody RealtimeCalculateRequest request) {
        try {
            if (request.getTemplateId() == null) {
                return Result.error("模板ID不能为空");
            }
            if (request.getAssessmentData() == null) {
                return Result.error("评估数据不能为空");
            }
            
            Map<String, Object> result = ruleService.calculateRealtime(
                request.getTemplateId(),
                request.getAssessmentData()
            );
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("实时计算失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试规则请求对象
     */
    public static class TestRuleRequest {
        private String conditionExpression;
        private String resultExpression;
        private java.util.Map<String, Object> testData;
        
        public String getConditionExpression() {
            return conditionExpression;
        }
        
        public void setConditionExpression(String conditionExpression) {
            this.conditionExpression = conditionExpression;
        }
        
        public String getResultExpression() {
            return resultExpression;
        }
        
        public void setResultExpression(String resultExpression) {
            this.resultExpression = resultExpression;
        }
        
        public java.util.Map<String, Object> getTestData() {
            return testData;
        }
        
        public void setTestData(java.util.Map<String, Object> testData) {
            this.testData = testData;
        }
    }
    
    /**
     * 实时计算请求对象
     */
    public static class RealtimeCalculateRequest {
        private Long templateId;
        private java.util.Map<String, Object> assessmentData;
        
        public Long getTemplateId() {
            return templateId;
        }
        
        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }
        
        public java.util.Map<String, Object> getAssessmentData() {
            return assessmentData;
        }
        
        public void setAssessmentData(java.util.Map<String, Object> assessmentData) {
            this.assessmentData = assessmentData;
        }
    }
}
