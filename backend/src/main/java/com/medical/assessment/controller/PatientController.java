package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.DiagnosisAdoptResultVO;
import com.medical.assessment.dto.DiagnosisCreateDTO;
import com.medical.assessment.dto.PatientCreateDTO;
import com.medical.assessment.dto.PatientDiagnosisUpdateDTO;
import com.medical.assessment.dto.PatientDetailVO;
import com.medical.assessment.dto.PatientUpdateDTO;
import com.medical.assessment.dto.PatientVO;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.Diagnosis;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.DiagnosisService;
import com.medical.assessment.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/patient")
public class PatientController {
    
    @Autowired
    private PatientService patientService;
    @Autowired
    private AssessmentRecordService assessmentRecordService;
    @Autowired
    private DiagnosisService diagnosisService;
    
    @GetMapping("/list")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<PageResult<PatientVO>> getPatientList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String patientNo,
            @RequestParam(required = false) Long departmentId) {
        Page<Patient> page = new Page<>(current, size);
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patient::getDeleted, 0);
        if (name != null && !name.isEmpty()) {
            wrapper.like(Patient::getName, name);
        }
        if (patientNo != null && !patientNo.isEmpty()) {
            wrapper.eq(Patient::getPatientNo, patientNo);
        }
        if (departmentId != null) {
            wrapper.eq(Patient::getDepartmentId, departmentId);
        }
        wrapper.orderByDesc(Patient::getCreateTime);
        
        Page<Patient> result = patientService.page(page, wrapper);
        List<PatientVO> voList = patientService.toPatientVOList(result.getRecords());
        PageResult<PatientVO> pageResult = new PageResult<>(result.getTotal(), voList);
        return Result.success(pageResult);
    }
    
    /**
     * 获取患者详情（脱敏），用于列表、诊断详情等查看场景
     */
    @GetMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<PatientVO> getPatient(@PathVariable Long id) {
        return Result.success(patientService.getPatientVO(id));
    }
    
    /**
     * 获取患者完整信息（不脱敏），仅用于编辑表单加载
     * 需有编辑权限，返回明文以便用户修改
     */
    @GetMapping("/{id}/edit")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<PatientDetailVO> getPatientForEdit(@PathVariable Long id) {
        return Result.success(patientService.getPatientDetail(id));
    }
    
    @PostMapping("/add")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "PATIENT",
            action = "CREATE",
            targetType = "PATIENT",
            targetId = "#createDTO.name",
            description = "'新增患者：' + #createDTO.name"
    )
    public Result<Void> addPatient(@Valid @RequestBody PatientCreateDTO createDTO) {
        patientService.createPatient(createDTO);
        return Result.success("添加成功");
    }
    
    @PutMapping("/update")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "PATIENT",
            action = "UPDATE",
            targetType = "PATIENT",
            targetId = "#updateDTO.id",
            description = "'更新患者：' + #updateDTO.name"
    )
    public Result<Void> updatePatient(@Valid @RequestBody PatientUpdateDTO updateDTO) {
        patientService.updatePatient(updateDTO);
        return Result.success("更新成功");
    }

    @PutMapping("/{id}/diagnosis")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @OperationLogRecord(
            module = "PATIENT",
            action = "UPDATE_DIAGNOSIS",
            targetType = "PATIENT",
            targetId = "#id",
            description = "'确认/修改患者诊断'"
    )
    public Result<Void> updatePatientDiagnosis(@PathVariable Long id, @RequestBody PatientDiagnosisUpdateDTO updateDTO) {
        patientService.updatePatientDiagnosis(id, updateDTO == null ? null : updateDTO.getDiagnosisId());
        return Result.success("诊断更新成功");
    }

    @PostMapping("/{id}/diagnosis/adopt-ai")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    @Transactional
    @OperationLogRecord(
            module = "PATIENT",
            action = "ADOPT_AI_DIAGNOSIS",
            targetType = "PATIENT",
            targetId = "#id",
            description = "'采用AI建议诊断并同步到诊断字典'"
    )
    public Result<DiagnosisAdoptResultVO> adoptLatestAiDiagnosis(@PathVariable Long id, HttpServletRequest request) {
        try {
            Patient patient = patientService.getById(id);
            if (patient == null) {
                throw new BusinessException("患者不存在");
            }
            if (patient.getDepartmentId() == null) {
                throw new BusinessException("患者尚未关联科室，无法加入诊断字典");
            }

            String role = request.getAttribute("role") != null ? request.getAttribute("role").toString() : "";
            Object requestDepartmentId = request.getAttribute("departmentId");
            if ("DOCTOR".equals(role) && requestDepartmentId != null
                    && !patient.getDepartmentId().equals(Long.valueOf(requestDepartmentId.toString()))) {
                throw new BusinessException("您只能维护本科室患者的诊断");
            }

            AssessmentRecord latestRecord = assessmentRecordService.getLatestCompletedRecord(id);
            if (latestRecord == null) {
                throw new BusinessException("患者暂无已完成评估，无法采用 AI 诊断");
            }

            String aiDiagnosisName = latestRecord.getAiDiagnosisName();
            if (aiDiagnosisName == null || aiDiagnosisName.trim().isEmpty()) {
                throw new BusinessException("最近一次评估暂无 AI 建议诊断");
            }
            aiDiagnosisName = aiDiagnosisName.trim();

            Diagnosis diagnosis = patientService.autoApplyDiagnosisByName(id, aiDiagnosisName);
            boolean created = false;
            if (diagnosis == null) {
                diagnosis = diagnosisService.getOne(new LambdaQueryWrapper<Diagnosis>()
                        .eq(Diagnosis::getDepartmentId, patient.getDepartmentId())
                        .eq(Diagnosis::getName, aiDiagnosisName)
                        .eq(Diagnosis::getDeleted, 0), false);
            }
            if (diagnosis == null) {
                DiagnosisCreateDTO createDTO = new DiagnosisCreateDTO();
                createDTO.setName(aiDiagnosisName);
                createDTO.setDepartmentId(patient.getDepartmentId());
                createDTO.setSortOrder(0);
                createDTO.setRemark("由患者最新 AI 建议诊断一键加入");
                diagnosis = diagnosisService.createDiagnosis(createDTO);
                if (diagnosis == null || diagnosis.getId() == null) {
                    throw new BusinessException("创建诊断字典失败");
                }
                created = true;
            }

            patientService.updatePatientDiagnosis(id, diagnosis.getId());
            DiagnosisAdoptResultVO data = DiagnosisAdoptResultVO.of(diagnosis, aiDiagnosisName, created);
            String message = created ? "AI 诊断已加入字典并设为当前诊断" : "已采用现有诊断字典项并设为当前诊断";
            return Result.success(message, data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("加入诊断字典失败：" + (e.getMessage() == null ? "未知异常" : e.getMessage()));
        }
    }
    
    @DeleteMapping("/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "PATIENT",
            action = "DELETE",
            targetType = "PATIENT",
            targetId = "#id",
            description = "'删除患者'"
    )
    public Result<Void> deletePatient(@PathVariable Long id) {
        patientService.removeById(id);
        return Result.success("删除成功");
    }
}

