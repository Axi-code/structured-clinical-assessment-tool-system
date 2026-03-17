package com.medical.assessment.dto;

/**
 * 用户信息展示 VO
 * 用途：后端把用户信息整理成前端展示需要的结构（含科室名等冗余字段）。
 * 谁传给哪个页面：
 * - 后端“我的信息/个人中心”接口 → 前端个人信息页面
 * - 后端“用户列表”接口 → 前端用户管理列表页
 */
import com.medical.assessment.entity.User;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String role;
    private Long departmentId;
    private String departmentName;
    private Integer status;

    public static UserVO fromUser(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setDepartmentName(user.getDepartmentName());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
