package com.urutare.sso.repository;

import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.Instructor;
import com.urutare.sso.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, UUID> {

    Optional<Instructor> findByEmployeeId(String employeeId);

    Optional<Instructor> findByUser(UserAccount user);

    List<Instructor> findByDepartment(Department department);

    List<Instructor> findByDepartmentId(UUID departmentId);

    @Query("SELECT i FROM Instructor i WHERE i.instructorType = :type")
    List<Instructor> findByInstructorType(@Param("type") Instructor.InstructorType type);

    @Query("SELECT i FROM Instructor i WHERE i.isAvailable = true AND i.currentTeachingHours < i.maxTeachingHours")
    List<Instructor> findAvailableInstructors();

    @Query("SELECT i FROM Instructor i WHERE i.department.id = :departmentId AND i.isAvailable = true")
    List<Instructor> findAvailableInstructorsByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT i FROM Instructor i WHERE i.currentTeachingHours > (i.maxTeachingHours * 0.9)")
    List<Instructor> findOverloadedInstructors();

    @Query("SELECT i FROM Instructor i WHERE i.currentTeachingHours < (i.maxTeachingHours * 0.5)")
    List<Instructor> findUnderutilizedInstructors();

    @Query("SELECT i FROM Instructor i WHERE i.qualification = :qualification")
    List<Instructor> findByQualification(@Param("qualification") Instructor.QualificationLevel qualification);

    @Query("SELECT SUM(CASE WHEN i.instructorType = 'FULL_TIME' THEN i.annualSalary ELSE i.hourlyRate * i.currentTeachingHours * 52 END) FROM Instructor i WHERE i.department.id = :departmentId")
    BigDecimal calculateTotalInstructorCostByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT AVG(i.currentTeachingHours) FROM Instructor i WHERE i.department.id = :departmentId")
    Double getAverageTeachingHoursByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT i FROM Instructor i ORDER BY i.yearsOfExperience DESC")
    List<Instructor> findAllOrderByExperienceDesc();

    @Query("SELECT COUNT(i) FROM Instructor i WHERE i.department.id = :departmentId AND i.instructorType = :type")
    Long countByDepartmentAndType(@Param("departmentId") UUID departmentId, @Param("type") Instructor.InstructorType type);
}
