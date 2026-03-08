package com.medical.assessment.dto;

import com.medical.assessment.entity.Patient;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientDetailVO {
    private Long id;
    private String patientNo;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String idCard;
    private String phone;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;
    private Long departmentId;
    private String departmentName;
    private Long diagnosisId;
    private String diagnosisName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static PatientDetailVO fromPatient(Patient patient) {
        if (patient == null) {
            return null;
        }
        PatientDetailVO vo = new PatientDetailVO();
        vo.setId(patient.getId());
        vo.setPatientNo(patient.getPatientNo());
        vo.setName(patient.getName());
        vo.setGender(patient.getGender());
        vo.setBirthDate(patient.getBirthDate());
        vo.setAge(patient.getAge());
        vo.setIdCard(patient.getIdCard());
        vo.setPhone(patient.getPhone());
        vo.setAddress(patient.getAddress());
        vo.setEmergencyContact(patient.getEmergencyContact());
        vo.setEmergencyPhone(patient.getEmergencyPhone());
        vo.setDepartmentId(patient.getDepartmentId());
        vo.setDepartmentName(patient.getDepartmentName());
        vo.setDiagnosisId(patient.getDiagnosisId());
        vo.setDiagnosisName(patient.getDiagnosisName());
        vo.setRemark(patient.getRemark());
        vo.setCreateTime(patient.getCreateTime());
        vo.setUpdateTime(patient.getUpdateTime());
        return vo;
    }
}
