package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.assessment.dto.AssessmentTemplateCreateDTO;
import com.medical.assessment.dto.AssessmentTemplateUpdateDTO;
import com.medical.assessment.dto.AssessmentTemplateVersionCreateDTO;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.AssessmentTemplateMapper;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.TemplateDepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssessmentTemplateServiceImpl extends ServiceImpl<AssessmentTemplateMapper, AssessmentTemplate> implements AssessmentTemplateService {

    @Resource
    private TemplateDepartmentService templateDepartmentService;

    @Resource
    private AssessmentFieldService fieldService;

    @Override
    public AssessmentTemplate getTemplateDetail(Long id) {
        AssessmentTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        template.setDepartmentIds(templateDepartmentService.getDepartmentIdsByTemplateId(id));
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssessmentTemplate createTemplate(AssessmentTemplateCreateDTO createDTO) {
        AssessmentTemplate template = new AssessmentTemplate();
        fillTemplate(template, createDTO);
        template.setVersion(createDTO.getVersion() == null ? 1 : createDTO.getVersion());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);
        save(template);
        templateDepartmentService.saveBindings(template.getId(), createDTO.getDepartmentIds());
        template.setDepartmentIds(createDTO.getDepartmentIds());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(AssessmentTemplateUpdateDTO updateDTO) {
        AssessmentTemplate template = getById(updateDTO.getId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        fillTemplate(template, updateDTO);
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        templateDepartmentService.saveBindings(template.getId(), updateDTO.getDepartmentIds());
    }

    @Override
    public void updateTemplateStatus(Long id, Integer status) {
        AssessmentTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssessmentTemplate createNewVersion(Long id, AssessmentTemplateVersionCreateDTO newVersionDTO) {
        if (newVersionDTO == null) {
            newVersionDTO = new AssessmentTemplateVersionCreateDTO();
        }
        AssessmentTemplate sourceTemplate = getById(id);
        if (sourceTemplate == null) {
            throw new BusinessException("源模板不存在");
        }

        String templateCode = sourceTemplate.getTemplateCode();
        if (templateCode == null || templateCode.trim().isEmpty()) {
            templateCode = "TPL_" + sourceTemplate.getId();
            sourceTemplate.setTemplateCode(templateCode);
            sourceTemplate.setUpdateTime(LocalDateTime.now());
            updateById(sourceTemplate);
        }

        LambdaQueryWrapper<AssessmentTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentTemplate::getTemplateCode, templateCode);
        wrapper.eq(AssessmentTemplate::getDeleted, 0);
        wrapper.orderByDesc(AssessmentTemplate::getVersion);
        wrapper.last("LIMIT 1");
        List<AssessmentTemplate> latestList = list(wrapper);
        AssessmentTemplate latestVersion = latestList.isEmpty() ? null : latestList.get(0);
        int nextVersion = latestVersion != null ? latestVersion.getVersion() + 1 : 1;

        AssessmentTemplate template = new AssessmentTemplate();
        template.setTemplateName(isBlank(newVersionDTO.getTemplateName()) ? sourceTemplate.getTemplateName() : newVersionDTO.getTemplateName());
        template.setTemplateCode(templateCode);
        template.setCategory(isBlank(newVersionDTO.getCategory()) ? sourceTemplate.getCategory() : newVersionDTO.getCategory());
        template.setDescription(isBlank(newVersionDTO.getDescription()) ? sourceTemplate.getDescription() : newVersionDTO.getDescription());
        template.setTemplateContent(sourceTemplate.getTemplateContent());
        template.setVersion(nextVersion);
        template.setStatus(newVersionDTO.getStatus() != null ? newVersionDTO.getStatus() : 0);
        template.setRemark(isBlank(newVersionDTO.getRemark()) ? "基于版本" + sourceTemplate.getVersion() + "创建" : newVersionDTO.getRemark());
        template.setMinScore(sourceTemplate.getMinScore());
        template.setMaxScore(sourceTemplate.getMaxScore());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);
        save(template);

        List<Long> sourceDeptIds = templateDepartmentService.getDepartmentIdsByTemplateId(id);
        templateDepartmentService.saveBindings(template.getId(), sourceDeptIds);
        template.setDepartmentIds(sourceDeptIds);

        List<AssessmentField> sourceFields = fieldService.getFieldsByTemplateId(id);
        for (AssessmentField sourceField : sourceFields) {
            AssessmentField newField = new AssessmentField();
            newField.setTemplateId(template.getId());
            newField.setFieldName(sourceField.getFieldName());
            newField.setFieldCode(sourceField.getFieldCode());
            newField.setFieldType(sourceField.getFieldType());
            newField.setFieldLabel(sourceField.getFieldLabel());
            newField.setRequired(sourceField.getRequired());
            newField.setDefaultValue(sourceField.getDefaultValue());
            newField.setOptions(sourceField.getOptions());
            newField.setValidationRule(sourceField.getValidationRule());
            newField.setSortOrder(sourceField.getSortOrder());
            newField.setGroupName(sourceField.getGroupName());
            newField.setRemark(sourceField.getRemark());
            newField.setCreateTime(LocalDateTime.now());
            newField.setUpdateTime(LocalDateTime.now());
            newField.setDeleted(0);
            fieldService.save(newField);
        }

        return template;
    }

    private void fillTemplate(AssessmentTemplate template, AssessmentTemplateCreateDTO source) {
        template.setTemplateName(source.getTemplateName());
        template.setTemplateCode(source.getTemplateCode());
        template.setCategory(source.getCategory());
        template.setDescription(source.getDescription());
        template.setTemplateContent(source.getTemplateContent());
        template.setStatus(source.getStatus());
        template.setRemark(source.getRemark());
        template.setMinScore(source.getMinScore());
        template.setMaxScore(source.getMaxScore());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

