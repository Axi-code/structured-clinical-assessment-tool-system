package com.medical.assessment.entity;

/**
 * 报告模板实体，对应报告模板配置表。
 * 用于定义评估结果生成正式报告时的版式、章节、样式等配置（如 PDF/WORD 报告）。
 * 主要会被 `ReportTemplateMapper`、`ReportTemplateService`/`ReportTemplateServiceImpl`、
 * 以及生成/管理报告模板的 `ReportTemplateController` 和报告生成相关服务调用。
 */
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_template")
public class ReportTemplate extends BaseEntity {
    private String templateName;      // 模板名称
    private String templateCode;     // 模板编码
    private Long assessmentTemplateId; // 关联的评估模板ID
    private String reportType;        // 报告类型：PDF/WORD
    private String title;             // 报告标题
    private String headerConfig;     // 页眉配置（JSON格式）
    private String footerConfig;     // 页脚配置（JSON格式）
    private String sections;          // 报告章节配置（JSON格式）
    private String styleConfig;      // 样式配置（JSON格式）
    private Integer isDefault;       // 是否默认模板：0-否，1-是
    private Integer status;          // 状态：0-停用，1-启用
    private String remark;           // 备注
}
