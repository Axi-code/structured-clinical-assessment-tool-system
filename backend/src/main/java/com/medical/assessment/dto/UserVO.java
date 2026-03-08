package com.medical.assessment.dto;

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
