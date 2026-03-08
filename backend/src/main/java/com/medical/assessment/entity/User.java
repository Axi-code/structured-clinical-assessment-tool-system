package com.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medical.assessment.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String role; // ADMIN, DOCTOR, NURSE
    private Long departmentId;

    @TableField(exist = false)
    private String departmentName; // 关联展示用
    private Integer status; // 0-禁用 1-启用
}

