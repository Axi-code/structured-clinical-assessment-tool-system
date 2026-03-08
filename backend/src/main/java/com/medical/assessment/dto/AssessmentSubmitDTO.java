package com.medical.assessment.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AssessmentSubmitDTO {
    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    private Map<String, Object> assessmentData;
}
