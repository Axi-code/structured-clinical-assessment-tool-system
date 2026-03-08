package com.medical.assessment.controller;

import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.AssessmentDraftCreateDTO;
import com.medical.assessment.dto.AssessmentSaveDTO;
import com.medical.assessment.dto.AssessmentSubmitDTO;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.service.AssessmentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 评估记录相关接口
 * 覆盖文档中的：
 * - 临床评估数据采集（创建草稿、保存草稿、提交完成）
 * - 评估数据管理与历史记录（历史记录查询、记录对比）
 */
@RestController
@RequestMapping("/assessment-record")
public class AssessmentRecordController {

    @Autowired
    private AssessmentRecordService assessmentRecordService;

    /**
     * 创建评估记录草稿
     */
    @PostMapping("/draft")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RECORD",
            action = "CREATE",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#retData?.id",
            description = "'创建评估草稿 patientId=' + #createDTO.patientId + ' templateId=' + #createDTO.templateId"
    )
    public Result<AssessmentRecord> createDraft(@Valid @RequestBody AssessmentDraftCreateDTO createDTO, HttpServletRequest request) {
        Long assessorId = (Long) request.getAttribute("userId");
        String assessorName = (String) request.getAttribute("realName");
        if (assessorName == null) assessorName = (String) request.getAttribute("username");
        Long departmentId = request.getAttribute("departmentId") != null ? Long.valueOf(request.getAttribute("departmentId").toString()) : null;

        AssessmentRecord record = assessmentRecordService.createDraft(
                createDTO.getPatientId(),
                createDTO.getTemplateId(),
                assessorId,
                assessorName,
                departmentId
        );
        return Result.success(record);
    }

    /**
     * 保存评估数据（草稿或完成）
     * status: 0-草稿 1-完成
     */
    @PostMapping("/save")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RECORD",
            action = "SAVE",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#saveDTO.recordId",
            description = "'保存评估记录 recordId=' + #saveDTO.recordId + ' status=' + (#saveDTO.status==1 ? 'COMPLETE' : 'SAVE')"
    )
    public Result<AssessmentRecord> saveAssessment(@Valid @RequestBody AssessmentSaveDTO saveDTO) {
        AssessmentRecord record = assessmentRecordService.saveAssessmentData(
                saveDTO.getRecordId(),
                saveDTO.getAssessmentData(),
                saveDTO.getStatus() == null ? 0 : saveDTO.getStatus()
        );
        return Result.success(record);
    }

    /**
     * 提交评估（完成评估）
     */
    @PostMapping("/submit")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "ASSESSMENT_RECORD",
            action = "SUBMIT",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#submitDTO.recordId",
            description = "'提交评估记录 recordId=' + #submitDTO.recordId"
    )
    public Result<AssessmentRecord> submitAssessment(@Valid @RequestBody AssessmentSubmitDTO submitDTO) {
        AssessmentRecord record = assessmentRecordService.submitAssessment(
                submitDTO.getRecordId(),
                submitDTO.getAssessmentData()
        );
        return Result.success(record);
    }

    /**
     * 获取某个患者的历史评估记录（仅已完成）
     */
    @GetMapping("/history/{patientId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<AssessmentRecord>> getPatientHistory(@PathVariable Long patientId) {
        List<AssessmentRecord> records = assessmentRecordService.getPatientHistory(patientId);
        return Result.success(records);
    }

    /**
     * 评估记录对比
     */
    @PostMapping("/compare")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> compareRecords(@RequestBody List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BusinessException("recordIds 不能为空");
        }
        Map<String, Object> data = assessmentRecordService.compareRecords(recordIds);
        return Result.success(data);
    }
}


