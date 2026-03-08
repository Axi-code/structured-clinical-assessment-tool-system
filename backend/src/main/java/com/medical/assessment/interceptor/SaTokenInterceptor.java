package com.medical.assessment.interceptor;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.medical.assessment.annotation.RequiresRoles;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Sa-Token权限拦截器
 */
@Component
public class SaTokenInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 登录和注册接口放行
        String uri = request.getRequestURI();
        if (uri.contains("/login") || uri.contains("/register") || uri.contains("/error")) {
            return true;
        }
        
        // 检查登录状态
        // Sa-Token已配置为从Authorization: Bearer {token} header读取token
        // 通过application.yml配置：token-name: Authorization, token-prefix: Bearer
        try {
            StpUtil.checkLogin();
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或token已过期\"}");
            return false;
        }
        
        // 角色权限检查
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresRoles requiresRoles = handlerMethod.getMethodAnnotation(RequiresRoles.class);
            if (requiresRoles != null) {
                String[] roles = requiresRoles.value();
                String userRole = (String) StpUtil.getSession().get("role");
                if (userRole == null || !Arrays.asList(roles).contains(userRole)) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
                    return false;
                }
            }
        }
        
        // 将用户信息存入request
        Long userId = StpUtil.getLoginIdAsLong();
        String username = (String) StpUtil.getSession().get("username");
        String realName = (String) StpUtil.getSession().get("realName");
        String role = (String) StpUtil.getSession().get("role");
        String department = (String) StpUtil.getSession().get("department");
        Object departmentId = StpUtil.getSession().get("departmentId");
        
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("realName", realName);
        request.setAttribute("role", role);
        request.setAttribute("department", department);
        request.setAttribute("departmentId", departmentId);
        
        return true;
    }
}
