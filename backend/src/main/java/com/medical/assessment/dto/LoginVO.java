package com.medical.assessment.dto;

/**
 * 登录结果返回 VO
 * 用途：返回登录成功后的会话信息（token、用户信息、科室信息等）。
 * 谁传给哪个页面：后端登录/刷新接口 → 前端登录页（登录成功后保存 token 并跳转）/ 需要刷新会话的页面
 */
import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String department;
    private Long departmentId;
}

