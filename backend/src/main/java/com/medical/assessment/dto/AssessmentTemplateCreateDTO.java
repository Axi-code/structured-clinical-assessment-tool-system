package com.medical.assessment.dto;

/**
 * 评估模板新增请求 DTO
 * 用途：承接“新增评估模板”时提交的模板配置（名称、编码、类别、内容、适用科室、分值范围等）。
 * 谁传给谁：前端模板管理-新增模板页面/弹窗 → `AssessmentTemplateController.addTemplate` → `AssessmentTemplateService.createTemplate`（`AssessmentTemplateServiceImpl.createTemplate`）
 */
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AssessmentTemplateCreateDTO {
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100个字符")
    private String templateName;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 100, message = "模板编码长度不能超过100个字符")
    private String templateCode;

    @NotBlank(message = "评估类别不能为空")
    @Size(max = 100, message = "评估类别长度不能超过100个字符")
    private String category;

    private String description;

    private String templateContent;

    private Integer version;

    @NotNull(message = "模板状态不能为空")
    private Integer status;

    private String remark;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    @NotEmpty(message = "请至少选择一个适用科室")
    private List<Long> departmentIds;
}
