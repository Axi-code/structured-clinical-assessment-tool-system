package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.dto.DepartmentCreateDTO;
import com.medical.assessment.dto.DepartmentUpdateDTO;
import com.medical.assessment.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    List<Department> listAll();

    Department createDepartment(DepartmentCreateDTO createDTO);

    void updateDepartment(DepartmentUpdateDTO updateDTO);
}
