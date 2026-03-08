package com.medical.assessment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.TreatmentSuggestion;
import com.medical.assessment.service.TreatmentSuggestionRecordService;
import com.medical.assessment.service.TreatmentSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 诊疗建议控制器
 */
@RestController
@RequestMapping("/treatment-suggestion")
public class TreatmentSuggestionController {
    
    @Autowired
    private TreatmentSuggestionService treatmentSuggestionService;
    
    @Autowired
    private TreatmentSuggestionRecordService suggestionRecordService;
    
    /**
     * 生成诊疗建议
     */
    @PostMapping("/generate/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "TREATMENT_SUGGESTION",
            action = "GENERATE",
            targetType = "TREATMENT_SUGGESTION",
            targetId = "#retData?.id",
            description = "'生成诊疗建议 recordId=' + #recordId"
    )
    public Result<TreatmentSuggestion> generateTreatmentSuggestion(@PathVariable Long recordId,
                                                                  HttpServletRequest request) {
        try {
            Long generatorId = (Long) request.getAttribute("userId");
            String generatorName = (String) request.getAttribute("username");
            
            TreatmentSuggestion suggestion = treatmentSuggestionService.generateTreatmentSuggestion(
                recordId, generatorId, generatorName);
            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.error("生成诊疗建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新生成诊疗建议
     */
    @PostMapping("/regenerate/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "TREATMENT_SUGGESTION",
            action = "REGENERATE",
            targetType = "TREATMENT_SUGGESTION",
            targetId = "#retData?.id",
            description = "'重新生成诊疗建议 recordId=' + #recordId"
    )
    public Result<TreatmentSuggestion> regenerateTreatmentSuggestion(@PathVariable Long recordId,
                                                                    HttpServletRequest request) {
        try {
            Long generatorId = (Long) request.getAttribute("userId");
            String generatorName = (String) request.getAttribute("username");
            
            TreatmentSuggestion suggestion = treatmentSuggestionService.generateTreatmentSuggestion(
                recordId, generatorId, generatorName);
            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.error("重新生成诊疗建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取诊疗建议
     */
    @GetMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<TreatmentSuggestion> getSuggestion(@PathVariable Long id) {
        try {
            TreatmentSuggestion suggestion = suggestionRecordService.getById(id);
            if (suggestion == null) {
                return Result.error("诊疗建议不存在");
            }
            return Result.success(suggestion);
        } catch (Exception e) {
            return Result.error("获取诊疗建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据患者ID获取诊疗建议列表
     */
    @GetMapping("/patient/{patientId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<TreatmentSuggestion>> getSuggestionsByPatientId(@PathVariable Long patientId) {
        try {
            List<TreatmentSuggestion> suggestions = suggestionRecordService.getByPatientId(patientId);
            return Result.success(suggestions);
        } catch (Exception e) {
            return Result.error("获取诊疗建议列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据评估记录ID获取诊疗建议列表
     */
    @GetMapping("/record/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<TreatmentSuggestion>> getSuggestionsByRecordId(@PathVariable Long recordId) {
        try {
            List<TreatmentSuggestion> suggestions = suggestionRecordService.getByAssessmentRecordId(recordId);
            return Result.success(suggestions);
        } catch (Exception e) {
            return Result.error("获取诊疗建议列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询诊疗建议
     */
    @GetMapping("/page")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<PageResult<TreatmentSuggestion>> getSuggestionPage(
            @RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Page<TreatmentSuggestion> page = suggestionRecordService.getPage(patientId, current, size);
            return Result.success(PageResult.of(page));
        } catch (Exception e) {
            return Result.error("分页查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除诊疗建议
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "TREATMENT_SUGGESTION",
            action = "DELETE",
            targetType = "TREATMENT_SUGGESTION",
            targetId = "#id",
            description = "'删除诊疗建议ID：' + #id"
    )
    public Result<Void> deleteSuggestion(@PathVariable Long id) {
        try {
            boolean success = suggestionRecordService.deleteById(id);
            if (success) {
                return Result.success(null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
