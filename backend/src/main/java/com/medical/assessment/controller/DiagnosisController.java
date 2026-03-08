package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.DiagnosisCreateDTO;
import com.medical.assessment.dto.DiagnosisUpdateDTO;
import com.medical.assessment.entity.Diagnosis;
import com.medical.assessment.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/diagnosis")
public class DiagnosisController {

    @Autowired
    private DiagnosisService diagnosisService;

    /**
     * 按科室获取诊断字典，用于医生确认患者当前诊断
     */
    @GetMapping("/by-department/{departmentId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Diagnosis>> listByDepartment(@PathVariable Long departmentId) {
        return Result.success(diagnosisService.listByDepartmentId(departmentId));
    }

    /**
     * 分页列表（管理用）
     */
    @GetMapping("/page")
    @RequiresRoles({"ADMIN"})
    public Result<PageResult<Diagnosis>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId) {
        Page<Diagnosis> page = new Page<>(current, size);
        LambdaQueryWrapper<Diagnosis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Diagnosis::getDeleted, 0);
        if (name != null && !name.isEmpty()) {
            wrapper.like(Diagnosis::getName, name);
        }
        if (departmentId != null) {
            wrapper.eq(Diagnosis::getDepartmentId, departmentId);
        }
        wrapper.orderByAsc(Diagnosis::getSortOrder);
        Page<Diagnosis> result = diagnosisService.page(page, wrapper);
        PageResult<Diagnosis> pageResult = new PageResult<>(result.getTotal(), result.getRecords());
        return Result.success(pageResult);
    }

    @PostMapping("/add")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DIAGNOSIS", action = "CREATE", targetType = "DIAGNOSIS", targetId = "#createDTO.name", description = "'新增诊断：' + #createDTO.name")
    public Result<Diagnosis> add(@Valid @RequestBody DiagnosisCreateDTO createDTO) {
        return Result.success(diagnosisService.createDiagnosis(createDTO));
    }

    @PutMapping("/update")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DIAGNOSIS", action = "UPDATE", targetType = "DIAGNOSIS", targetId = "#updateDTO.id", description = "'更新诊断：' + #updateDTO.name")
    public Result<Void> update(@Valid @RequestBody DiagnosisUpdateDTO updateDTO) {
        diagnosisService.updateDiagnosis(updateDTO);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DIAGNOSIS", action = "DELETE", targetType = "DIAGNOSIS", targetId = "#id", description = "'删除诊断ID：' + #id")
    public Result<Void> delete(@PathVariable Long id) {
        diagnosisService.removeById(id);
        return Result.success("删除成功");
    }
}
