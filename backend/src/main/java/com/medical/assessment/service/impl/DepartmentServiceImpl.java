package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.dto.DepartmentCreateDTO;
import com.medical.assessment.dto.DepartmentUpdateDTO;
import com.medical.assessment.entity.Department;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.DepartmentMapper;
import com.medical.assessment.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Override
    public List<Department> listAll() {
        return list(new LambdaQueryWrapper<Department>()
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSortOrder));
    }

    @Override
    public Department createDepartment(DepartmentCreateDTO createDTO) {
        Department department = new Department();
        fillDepartment(department, createDTO);
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        department.setDeleted(0);
        save(department);
        return department;
    }

    @Override
    public void updateDepartment(DepartmentUpdateDTO updateDTO) {
        Department department = getById(updateDTO.getId());
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        fillDepartment(department, updateDTO);
        department.setUpdateTime(LocalDateTime.now());
        updateById(department);
    }

    private void fillDepartment(Department department, DepartmentCreateDTO source) {
        department.setName(source.getName());
        department.setCode(source.getCode());
        department.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        department.setRemark(source.getRemark());
    }
}
