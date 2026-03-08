package com.medical.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.assessment.entity.TemplateDepartment;
import com.medical.assessment.mapper.TemplateDepartmentMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TemplateDepartmentService {

    @Resource
    private TemplateDepartmentMapper templateDepartmentMapper;

    public List<Long> getTemplateIdsByDepartmentId(Long departmentId) {
        if (departmentId == null) return Collections.emptyList();
        return templateDepartmentMapper.selectList(
                new LambdaQueryWrapper<TemplateDepartment>()
                        .eq(TemplateDepartment::getDepartmentId, departmentId)
                        .select(TemplateDepartment::getTemplateId))
                .stream()
                .map(TemplateDepartment::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 获取模板绑定的科室ID列表 */
    public List<Long> getDepartmentIdsByTemplateId(Long templateId) {
        if (templateId == null) return Collections.emptyList();
        return templateDepartmentMapper.selectList(
                new LambdaQueryWrapper<TemplateDepartment>()
                        .eq(TemplateDepartment::getTemplateId, templateId)
                        .select(TemplateDepartment::getDepartmentId))
                .stream()
                .map(TemplateDepartment::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 保存模板-科室绑定（先删后插） */
    public void saveBindings(Long templateId, List<Long> departmentIds) {
        if (templateId == null) return;
        templateDepartmentMapper.delete(new LambdaQueryWrapper<TemplateDepartment>()
                .eq(TemplateDepartment::getTemplateId, templateId));
        if (departmentIds != null && !departmentIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Long deptId : departmentIds) {
                if (deptId == null) continue;
                TemplateDepartment td = new TemplateDepartment();
                td.setTemplateId(templateId);
                td.setDepartmentId(deptId);
                td.setCreateTime(now);
                templateDepartmentMapper.insert(td);
            }
        }
    }
}
