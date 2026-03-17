package com.medical.assessment.entity;

/**
 * 系统用户实体，对应系统用户表 `sys_user`。
 * 记录登录账号、姓名、联系方式、角色、所属科室及状态等信息，用于权限控制和操作审计。
 * 主要会被 `UserMapper`、`UserService`/`UserServiceImpl`、`UserController`，
 * 以及登录认证、安全控制、操作日志等相关业务模块调用。
 */
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

