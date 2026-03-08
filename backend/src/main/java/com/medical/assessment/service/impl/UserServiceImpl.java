package com.medical.assessment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.dto.LoginDTO;
import com.medical.assessment.dto.LoginVO;
import com.medical.assessment.dto.UserCreateDTO;
import com.medical.assessment.dto.UserUpdateDTO;
import com.medical.assessment.dto.UserVO;
import com.medical.assessment.entity.Department;
import com.medical.assessment.entity.User;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.UserMapper;
import com.medical.assessment.service.DepartmentService;
import com.medical.assessment.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户服务实现。
 * 密码存储：新用户与迁移后使用 BCrypt；旧数据保留 MD5，首次登录成功时自动迁移为 BCrypt。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String BCRYPT_PREFIX = "$2";

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private DepartmentService departmentService;

    /**
     * 判断数据库中存储的密码是否为 BCrypt 格式（以 $2a$ / $2b$ / $2y$ 开头）。
     */
    private boolean isBcryptHash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(BCRYPT_PREFIX);
    }

    /**
     * 校验明文密码与存储密码是否匹配（支持 BCrypt 与旧版 MD5）。
     */
    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // 与历史数据兼容：沿用原逻辑使用默认字符集计算 MD5
        String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return md5.equalsIgnoreCase(storedPassword);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getDeleted, 0));

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!matchesPassword(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 若仍为 MD5 存储，首次登录成功时迁移为 BCrypt 并写回（使用 LambdaUpdateWrapper 确保写库生效）
        if (!isBcryptHash(user.getPassword())) {
            String newEncoded = passwordEncoder.encode(loginDTO.getPassword());
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, user.getId())
                    .set(User::getPassword, newEncoded)
                    .set(User::getUpdateTime, LocalDateTime.now());
            this.update(updateWrapper);
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());
        StpUtil.getSession().set("role", user.getRole());
        String deptName = getDepartmentName(user.getDepartmentId());
        StpUtil.getSession().set("department", deptName);
        StpUtil.getSession().set("departmentId", user.getDepartmentId());

        return buildLoginVO(user, deptName);
    }

    @Override
    public LoginVO refreshSession(Long userId) {
        User user = this.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在或已删除");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());
        StpUtil.getSession().set("role", user.getRole());
        String deptName = getDepartmentName(user.getDepartmentId());
        StpUtil.getSession().set("department", deptName);
        StpUtil.getSession().set("departmentId", user.getDepartmentId());
        return buildLoginVO(user, deptName);
    }

    @Override
    public boolean register(User user) {
        User existUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())
                .eq(User::getDeleted, 0));

        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        if (("DOCTOR".equals(user.getRole()) || "NURSE".equals(user.getRole())) && user.getDepartmentId() == null) {
            throw new BusinessException("医生和护士必须指定科室");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);

        return this.save(user);
    }

    @Override
    public boolean register(UserCreateDTO createDTO) {
        User user = new User();
        user.setUsername(createDTO.getUsername());
        user.setPassword(createDTO.getPassword());
        user.setRealName(createDTO.getRealName());
        user.setPhone(createDTO.getPhone());
        user.setEmail(createDTO.getEmail());
        user.setRole(createDTO.getRole());
        user.setDepartmentId(createDTO.getDepartmentId());
        return register(user);
    }

    @Override
    public void updateUser(UserUpdateDTO updateDTO) {
        User user = this.getById(updateDTO.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setUsername(updateDTO.getUsername());
        user.setRealName(updateDTO.getRealName());
        user.setPhone(updateDTO.getPhone());
        user.setEmail(updateDTO.getEmail());
        user.setRole(updateDTO.getRole());
        user.setDepartmentId(updateDTO.getDepartmentId());
        user.setStatus(updateDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    public UserVO getUserVO(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        enrichUser(user);
        return UserVO.fromUser(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!matchesPassword(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        return this.updateById(user);
    }

    private String getDepartmentName(Long departmentId) {
        if (departmentId == null) return "";
        Department d = departmentService.getById(departmentId);
        return d != null ? d.getName() : "";
    }

    @Override
    public void enrichUser(User user) {
        if (user != null && user.getDepartmentId() != null) {
            user.setDepartmentName(getDepartmentName(user.getDepartmentId()));
        }
    }

    private LoginVO buildLoginVO(User user, String deptName) {
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(user.getRole());
        loginVO.setDepartment(deptName);
        loginVO.setDepartmentId(user.getDepartmentId());
        return loginVO;
    }
}
