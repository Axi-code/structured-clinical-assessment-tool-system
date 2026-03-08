package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.mapper.AssessmentRecordMapper;
import com.medical.assessment.entity.Department;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.DepartmentService;
import com.medical.assessment.service.PatientService;
import com.medical.assessment.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    
    @Autowired
    private AssessmentRecordService recordService;
    
    @Autowired
    private AssessmentTemplateService templateService;
    
    @Autowired
    private AssessmentRecordMapper recordMapper;
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private PatientService patientService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public List<Map<String, Object>> statisticsByTime(LocalDateTime startTime, LocalDateTime endTime, String groupBy) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1); // 只统计已完成的记录
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        wrapper.orderByAsc(AssessmentRecord::getCreateTime);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<String, Long> timeMap = new LinkedHashMap<>();
        
        for (AssessmentRecord record : records) {
            String timeKey = formatTimeByGroup(record.getCreateTime(), groupBy);
            timeMap.put(timeKey, timeMap.getOrDefault(timeKey, 0L) + 1);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : timeMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("time", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> statisticsByDepartment(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        wrapper.isNotNull(AssessmentRecord::getDepartmentId);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<Long, Long> deptIdMap = records.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getDepartmentId() != null ? record.getDepartmentId() : -1L,
                        Collectors.counting()
                ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : deptIdMap.entrySet()) {
            if (entry.getKey() < 0) continue;
            Department d = departmentService.getById(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("department", d != null ? d.getName() : "未知科室");
            item.put("departmentId", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        // 按数量降序排序
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> statisticsByTemplate(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<Long, Long> templateCountMap = records.stream()
                .collect(Collectors.groupingBy(AssessmentRecord::getTemplateId, Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : templateCountMap.entrySet()) {
            AssessmentTemplate template = templateService.getById(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("templateId", entry.getKey());
            item.put("templateName", template != null ? template.getTemplateName() : "未知模板");
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        // 按数量降序排序
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> statisticsByRiskLevel(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        wrapper.isNotNull(AssessmentRecord::getRiskLevel);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<String, Long> riskMap = records.stream()
                .collect(Collectors.groupingBy(
                        record -> normalizeRiskLevel(record.getRiskLevel()),
                        Collectors.counting()
                ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : riskMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("riskLevel", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getIndicatorTrend(Long templateId, String fieldCode, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.eq(AssessmentRecord::getTemplateId, templateId);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        wrapper.orderByAsc(AssessmentRecord::getCreateTime);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssessmentRecord record : records) {
            try {
                Map<String, Object> assessmentData = JSON.parseObject(record.getAssessmentData(), Map.class);
                if (assessmentData != null && assessmentData.containsKey(fieldCode)) {
                    Object value = assessmentData.get(fieldCode);
                    Map<String, Object> item = new HashMap<>();
                    item.put("time", record.getCreateTime().format(DATETIME_FORMATTER));
                    item.put("value", value);
                    result.add(item);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getIndicatorDistribution(Long templateId, String fieldCode, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.eq(AssessmentRecord::getTemplateId, templateId);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<String, Long> distributionMap = new HashMap<>();
        for (AssessmentRecord record : records) {
            try {
                Map<String, Object> assessmentData = JSON.parseObject(record.getAssessmentData(), Map.class);
                if (assessmentData != null && assessmentData.containsKey(fieldCode)) {
                    Object value = assessmentData.get(fieldCode);
                    String valueStr = value != null ? value.toString() : "未知";
                    distributionMap.put(valueStr, distributionMap.getOrDefault(valueStr, 0L) + 1);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : distributionMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("value", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        
        // 按数量降序排序
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        
        return result;
    }
    
    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("patientCount", patientService.count());
        stats.put("templateCount", templateService.count());
        LambdaQueryWrapper<AssessmentRecord> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(AssessmentRecord::getDeleted, 0);
        stats.put("assessmentCount", recordService.count(allWrapper));
        LambdaQueryWrapper<AssessmentRecord> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(AssessmentRecord::getDeleted, 0);
        completedWrapper.eq(AssessmentRecord::getStatus, 1);
        stats.put("completedCount", recordService.count(completedWrapper));
        return stats;
    }
    
    @Override
    public Map<String, Object> getStatisticsOverview(LocalDateTime startTime, LocalDateTime endTime, String department) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentRecord::getDeleted, 0);
        wrapper.eq(AssessmentRecord::getStatus, 1);
        wrapper.ge(AssessmentRecord::getCreateTime, startTime);
        wrapper.le(AssessmentRecord::getCreateTime, endTime);
        
        if (department != null && !department.isEmpty()) {
            try {
                Long deptId = Long.parseLong(department);
                wrapper.eq(AssessmentRecord::getDepartmentId, deptId);
            } catch (NumberFormatException e) {
                Department dept = departmentService.listAll().stream()
                        .filter(d -> department.equals(d.getName()))
                        .findFirst().orElse(null);
                if (dept != null) {
                    wrapper.eq(AssessmentRecord::getDepartmentId, dept.getId());
                }
            }
        }
        
        List<AssessmentRecord> records = recordService.list(wrapper);
        
        Map<String, Object> overview = new HashMap<>();
        
        // 总记录数
        overview.put("totalRecords", records.size());
        
        // 平均分
        double avgScore = records.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToDouble(AssessmentRecord::getTotalScore)
                .average()
                .orElse(0.0);
        overview.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
        
        // 风险等级分布
        Map<String, Long> riskDistribution = records.stream()
                .filter(r -> r.getRiskLevel() != null)
                .collect(Collectors.groupingBy(
                        AssessmentRecord::getRiskLevel,
                        Collectors.counting()
                ));
        overview.put("riskDistribution", riskDistribution);
        
        // 科室分布（前5）
        Map<Long, Long> deptIdDistribution = records.stream()
                .filter(r -> r.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                        AssessmentRecord::getDepartmentId,
                        Collectors.counting()
                ));
        List<Map<String, Object>> topDepartments = deptIdDistribution.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    Department d = departmentService.getById(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    item.put("department", d != null ? d.getName() : "未知科室");
                    item.put("departmentId", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        overview.put("topDepartments", topDepartments);
        
        // 模板使用分布（前5）
        Map<Long, Long> templateDistribution = records.stream()
                .collect(Collectors.groupingBy(
                        AssessmentRecord::getTemplateId,
                        Collectors.counting()
                ));
        List<Map<String, Object>> topTemplates = templateDistribution.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    AssessmentTemplate template = templateService.getById(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    item.put("templateId", entry.getKey());
                    item.put("templateName", template != null ? template.getTemplateName() : "未知模板");
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        overview.put("topTemplates", topTemplates);
        
        return overview;
    }
    
    @Override
    public List<Map<String, Object>> getRiskAlert(LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  int baselineDays,
                                                  List<String> riskLevels,
                                                  double growthRateThreshold,
                                                  int minIncrease) {
        // 计算统计期长度（天），用于构造基线期时间窗
        long days = java.time.Duration.between(startTime, endTime).toDays();
        if (days <= 0) {
            days = 1;
        }
        
        // 基线期：向前回溯 baselineDays 天，以保持与统计期大致等长
        LocalDateTime baselineEnd = startTime.minusSeconds(1);
        LocalDateTime baselineStart = baselineEnd.minusDays(Math.max(baselineDays, days));
        
        // 查询统计期记录
        LambdaQueryWrapper<AssessmentRecord> currentWrapper = new LambdaQueryWrapper<>();
        currentWrapper.eq(AssessmentRecord::getDeleted, 0);
        currentWrapper.eq(AssessmentRecord::getStatus, 1);
        currentWrapper.ge(AssessmentRecord::getCreateTime, startTime);
        currentWrapper.le(AssessmentRecord::getCreateTime, endTime);
        currentWrapper.isNotNull(AssessmentRecord::getRiskLevel);
        List<AssessmentRecord> currentRecords = recordService.list(currentWrapper);
        
        // 查询基线期记录
        LambdaQueryWrapper<AssessmentRecord> baselineWrapper = new LambdaQueryWrapper<>();
        baselineWrapper.eq(AssessmentRecord::getDeleted, 0);
        baselineWrapper.eq(AssessmentRecord::getStatus, 1);
        baselineWrapper.ge(AssessmentRecord::getCreateTime, baselineStart);
        baselineWrapper.le(AssessmentRecord::getCreateTime, baselineEnd);
        baselineWrapper.isNotNull(AssessmentRecord::getRiskLevel);
        List<AssessmentRecord> baselineRecords = recordService.list(baselineWrapper);
        
        // 统计每个风险等级数量
        Map<String, Long> currentMap = currentRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> normalizeRiskLevel(r.getRiskLevel()),
                        Collectors.counting()));
        
        Map<String, Long> baselineMap = baselineRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> normalizeRiskLevel(r.getRiskLevel()),
                        Collectors.counting()));
        
        // 如果指定了要检测的风险等级，则只保留这些
        Set<String> targetRiskLevels = null;
        if (riskLevels != null && !riskLevels.isEmpty()) {
            targetRiskLevels = new HashSet<>(riskLevels);
        }
        
        List<Map<String, Object>> alerts = new ArrayList<>();
        Set<String> allLevels = new HashSet<>();
        allLevels.addAll(currentMap.keySet());
        allLevels.addAll(baselineMap.keySet());
        
        for (String level : allLevels) {
            if (targetRiskLevels != null && !targetRiskLevels.contains(level)) {
                continue;
            }
            
            long currentCount = currentMap.getOrDefault(level, 0L);
            long baselineCount = baselineMap.getOrDefault(level, 0L);
            long increase = currentCount - baselineCount;
            
            if (increase < minIncrease) {
                continue;
            }
            
            double base = baselineCount <= 0 ? 1.0 : baselineCount;
            double growthRate = increase / base;
            
            if (growthRate < growthRateThreshold) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("riskLevel", level);
            item.put("currentCount", currentCount);
            item.put("baselineCount", baselineCount);
            item.put("increase", increase);
            item.put("growthRate", Math.round(growthRate * 100.0) / 100.0);
            item.put("startTime", startTime.format(DATETIME_FORMATTER));
            item.put("endTime", endTime.format(DATETIME_FORMATTER));
            item.put("baselineStartTime", baselineStart.format(DATETIME_FORMATTER));
            item.put("baselineEndTime", baselineEnd.format(DATETIME_FORMATTER));
            
            String msg = String.format(
                    "%s患者数量在统计期较基线期上涨 %.0f%%，请关注。",
                    level,
                    growthRate * 100
            );
            item.put("message", msg);
            
            alerts.add(item);
        }
        
        // 按增长率从高到低排序
        alerts.sort((a, b) -> Double.compare(
                (Double) b.get("growthRate"),
                (Double) a.get("growthRate")
        ));
        
        return alerts;
    }
    
    /**
     * 根据分组方式格式化时间
     */
    private String formatTimeByGroup(LocalDateTime time, String groupBy) {
        if (time == null) {
            return "";
        }
        
        switch (groupBy.toLowerCase()) {
            case "day":
                return time.format(DATE_FORMATTER);
            case "week":
                // 返回周的开始日期（周一）
                int dayOfWeek = time.getDayOfWeek().getValue();
                LocalDateTime weekStart = time.minusDays(dayOfWeek - 1);
                return weekStart.format(DATE_FORMATTER) + " 周";
            case "month":
                return time.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default:
                return time.format(DATE_FORMATTER);
        }
    }
    
    /**
     * 统一风险等级显示为中文
     */
    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.trim().isEmpty()) {
            return "未知";
        }
        String v = riskLevel.trim().toUpperCase();
        switch (v) {
            case "LOW":
            case "低":
            case "低风险":
                return "低风险";
            case "MEDIUM":
            case "MIDDLE":
            case "中":
            case "中等":
            case "中风险":
                return "中风险";
            case "HIGH":
            case "高":
            case "高风险":
                return "高风险";
            case "CRITICAL":
            case "严重":
            case "严重风险":
                return "严重风险";
            default:
                // 已经是中文描述或其他自定义等级，直接返回原值
                return riskLevel;
        }
    }
}
