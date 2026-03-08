package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.PatientCreateDTO;
import com.medical.assessment.dto.PatientDiagnosisUpdateDTO;
import com.medical.assessment.dto.PatientDetailVO;
import com.medical.assessment.dto.PatientUpdateDTO;
import com.medical.assessment.dto.PatientVO;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/patient")
public class PatientController {
    
    @Autowired
    private PatientService patientService;
    
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

