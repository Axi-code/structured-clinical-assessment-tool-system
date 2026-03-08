package com.medical.assessment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务
 */
public interface StatisticsService {
    /**
     * 按时间统计评估记录数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param groupBy 分组方式：day/week/month
     * @return 统计数据
     */
    List<Map<String, Object>> statisticsByTime(LocalDateTime startTime, LocalDateTime endTime, String groupBy);
    
    /**
     * 按科室统计评估记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    List<Map<String, Object>> statisticsByDepartment(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按评估模板统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    List<Map<String, Object>> statisticsByTemplate(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按风险等级统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    List<Map<String, Object>> statisticsByRiskLevel(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 评估指标趋势分析（按时间）
     * @param templateId 模板ID
     * @param fieldCode 字段编码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 趋势数据
     */
    List<Map<String, Object>> getIndicatorTrend(Long templateId, String fieldCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 评估指标分布统计
     * @param templateId 模板ID
     * @param fieldCode 字段编码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分布数据
     */
    List<Map<String, Object>> getIndicatorDistribution(Long templateId, String fieldCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 综合统计概览
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param department 科室（可选）
     * @return 综合统计数据
     */
    Map<String, Object> getStatisticsOverview(LocalDateTime startTime, LocalDateTime endTime, String department);
    
    /**
     * 首页仪表盘统计（患者数、评估记录数、模板数、已完成评估数）
     * @return 仪表盘统计数据
     */
    Map<String, Object> getDashboardStats();
    
    /**
     * 风险等级异常预警
     * @param startTime 统计期开始时间
     * @param endTime 统计期结束时间
     * @param baselineDays 基线窗口天数
     * @param riskLevels 需要检测的风险等级列表（可选，null 或空表示全部）
     * @param growthRateThreshold 增长率阈值（如 0.5 表示上涨 50%）
     * @param minIncrease 最小新增数量阈值
     * @return 满足预警条件的风险等级列表
     */
    List<Map<String, Object>> getRiskAlert(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int baselineDays,
            List<String> riskLevels,
            double growthRateThreshold,
            int minIncrease);
}

