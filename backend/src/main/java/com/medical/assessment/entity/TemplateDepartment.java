package com.medical.assessment.entity;

/**
 * 模板-科室关联实体，对应评估模板与科室之间的多对多关系表。
 * 用于配置某个评估模板适用于哪些科室，在模板管理和权限控制时作为关联数据使用。
 * 主要会被 `TemplateDepartmentMapper`、`TemplateDepartmentService`/`TemplateDepartmentServiceImpl`，
 * 以及模板管理相关的 `AssessmentTemplateServiceImpl`、`AssessmentTemplateController` 等业务调用。
 */
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("template_department")
public class TemplateDepartment implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long departmentId;
    private LocalDateTime createTime;
}
