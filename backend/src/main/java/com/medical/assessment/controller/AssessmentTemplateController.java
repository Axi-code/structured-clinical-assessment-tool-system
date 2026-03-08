package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.AssessmentTemplateCreateDTO;
import com.medical.assessment.dto.AssessmentTemplateUpdateDTO;
import com.medical.assessment.dto.AssessmentTemplateVersionCreateDTO;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.TemplateDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/assessment-template")
public class AssessmentTemplateController {
    
    @Autowired
    private AssessmentTemplateService templateService;
    
    @Autowired
    private AssessmentFieldService fieldService;
    @Autowired
    private TemplateDepartmentService templateDepartmentService;
    
    @GetMapping("/list")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<PageResult<AssessmentTemplate>> getTemplateList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long departmentId) {
        Page<AssessmentTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<AssessmentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentTemplate::getDeleted, 0);
        if (departmentId != null) {
            List<Long> templateIds = templateDepartmentService.getTemplateIdsByDepartmentId(departmentId);
            if (templateIds.isEmpty()) {
                return Result.success(new PageResult<>(0L, java.util.Collections.<AssessmentTemplate>emptyList()));
            }
            wrapper.in(AssessmentTemplate::getId, templateIds);
        }
        if (templateName != null && !templateName.isEmpty()) {
            wrapper.like(AssessmentTemplate::getTemplateName, templateName);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AssessmentTemplate::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(AssessmentTemplate::getStatus, status);
        }
        wrapper.orderByDesc(AssessmentTemplate::getCreateTime);
        
        Page<AssessmentTemplate> result = templateService.page(page, wrapper);
        PageResult<AssessmentTemplate> pageResult = new PageResult<>(result.getTotal(), result.getRecords());
        return Result.success(pageResult);
    }
    
    @GetMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<AssessmentTemplate> getTemplate(@PathVariable Long id) {
        return Result.success(templateService.getTemplateDetail(id));
    }
    
    @GetMapping("/{id}/fields")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<AssessmentField>> getTemplateFields(@PathVariable Long id) {
        List<AssessmentField> fields = fieldService.getFieldsByTemplateId(id);
        return Result.success(fields);
    }
    
    @PostMapping("/add")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE",
            action = "CREATE",
            targetType = "ASSESSMENT_TEMPLATE",
            targetId = "#createDTO.templateCode",
            description = "'新增模板：' + #createDTO.templateName"
    )
    public Result<AssessmentTemplate> addTemplate(@Valid @RequestBody AssessmentTemplateCreateDTO createDTO) {
        return Result.success(templateService.createTemplate(createDTO));
    }
    
    @PutMapping("/update")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE",
            action = "UPDATE",
            targetType = "ASSESSMENT_TEMPLATE",
            targetId = "#updateDTO.id",
            description = "'更新模板：' + #updateDTO.id"
    )
    public Result<Void> updateTemplate(@Valid @RequestBody AssessmentTemplateUpdateDTO updateDTO) {
        templateService.updateTemplate(updateDTO);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE",
            action = "DELETE",
            targetType = "ASSESSMENT_TEMPLATE",
            targetId = "#id",
            description = "'删除模板ID：' + #id"
    )
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.removeById(id);
        return Result.success("删除成功");
    }
    
    @PutMapping("/{id}/status")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE",
            action = "STATUS",
            targetType = "ASSESSMENT_TEMPLATE",
            targetId = "#id",
            description = "'更新模板状态 ID=' + #id + ' status=' + #status"
    )
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestParam Integer status) {
        templateService.updateTemplateStatus(id, status);
        return Result.success(status == 1 ? "启用成功" : "停用成功");
    }
    
    @GetMapping("/{templateCode}/versions")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<AssessmentTemplate>> getTemplateVersions(@PathVariable String templateCode) {
        LambdaQueryWrapper<AssessmentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentTemplate::getTemplateCode, templateCode);
        wrapper.eq(AssessmentTemplate::getDeleted, 0);
        wrapper.orderByDesc(AssessmentTemplate::getVersion);
        List<AssessmentTemplate> versions = templateService.list(wrapper);
        return Result.success(versions);
    }
    
    @PostMapping("/{id}/create-version")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
        module = "ASSESSMENT_TEMPLATE",
        action = "CREATE_VERSION",
        targetType = "ASSESSMENT_TEMPLATE",
        targetId = "#id",
        description = "'创建模板新版本，源ID=' + #id"
    )
    public Result<AssessmentTemplate> createNewVersion(@PathVariable Long id, @RequestBody AssessmentTemplateVersionCreateDTO newVersionDTO) {
        return Result.success(templateService.createNewVersion(id, newVersionDTO));
    }
    
    @PostMapping("/field/add")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE_FIELD",
            action = "CREATE",
            targetType = "ASSESSMENT_TEMPLATE_FIELD",
            targetId = "#field.id",
            description = "'新增模板字段 templateId=' + #field.templateId"
    )
    public Result<Void> addField(@RequestBody AssessmentField field) {
        field.setCreateTime(LocalDateTime.now());
        field.setUpdateTime(LocalDateTime.now());
        field.setDeleted(0);
        fieldService.save(field);
        return Result.success("添加成功");
    }
    
    @PutMapping("/field/update")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE_FIELD",
            action = "UPDATE",
            targetType = "ASSESSMENT_TEMPLATE_FIELD",
            targetId = "#field.id",
            description = "'更新模板字段 ID=' + #field.id"
    )
    public Result<Void> updateField(@RequestBody AssessmentField field) {
        if (field.getId() == null) {
            throw new BusinessException("字段ID不能为空");
        }
        field.setUpdateTime(LocalDateTime.now());
        fieldService.updateById(field);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/field/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "ASSESSMENT_TEMPLATE_FIELD",
            action = "DELETE",
            targetType = "ASSESSMENT_TEMPLATE_FIELD",
            targetId = "#id",
            description = "'删除模板字段ID：' + #id"
    )
    public Result<Void> deleteField(@PathVariable Long id) {
        fieldService.removeById(id);
        return Result.success("删除成功");
    }
}

