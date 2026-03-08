package com.medical.assessment.dto;

import com.medical.assessment.entity.Patient;
import com.medical.assessment.util.PatientDesensitizationUtil;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者展示用 VO，对敏感字段进行脱敏
 * 用于列表、详情查看等场景，不包含明文敏感信息
 */
@Data
public class PatientVO {

    private Long id;
    private String patientNo;
    private String name;           // 脱敏：张*
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String idCard;         // 脱敏：1101**********34
    private String phone;          // 脱敏：138****8001
    private String address;        // 脱敏：北京市朝阳区***
    private String emergencyContact;  // 脱敏：李*
    private String emergencyPhone;    // 脱敏：139****9001
    private Long departmentId;
    private String departmentName;
    private Long diagnosisId;
    private String diagnosisName;
    private String remark;         // 脱敏：过长时截断或打星
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 Patient 实体转换为脱敏后的 VO
     */
    public static PatientVO fromPatient(Patient patient) {
        if (patient == null) {
            return null;
        }
        PatientVO vo = new PatientVO();
        vo.setId(patient.getId());
        vo.setPatientNo(patient.getPatientNo());
        vo.setName(PatientDesensitizationUtil.desensitizeName(patient.getName()));
        vo.setGender(patient.getGender());
        vo.setBirthDate(patient.getBirthDate());
        vo.setAge(patient.getAge());
        vo.setIdCard(PatientDesensitizationUtil.desensitizeIdCard(patient.getIdCard()));
        vo.setPhone(PatientDesensitizationUtil.desensitizePhone(patient.getPhone()));
        vo.setAddress(PatientDesensitizationUtil.desensitizeAddress(patient.getAddress()));
        vo.setEmergencyContact(PatientDesensitizationUtil.desensitizeName(patient.getEmergencyContact()));
        vo.setEmergencyPhone(PatientDesensitizationUtil.desensitizePhone(patient.getEmergencyPhone()));
        vo.setDepartmentId(patient.getDepartmentId());
        vo.setDepartmentName(patient.getDepartmentName());
        vo.setDiagnosisId(patient.getDiagnosisId());
        vo.setDiagnosisName(patient.getDiagnosisName());
        vo.setRemark(desensitizeRemark(patient.getRemark()));
        vo.setCreateTime(patient.getCreateTime());
        vo.setUpdateTime(patient.getUpdateTime());
        return vo;
    }

    private static String desensitizeRemark(String remark) {
        if (remark == null || remark.isEmpty()) {
            return "";
        }
        if (remark.length() > 50) {
            return remark.substring(0, 30) + "***";
        }
        return remark;
    }
}
