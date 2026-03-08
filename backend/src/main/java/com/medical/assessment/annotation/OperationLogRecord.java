package com.medical.assessment.annotation;

import java.lang.annotation.*;

/**
 * 操作日志记录注解
 * 支持 SpEL 表达式：
 * - #paramName 获取方法参数
 * - #ret     获取方法返回值（Result 对象）
 * - #retData 获取返回值中的 data 字段（Result.data）
 * - Map 参数可用 #body['key'] 访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogRecord {
    String module();          // 模块，如 PATIENT / ASSESSMENT_RECORD
    String action();          // 动作，如 CREATE / UPDATE / DELETE / SAVE / SUBMIT
    String targetType() default ""; // 目标类型，如 PATIENT / ASSESSMENT_RECORD
    String targetId() default "";   // SpEL，返回 Long 或可转为 Long
    String description() default ""; // SpEL，返回字符串；若为空则不记录描述
}
