package com.medical.assessment.exception;

import com.medical.assessment.common.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        return Result.error(fieldError != null ? fieldError.getDefaultMessage() : "请求参数错误");
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        return Result.error(fieldError != null ? fieldError.getDefaultMessage() : "请求参数错误");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.error("服务器内部错误");
    }
}
