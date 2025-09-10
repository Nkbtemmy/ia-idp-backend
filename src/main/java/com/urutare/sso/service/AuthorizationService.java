package com.urutare.sso.service;

import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    public boolean hasPermissionOver(UserAccount user, Role targetRole) {
        return user.getRoles().stream()
            .anyMatch(role -> role.hasPermissionOver(targetRole));
    }

    public boolean canAccessDepartment(UserAccount user, Department department) {
        Set<Role> userRoles = user.getRoles();
        
        // Admins and Deans can access all departments
        if (userRoles.contains(Role.ROLE_ADMIN) || userRoles.contains(Role.ROLE_DEAN)) {
            return true;
        }
        
        // Department heads can only access their own department
        if (userRoles.contains(Role.ROLE_DEPARTMENT_HEAD)) {
            return department.getDepartmentHead() != null && 
                   department.getDepartmentHead().getId().equals(user.getId());
        }
        
        // Teachers can access their department's data (read-only)
        if (userRoles.contains(Role.ROLE_TEACHER)) {
            return department.getInstructors().stream()
                .anyMatch(instructor -> instructor.getUser().getId().equals(user.getId()));
        }
        
        return false;
    }

    public void validateDepartmentAccess(UserAccount user, Department department) {
        if (!canAccessDepartment(user, department)) {
            throw new SecurityException("Access denied to department: " + department.getName());
        }
    }

    public boolean canModifyBudget(UserAccount user, Department department) {
        Set<Role> userRoles = user.getRoles();
        
        // Only Deans and Admins can modify budgets
        if (userRoles.contains(Role.ROLE_ADMIN) || userRoles.contains(Role.ROLE_DEAN)) {
            return true;
        }
        
        // Department heads can modify their own department's allocations (not total budget)
        if (userRoles.contains(Role.ROLE_DEPARTMENT_HEAD)) {
            return department.getDepartmentHead() != null && 
                   department.getDepartmentHead().getId().equals(user.getId());
        }
        
        return false;
    }

    public boolean canViewCrossDepartmentalData(UserAccount user) {
        return user.getRoles().contains(Role.ROLE_DEAN) || 
               user.getRoles().contains(Role.ROLE_ADMIN);
    }

    public boolean canApproveAllocations(UserAccount user) {
        return user.getRoles().contains(Role.ROLE_DEAN) || 
               user.getRoles().contains(Role.ROLE_ADMIN) ||
               user.getRoles().contains(Role.ROLE_MANAGER);
    }
}
