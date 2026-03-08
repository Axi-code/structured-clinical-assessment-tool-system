package com.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis")
public class Diagnosis extends BaseEntity {
    private String name;
    private Long departmentId;
    private String icdCode;
    private Integer sortOrder;
    private String remark;

    @TableField(exist = false)
    private String departmentName; // 关联查询用
}
