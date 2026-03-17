package com.medical.assessment.entity;

/**
 * 科室实体，对应医院科室基础信息表。
 * 用于维护系统中的科室名称、编码、排序等信息，是患者、诊断、评估模板等多处的关联维度。
 * 主要会被 `DepartmentMapper`、`DepartmentService`/`DepartmentServiceImpl`、
 * `DepartmentController`，以及与科室分布相关的评估、统计报表等业务模块调用。
 */
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("department")
public class Department extends BaseEntity {
    private String name;
    private String code;
    private Integer sortOrder;
    private String remark;
}
