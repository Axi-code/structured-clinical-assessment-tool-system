package com.medical.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.assessment.entity.TreatmentSuggestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 诊疗建议记录Mapper
 */
@Mapper
public interface TreatmentSuggestionMapper extends BaseMapper<TreatmentSuggestion> {
}
