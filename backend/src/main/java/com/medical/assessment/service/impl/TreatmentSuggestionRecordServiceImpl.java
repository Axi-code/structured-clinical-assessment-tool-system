package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.entity.TreatmentSuggestion;
import com.medical.assessment.mapper.TreatmentSuggestionMapper;
import com.medical.assessment.service.TreatmentSuggestionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 诊疗建议记录服务实现
 */
@Service
public class TreatmentSuggestionRecordServiceImpl implements TreatmentSuggestionRecordService {
    
    @Autowired
    private TreatmentSuggestionMapper suggestionMapper;
    
    @Override
    public TreatmentSuggestion saveSuggestion(TreatmentSuggestion suggestion) {
        if (suggestion.getId() == null) {
            // 新增
            if (suggestion.getSuggestionNo() == null || suggestion.getSuggestionNo().isEmpty()) {
                // 生成建议编号：TS + 时间戳 + 随机数
                suggestion.setSuggestionNo("TS" + System.currentTimeMillis() + 
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }
            if (suggestion.getStatus() == null) {
                suggestion.setStatus(1);
            }
            suggestion.setCreateTime(LocalDateTime.now());
            suggestion.setUpdateTime(LocalDateTime.now());
            suggestionMapper.insert(suggestion);
        } else {
            // 更新
            suggestion.setUpdateTime(LocalDateTime.now());
            suggestionMapper.updateById(suggestion);
        }
        return suggestion;
    }
    
    @Override
    public TreatmentSuggestion getById(Long id) {
        return suggestionMapper.selectById(id);
    }
    
    @Override
    public List<TreatmentSuggestion> getByPatientId(Long patientId) {
        LambdaQueryWrapper<TreatmentSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TreatmentSuggestion::getPatientId, patientId)
               .eq(TreatmentSuggestion::getDeleted, 0)
               .orderByDesc(TreatmentSuggestion::getCreateTime);
        return suggestionMapper.selectList(wrapper);
    }
    
    @Override
    public List<TreatmentSuggestion> getByAssessmentRecordId(Long assessmentRecordId) {
        LambdaQueryWrapper<TreatmentSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TreatmentSuggestion::getAssessmentRecordId, assessmentRecordId)
               .eq(TreatmentSuggestion::getDeleted, 0)
               .orderByDesc(TreatmentSuggestion::getCreateTime);
        return suggestionMapper.selectList(wrapper);
    }
    
    @Override
    public Page<TreatmentSuggestion> getPage(Long patientId, Integer current, Integer size) {
        Page<TreatmentSuggestion> page = new Page<>(current, size);
        LambdaQueryWrapper<TreatmentSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(TreatmentSuggestion::getPatientId, patientId);
        }
        wrapper.eq(TreatmentSuggestion::getDeleted, 0)
               .orderByDesc(TreatmentSuggestion::getCreateTime);
        return suggestionMapper.selectPage(page, wrapper);
    }
    
    @Override
    public boolean deleteById(Long id) {
        TreatmentSuggestion suggestion = new TreatmentSuggestion();
        suggestion.setId(id);
        suggestion.setDeleted(1);
        suggestion.setUpdateTime(LocalDateTime.now());
        return suggestionMapper.updateById(suggestion) > 0;
    }
}
