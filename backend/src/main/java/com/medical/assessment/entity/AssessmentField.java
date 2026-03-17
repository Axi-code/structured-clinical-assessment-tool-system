package com.medical.assessment.entity;

/**
 * 评估字段实体，对应评估模板中的单个字段配置表。
 * 描述字段的名称、编码、类型、标签、选项、校验规则、排序及分组，用于渲染评估表单并校验输入。
 * 主要会被 `AssessmentFieldMapper`、`AssessmentFieldService`/`AssessmentFieldServiceImpl`、
 * 以及模板设计/渲染相关的控制层、评估会话服务和前端表单构建逻辑调用。
 */
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_field")
public class AssessmentField extends BaseEntity {
    private Long templateId; // 模板ID
    private String fieldName; // 字段名称
    private String fieldCode; // 字段编码
    private String fieldType; // 字段类型: TEXT, NUMBER, SELECT, DATE, RADIO, CHECKBOX
    private String fieldLabel; // 字段标签
    private Integer required; // 是否必填 0-否 1-是
    private String defaultValue; // 默认值
    private String options; // 选项值(JSON格式，用于SELECT/RADIO/CHECKBOX)
    private String validationRule; // 验证规则(JSON格式)
    private Integer sortOrder; // 排序
    private String groupName; // 分组名称
    private String remark; // 备注
}

