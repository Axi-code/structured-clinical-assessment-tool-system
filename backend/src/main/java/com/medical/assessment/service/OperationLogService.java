package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.entity.OperationLog;

import java.time.LocalDateTime;

public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 保存操作日志，自动补充时间与删除标记
     */
    void saveLog(OperationLog log);
    
    /**
     * 分页查询操作日志
     */
    Page<OperationLog> pageLogs(Integer current,
                                Integer size,
                                String module,
                                String action,
                                String username,
                                LocalDateTime startTime,
                                LocalDateTime endTime);
}
