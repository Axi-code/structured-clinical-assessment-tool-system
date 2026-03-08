package com.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_template")
public class AssessmentTemplate extends BaseEntity {
    private String templateName; // 模板名称
    private String templateCode; // 模板编码
    private String category; // 评估类别
    private String description; // 描述
    private String templateContent; // 模板内容(JSON格式)
    private Integer version; // 版本号
    private Integer status; // 0-停用 1-启用
    private String remark; // 备注
    private java.math.BigDecimal minScore; // 理论最低分（做法一：每个模板独立配置）
    private java.math.BigDecimal maxScore; // 理论最高分（做法一：每个模板独立配置）

    /** 适用科室ID列表（仅用于创建/更新时传入，不持久化到本表） */
    @TableField(exist = false)
    private List<Long> departmentIds;
}

