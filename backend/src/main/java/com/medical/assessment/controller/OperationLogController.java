package com.medical.assessment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.OperationLog;
import com.medical.assessment.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/operation-log")
public class OperationLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 操作日志分页查询（管理员查看）
     */
    @GetMapping("/page")
    @RequiresRoles({"ADMIN"})
    public Result<PageResult<OperationLog>> pageLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<OperationLog> page = operationLogService.pageLogs(current, size, module, action, username, startTime, endTime);
        return Result.success(PageResult.of(page));
    }
}
