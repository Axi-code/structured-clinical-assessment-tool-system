package com.medical.assessment.entity;

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
