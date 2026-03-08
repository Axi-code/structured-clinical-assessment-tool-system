package com.medical.assessment.controller;

import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.Result;
import com.medical.assessment.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 按时间统计评估记录数量
     */
    @GetMapping("/time")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> statisticsByTime(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "day") String groupBy) {
        List<Map<String, Object>> data = statisticsService.statisticsByTime(startTime, endTime, groupBy);
        return Result.success(data);
    }
    
    /**
     * 按科室统计评估记录
     */
    @GetMapping("/department")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> statisticsByDepartment(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<Map<String, Object>> data = statisticsService.statisticsByDepartment(startTime, endTime);
        return Result.success(data);
    }
    
    /**
     * 按评估模板统计
     */
    @GetMapping("/template")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> statisticsByTemplate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<Map<String, Object>> data = statisticsService.statisticsByTemplate(startTime, endTime);
        return Result.success(data);
    }
    
    /**
     * 按风险等级统计
     */
    @GetMapping("/risk-level")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> statisticsByRiskLevel(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<Map<String, Object>> data = statisticsService.statisticsByRiskLevel(startTime, endTime);
        return Result.success(data);
    }
    
    /**
     * 评估指标趋势分析
     */
    @GetMapping("/indicator/trend")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> getIndicatorTrend(
            @RequestParam Long templateId,
            @RequestParam String fieldCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<Map<String, Object>> data = statisticsService.getIndicatorTrend(templateId, fieldCode, startTime, endTime);
        return Result.success(data);
    }
    
    /**
     * 评估指标分布统计
     */
    @GetMapping("/indicator/distribution")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> getIndicatorDistribution(
            @RequestParam Long templateId,
            @RequestParam String fieldCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<Map<String, Object>> data = statisticsService.getIndicatorDistribution(templateId, fieldCode, startTime, endTime);
        return Result.success(data);
    }
    
    /**
     * 首页仪表盘统计（患者数、评估记录数、模板数、已完成评估数）
     */
    @GetMapping("/dashboard")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> data = statisticsService.getDashboardStats();
        return Result.success(data);
    }
    
    /**
     * 综合统计概览
     */
    @GetMapping("/overview")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<Map<String, Object>> getStatisticsOverview(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String department) {
        Map<String, Object> data = statisticsService.getStatisticsOverview(startTime, endTime, department);
        return Result.success(data);
    }
    
    /**
     * 风险等级异常预警
     */
    @GetMapping("/risk-alert")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<Map<String, Object>>> getRiskAlert(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "7") Integer baselineDays,
            @RequestParam(required = false) String riskLevels,
            @RequestParam(defaultValue = "0.5") Double growthRateThreshold,
            @RequestParam(defaultValue = "5") Integer minIncrease) {
        
        List<String> riskLevelList = null;
        if (riskLevels != null && !riskLevels.trim().isEmpty()) {
            String[] parts = riskLevels.split(",");
            riskLevelList = new java.util.ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    riskLevelList.add(trimmed);
                }
            }
        }
        
        List<Map<String, Object>> alerts = statisticsService.getRiskAlert(
                startTime,
                endTime,
                baselineDays != null ? baselineDays : 7,
                riskLevelList,
                growthRateThreshold != null ? growthRateThreshold : 0.5,
                minIncrease != null ? minIncrease : 5
        );
        return Result.success(alerts);
    }
}
