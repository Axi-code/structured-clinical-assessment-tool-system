package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.dto.PatientCreateDTO;
import com.medical.assessment.dto.PatientDetailVO;
import com.medical.assessment.dto.PatientUpdateDTO;
import com.medical.assessment.dto.PatientVO;
import com.medical.assessment.entity.Patient;

import java.util.List;

public interface PatientService extends IService<Patient> {
    /** 将患者列表转为 VO 并填充科室、诊断名称 */
    List<PatientVO> toPatientVOList(List<Patient> patients);
    /** 填充单个患者的科室、诊断名称 */
    void enrichPatient(Patient patient);
    /** 校验患者建档基础数据 */
    void validatePatient(Patient patient);
    /** 创建患者 */
    Patient createPatient(PatientCreateDTO createDTO);
    /** 更新患者 */
    Patient updatePatient(PatientUpdateDTO updateDTO);
    /** 确认或修改患者当前诊断 */
    Patient updatePatientDiagnosis(Long patientId, Long diagnosisId);
    /** 获取脱敏后的患者详情 */
    PatientVO getPatientVO(Long id);
    /** 获取用于编辑的患者详情 */
    PatientDetailVO getPatientDetail(Long id);
}

