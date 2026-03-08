package com.medical.assessment.common;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String msg;
    private T data;
    
    public Result() {}
    
    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    @JsonGetter("message")
    public String getMessage() {
        return msg;
    }
}

