package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.dto.CaptchaVO;
import com.medical.assessment.dto.LoginDTO;
import com.medical.assessment.dto.LoginVO;
import com.medical.assessment.dto.UserCreateDTO;
import com.medical.assessment.dto.UserUpdateDTO;
import com.medical.assessment.dto.UserVO;
import com.medical.assessment.entity.User;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.service.LoginSecurityService;
import com.medical.assessment.service.RefreshTokenService;
import com.medical.assessment.service.UserService;
import com.medical.assessment.util.CaptchaImageUtil;
import com.medical.assessment.util.RefreshTokenCookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginSecurityService loginSecurityService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenCookieUtil refreshTokenCookieUtil;

    private static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        try {
            Object[] gen = CaptchaImageUtil.generate();
            String code = (String) gen[0];
            byte[] imageBytes = (byte[]) gen[1];
            String key = loginSecurityService.saveCaptcha(code);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            CaptchaVO vo = new CaptchaVO(key, "data:image/png;base64," + base64);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("验证码生成失败");
        }
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIp(request);
        try {
            loginSecurityService.checkRateLimit(clientIp);
        } catch (Exception e) {
            throw new BusinessException(429, e.getMessage());
        }
        if (loginSecurityService.needCaptcha(clientIp)) {
            if (!loginSecurityService.verifyCaptcha(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode())) {
                throw new BusinessException(400, "请完成验证码");
            }
        }
        try {
            LoginVO loginVO = userService.login(loginDTO);
            loginSecurityService.clearLoginFail(clientIp);
            String refreshToken = refreshTokenService.create(loginVO.getUserId());
            refreshTokenCookieUtil.setRefreshTokenCookie(response, refreshToken);
            return Result.success(loginVO);
        } catch (Exception e) {
            loginSecurityService.recordLoginFail(clientIp);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieUtil.getRefreshTokenFromCookie(request);
        Long userId = refreshToken != null ? refreshTokenService.validateAndConsume(refreshToken) : null;
        if (userId == null) {
            refreshTokenCookieUtil.clearRefreshTokenCookie(response);
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        String newRefreshToken = refreshTokenService.create(userId);
        refreshTokenCookieUtil.setRefreshTokenCookie(response, newRefreshToken);
        LoginVO loginVO = userService.refreshSession(userId);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            refreshTokenService.revokeByUserId(userId);
            try {
                cn.dev33.satoken.stp.StpUtil.logout(userId);
            } catch (Exception ignored) {}
        }
        refreshTokenCookieUtil.clearRefreshTokenCookie(response);
        return Result.success("已退出登录");
    }
    
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserCreateDTO createDTO) {
        userService.register(createDTO);
        return Result.success("注册成功");
    }
    
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getUserVO(userId));
    }
    
    @GetMapping("/list")
    @RequiresRoles({"ADMIN"})
    public Result<PageResult<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> result = userService.page(page, wrapper);
        result.getRecords().forEach(userService::enrichUser);
        List<UserVO> records = result.getRecords().stream().map(UserVO::fromUser).collect(Collectors.toList());
        PageResult<UserVO> pageResult = new PageResult<>(result.getTotal(), records);
        return Result.success(pageResult);
    }
    
    @PostMapping("/add")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "USER",
            action = "CREATE",
            targetType = "USER",
            targetId = "#createDTO.username",
            description = "'新增用户：' + #createDTO.username"
    )
    public Result<Void> addUser(@Valid @RequestBody UserCreateDTO createDTO) {
        userService.register(createDTO);
        return Result.success("添加成功");
    }
    
    @PutMapping("/update")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "USER",
            action = "UPDATE",
            targetType = "USER",
            targetId = "#updateDTO.id",
            description = "'更新用户：' + #updateDTO.username"
    )
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO updateDTO) {
        userService.updateUser(updateDTO);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/delete/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "USER",
            action = "DELETE",
            targetType = "USER",
            targetId = "#id",
            description = "'删除用户ID：' + #id"
    )
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }
    
    @PutMapping("/password")
    @OperationLogRecord(
            module = "USER",
            action = "PASSWORD_CHANGE",
            targetType = "USER",
            targetId = "#request.getAttribute('userId')",
            description = "'用户修改密码 userId=' + #request.getAttribute('userId')"
    )
    public Result<Void> updatePassword(
            HttpServletRequest request,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }
}

