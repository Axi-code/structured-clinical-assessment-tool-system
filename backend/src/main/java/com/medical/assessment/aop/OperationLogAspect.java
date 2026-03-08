package com.medical.assessment.aop;

import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.OperationLog;
import com.medical.assessment.service.OperationLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class OperationLogAspect {
    
    @Autowired
    private OperationLogService operationLogService;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @AfterReturning(pointcut = "@annotation(com.medical.assessment.annotation.OperationLogRecord)", returning = "retVal")
    public void afterReturning(JoinPoint joinPoint, Object retVal) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLogRecord ann = method.getAnnotation(OperationLogRecord.class);
            if (ann == null) {
                return;
            }
            
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            
            OperationLog log = new OperationLog();
            log.setModule(ann.module());
            log.setAction(ann.action());
            log.setTargetType(ann.targetType());
            log.setIp(request.getRemoteAddr());
            
            // 用户信息（由 SaTokenInterceptor 放入 request）
            Object userId = request.getAttribute("userId");
            Object username = request.getAttribute("username");
            Object realName = request.getAttribute("realName");
            Object role = request.getAttribute("role");
            if (userId instanceof Long) {
                log.setUserId((Long) userId);
            }
            if (username != null) {
                log.setUsername(username.toString());
            }
            if (realName != null) {
                log.setRealName(realName.toString());
            }
            if (role != null) {
                log.setRole(role.toString());
            }
            
            // 构建 SpEL 上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            context.setVariable("ret", retVal);
            Object retData = extractResultData(retVal);
            context.setVariable("retData", retData);
            
            // targetId
            if (StringUtils.hasText(ann.targetId())) {
                Object val = evaluate(ann.targetId(), context);
                if (val != null) {
                    try {
                        log.setTargetId(Long.valueOf(val.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            
            // description
            if (StringUtils.hasText(ann.description())) {
                Object val = evaluate(ann.description(), context);
                if (val != null) {
                    log.setDescription(val.toString());
                }
            }
            
            operationLogService.saveLog(log);
        } catch (Exception ignored) {
            // 为避免影响业务流程，日志异常吞掉
        }
    }
    
    private Object evaluate(String spel, StandardEvaluationContext context) {
        try {
            Expression expression = parser.parseExpression(spel);
            return expression.getValue(context);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    private Object extractResultData(Object retVal) {
        if (retVal instanceof Result) {
            return ((Result<?>) retVal).getData();
        }
        return null;
    }
}
