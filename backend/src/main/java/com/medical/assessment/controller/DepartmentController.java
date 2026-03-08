package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.DepartmentCreateDTO;
import com.medical.assessment.dto.DepartmentUpdateDTO;
import com.medical.assessment.entity.Department;
import com.medical.assessment.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取所有科室（下拉选择用，无需分页）
     */
    @GetMapping("/list-all")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Department>> listAll() {
        return Result.success(departmentService.listAll());
    }

    /**
     * 分页列表（管理用）
     */
    @GetMapping("/page")
    @RequiresRoles({"ADMIN"})
    public Result<PageResult<Department>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        Page<Department> page = new Page<>(current, size);
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeleted, 0);
        if (name != null && !name.isEmpty()) {
            wrapper.like(Department::getName, name);
        }
        wrapper.orderByAsc(Department::getSortOrder);
        Page<Department> result = departmentService.page(page, wrapper);
        PageResult<Department> pageResult = new PageResult<>(result.getTotal(), result.getRecords());
        return Result.success(pageResult);
    }

    @PostMapping("/add")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DEPARTMENT", action = "CREATE", targetType = "DEPARTMENT", targetId = "#createDTO.name", description = "'新增科室：' + #createDTO.name")
    public Result<Department> add(@Valid @RequestBody DepartmentCreateDTO createDTO) {
        return Result.success(departmentService.createDepartment(createDTO));
    }

    @PutMapping("/update")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DEPARTMENT", action = "UPDATE", targetType = "DEPARTMENT", targetId = "#updateDTO.id", description = "'更新科室：' + #updateDTO.name")
    public Result<Void> update(@Valid @RequestBody DepartmentUpdateDTO updateDTO) {
        departmentService.updateDepartment(updateDTO);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(module = "DEPARTMENT", action = "DELETE", targetType = "DEPARTMENT", targetId = "#id", description = "'删除科室ID：' + #id")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.removeById(id);
        return Result.success("删除成功");
    }
}
