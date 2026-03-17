package com.medical.assessment.dto;

/**
 * 诊断新增请求 DTO
 * 用途：承接“新增诊断/维护诊断字典”时提交的诊断信息（名称、科室、ICD 等）。
 * 谁传给谁：
 * - 前端诊断管理-新增诊断页面/弹窗 → `DiagnosisController.add` → `DiagnosisService.createDiagnosis`（`DiagnosisServiceImpl.createDiagnosis`）
 * - 前端患者详情页采纳 AI 诊断（若库中不存在）→ 后端在 `PatientController.adoptLatestAiDiagnosis` 内部组装该 DTO 并调用创建逻辑
 */
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DiagnosisCreateDTO {
    @NotBlank(message = "诊断名称不能为空")
    @Size(max = 100, message = "诊断名称长度不能超过100个字符")
    private String name;

    @NotNull(message = "所属科室不能为空")
    private Long departmentId;

    @Size(max = 50, message = "ICD编码长度不能超过50个字符")
    @Pattern(regexp = "^[A-Za-z0-9.\\-_]*$", message = "ICD编码格式不正确")
    private String icdCode;

    private Integer sortOrder;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
