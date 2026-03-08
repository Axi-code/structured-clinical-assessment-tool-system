package com.medical.assessment.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class PatientCreateDTO {
    @NotBlank(message = "患者姓名不能为空")
    @Size(max = 50, message = "患者姓名长度不能超过50个字符")
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "男|女", message = "性别只能为男或女")
    private String gender;

    private LocalDate birthDate;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Size(max = 32, message = "身份证号长度不能超过32个字符")
    private String idCard;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Size(max = 255, message = "地址长度不能超过255个字符")
    private String address;

    @Size(max = 50, message = "紧急联系人长度不能超过50个字符")
    private String emergencyContact;

    @Size(max = 20, message = "紧急联系电话长度不能超过20个字符")
    private String emergencyPhone;

    @NotNull(message = "请选择科室")
    private Long departmentId;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
