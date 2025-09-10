package com.urutare.sso.service;

import com.urutare.sso.entity.Course;
import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.CourseRepository;
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
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final AuthorizationService authorizationService;

    @PreAuthorize("hasRole('TEACHER') or hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public List<Course> getCoursesByDepartment(UUID departmentId, UserAccount currentUser) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new RuntimeException("Department not found"));
        
        authorizationService.validateDepartmentAccess(currentUser, department);
        return courseRepository.findByDepartmentId(departmentId);
    }

    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public Course createCourse(Course course, UserAccount currentUser) {
        authorizationService.validateDepartmentAccess(currentUser, course.getDepartment());
        return courseRepository.save(course);
    }

    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public Course updateCourse(UUID id, Course updatedCourse, UserAccount currentUser) {
        Course existing = courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        authorizationService.validateDepartmentAccess(currentUser, existing.getDepartment());
        
        existing.setName(updatedCourse.getName());
        existing.setDescription(updatedCourse.getDescription());
        existing.setMaxStudents(updatedCourse.getMaxStudents());
        existing.setProjectedEnrollment(updatedCourse.getProjectedEnrollment());
        existing.setCostPerStudent(updatedCourse.getCostPerStudent());
        existing.setEquipmentCost(updatedCourse.getEquipmentCost());
        existing.setMaterialsCost(updatedCourse.getMaterialsCost());
        
        return courseRepository.save(existing);
    }

    public List<Course> getOverEnrolledCourses() {
        return courseRepository.findOverEnrolledCourses();
    }

    public List<Course> getUnderEnrolledCourses() {
        return courseRepository.findUnderEnrolledCourses();
    }

    public BigDecimal calculateDepartmentProjectedCosts(UUID departmentId) {
        return courseRepository.calculateTotalProjectedCostByDepartment(departmentId);
    }
}
