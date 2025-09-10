package com.urutare.sso.service;

import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuthorizationService authorizationService;

    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public Department getDepartmentById(UUID id, UserAccount currentUser) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found"));
        
        // Department heads can only view their own department
        if (currentUser.getRoles().contains(Role.ROLE_DEPARTMENT_HEAD)) {
            authorizationService.validateDepartmentAccess(currentUser, department);
        }
        
        return department;
    }

    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public Department updateDepartment(UUID id, Department updatedDepartment, UserAccount currentUser) {
        Department existing = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found"));
        
        existing.setName(updatedDepartment.getName());
        existing.setDescription(updatedDepartment.getDescription());
        existing.setDepartmentHead(updatedDepartment.getDepartmentHead());
        existing.setTotalBudget(updatedDepartment.getTotalBudget());
        existing.setEnrollmentProjection(updatedDepartment.getEnrollmentProjection());
        
        return departmentRepository.save(existing);
    }

    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public List<Department> getDepartmentsByUser(UserAccount user) {
        if (user.getRoles().contains(Role.ROLE_DEPARTMENT_HEAD)) {
            return departmentRepository.findByDepartmentHead(user);
        }
        return departmentRepository.findAll();
    }

    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public void transferBudget(UUID fromDepartmentId, UUID toDepartmentId, BigDecimal amount, UserAccount currentUser) {
        Department fromDept = departmentRepository.findById(fromDepartmentId)
            .orElseThrow(() -> new RuntimeException("Source department not found"));
        Department toDept = departmentRepository.findById(toDepartmentId)
            .orElseThrow(() -> new RuntimeException("Target department not found"));

        if (fromDept.getRemainingBudget().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient budget for transfer");
        }

        fromDept.setTotalBudget(fromDept.getTotalBudget().subtract(amount));
        toDept.setTotalBudget(toDept.getTotalBudget().add(amount));

        departmentRepository.save(fromDept);
        departmentRepository.save(toDept);
    }

    public List<Department> getOverBudgetDepartments() {
        return departmentRepository.findOverBudgetDepartments();
    }

    public BigDecimal getTotalInstitutionBudget() {
        return departmentRepository.getTotalInstitutionBudget();
    }
}
