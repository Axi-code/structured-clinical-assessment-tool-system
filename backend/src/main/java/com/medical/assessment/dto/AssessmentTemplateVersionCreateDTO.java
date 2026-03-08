package com.medical.assessment.dto;

import lombok.Data;

@Data
public class AssessmentTemplateVersionCreateDTO {
    private String templateName;
    private String category;
    private String description;
    private Integer status;
    private String remark;
}
