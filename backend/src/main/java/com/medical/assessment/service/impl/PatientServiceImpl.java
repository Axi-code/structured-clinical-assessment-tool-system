package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.dto.PatientCreateDTO;
import com.medical.assessment.dto.PatientDetailVO;
import com.medical.assessment.dto.PatientVO;
import com.medical.assessment.dto.PatientUpdateDTO;
import com.medical.assessment.entity.Department;
import com.medical.assessment.entity.Diagnosis;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.PatientMapper;
import com.medical.assessment.service.PatientService;
import com.medical.assessment.service.DepartmentService;
import com.medical.assessment.service.DiagnosisService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Resource
    private DepartmentService departmentService;
    @Resource
    private DiagnosisService diagnosisService;

    @Override
    public List<PatientVO> toPatientVOList(List<Patient> patients) {
        patients.forEach(this::enrichPatient);
        return patients.stream().map(PatientVO::fromPatient).collect(Collectors.toList());
    }

    @Override
    public void enrichPatient(Patient patient) {
        if (patient == null) return;
        if (patient.getDepartmentId() != null) {
            Department d = departmentService.getById(patient.getDepartmentId());
            if (d != null) patient.setDepartmentName(d.getName());
        }
        if (patient.getDiagnosisId() != null) {
            Diagnosis diag = diagnosisService.getById(patient.getDiagnosisId());
            if (diag != null) patient.setDiagnosisName(diag.getName());
        }
    }

    @Override
    public void validatePatient(Patient patient) {
        if (patient.getDepartmentId() == null) {
            throw new BusinessException("请选择科室");
        }
    }

    @Override
    public Patient createPatient(PatientCreateDTO createDTO) {
        Patient patient = new Patient();
        fillPatientFromCreateDTO(patient, createDTO);
        patient.setPatientNo("P" + System.currentTimeMillis());
        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        patient.setDeleted(0);
        validatePatient(patient);
        save(patient);
        return patient;
    }

    @Override
    public Patient updatePatient(PatientUpdateDTO updateDTO) {
        Patient patient = getById(updateDTO.getId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        fillPatientFromCreateDTO(patient, updateDTO);
        clearDiagnosisIfDepartmentChanged(patient);
        patient.setUpdateTime(LocalDateTime.now());
        validatePatient(patient);
        updateById(patient);
        return patient;
    }

    @Override
    public Patient updatePatientDiagnosis(Long patientId, Long diagnosisId) {
        Patient patient = getRequiredPatient(patientId);
        if (diagnosisId == null) {
            updatePatientDiagnosisField(patientId, null);
            patient.setDiagnosisId(null);
        } else {
            Diagnosis diagnosis = diagnosisService.getById(diagnosisId);
            if (diagnosis == null) {
                throw new BusinessException("诊断不存在");
            }
            if (patient.getDepartmentId() == null || !patient.getDepartmentId().equals(diagnosis.getDepartmentId())) {
                throw new BusinessException("当前诊断必须属于患者所属科室");
            }
            updatePatientDiagnosisField(patientId, diagnosisId);
            patient.setDiagnosisId(diagnosisId);
        }
        patient.setUpdateTime(LocalDateTime.now());
        return patient;
    }

    @Override
    public Diagnosis autoApplyDiagnosisByName(Long patientId, String diagnosisName) {
        if (diagnosisName == null || diagnosisName.trim().isEmpty()) {
            return null;
        }
        Patient patient = getRequiredPatient(patientId);
        if (patient.getDepartmentId() == null) {
            return null;
        }
        List<Diagnosis> diagnoses = diagnosisService.listByDepartmentId(patient.getDepartmentId());
        Diagnosis matched = matchDiagnosisByName(diagnoses, diagnosisName);
        if (matched == null) {
            return null;
        }
        if (!Objects.equals(patient.getDiagnosisId(), matched.getId())) {
            updatePatientDiagnosisField(patientId, matched.getId());
            patient.setDiagnosisId(matched.getId());
            patient.setUpdateTime(LocalDateTime.now());
        }
        return matched;
    }

    @Override
    public PatientVO getPatientVO(Long id) {
        Patient patient = getRequiredPatient(id);
        enrichPatient(patient);
        return PatientVO.fromPatient(patient);
    }

    @Override
    public PatientDetailVO getPatientDetail(Long id) {
        Patient patient = getRequiredPatient(id);
        enrichPatient(patient);
        return PatientDetailVO.fromPatient(patient);
    }

    private Patient getRequiredPatient(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        return patient;
    }

    private void fillPatientFromCreateDTO(Patient patient, PatientCreateDTO source) {
        patient.setName(source.getName());
        patient.setGender(source.getGender());
        patient.setBirthDate(source.getBirthDate());
        patient.setAge(source.getAge());
        patient.setIdCard(source.getIdCard());
        patient.setPhone(source.getPhone());
        patient.setAddress(source.getAddress());
        patient.setEmergencyContact(source.getEmergencyContact());
        patient.setEmergencyPhone(source.getEmergencyPhone());
        patient.setDepartmentId(source.getDepartmentId());
        patient.setRemark(source.getRemark());
    }

    private void clearDiagnosisIfDepartmentChanged(Patient patient) {
        if (patient.getDiagnosisId() == null || patient.getDepartmentId() == null) {
            return;
        }
        Diagnosis diagnosis = diagnosisService.getById(patient.getDiagnosisId());
        if (diagnosis == null || !patient.getDepartmentId().equals(diagnosis.getDepartmentId())) {
            patient.setDiagnosisId(null);
        }
    }

    private Diagnosis matchDiagnosisByName(List<Diagnosis> diagnoses, String diagnosisName) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return null;
        }
        String normalizedTarget = normalizeDiagnosisName(diagnosisName);
        if (normalizedTarget.isEmpty()) {
            return null;
        }

        for (Diagnosis diagnosis : diagnoses) {
            if (diagnosis == null || diagnosis.getName() == null) {
                continue;
            }
            if (normalizedTarget.equals(normalizeDiagnosisName(diagnosis.getName()))) {
                return diagnosis;
            }
        }

        return diagnoses.stream()
                .filter(Objects::nonNull)
                .filter(diagnosis -> diagnosis.getName() != null)
                .filter(diagnosis -> {
                    String normalizedName = normalizeDiagnosisName(diagnosis.getName());
                    return normalizedName.contains(normalizedTarget) || normalizedTarget.contains(normalizedName);
                })
                .min(Comparator.comparingInt(diagnosis ->
                        Math.abs(normalizeDiagnosisName(diagnosis.getName()).length() - normalizedTarget.length())))
                .orElse(null);
    }

    private String normalizeDiagnosisName(String diagnosisName) {
        if (diagnosisName == null) {
            return "";
        }
        return diagnosisName
                .replaceAll("[\\s,，。；;、:：()（）\\-_/]+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private void updatePatientDiagnosisField(Long patientId, Long diagnosisId) {
        LambdaUpdateWrapper<Patient> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Patient::getId, patientId)
                .set(Patient::getDiagnosisId, diagnosisId)
                .set(Patient::getUpdateTime, LocalDateTime.now());
        update(updateWrapper);
    }
}

