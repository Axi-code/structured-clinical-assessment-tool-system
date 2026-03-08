package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.entity.OperationLog;
import com.medical.assessment.mapper.OperationLogMapper;
import com.medical.assessment.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
    
    @Override
    public void saveLog(OperationLog log) {
        if (log == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setDeleted(0);
        this.save(log);
    }
    
    @Override
    public Page<OperationLog> pageLogs(Integer current,
                                       Integer size,
                                       String module,
                                       String action,
                                       String username,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        Page<OperationLog> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getDeleted, 0);
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return this.page(page, wrapper);
    }
}
