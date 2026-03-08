package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.dto.LoginDTO;
import com.medical.assessment.dto.LoginVO;
import com.medical.assessment.dto.UserCreateDTO;
import com.medical.assessment.dto.UserUpdateDTO;
import com.medical.assessment.dto.UserVO;
import com.medical.assessment.entity.User;

public interface UserService extends IService<User> {
    void enrichUser(User user);
    LoginVO login(LoginDTO loginDTO);
    /** 根据 userId 建立新 access 会话并返回 token 与用户信息（供 refresh 使用） */
    LoginVO refreshSession(Long userId);
    boolean register(User user);
    boolean register(UserCreateDTO createDTO);
    void updateUser(UserUpdateDTO updateDTO);
    UserVO getUserVO(Long userId);
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
}

