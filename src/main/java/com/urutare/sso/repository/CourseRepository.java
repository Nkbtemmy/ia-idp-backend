package com.urutare.sso.repository;

import com.urutare.sso.entity.Course;
import com.urutare.sso.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByDepartment(Department department);

    List<Course> findByDepartmentId(UUID departmentId);

    @Query("SELECT c FROM Course c WHERE c.department.id = :departmentId AND c.academicYear = :year")
    List<Course> findByDepartmentAndYear(@Param("departmentId") UUID departmentId, @Param("year") Integer year);

    @Query("SELECT c FROM Course c WHERE c.currentEnrollment > c.maxStudents")
    List<Course> findOverEnrolledCourses();

    @Query("SELECT c FROM Course c WHERE c.currentEnrollment < (c.maxStudents * 0.5)")
    List<Course> findUnderEnrolledCourses();

    @Query("SELECT c FROM Course c WHERE c.projectedEnrollment > c.currentEnrollment")
    List<Course> findCoursesWithGrowthProjection();

    @Query("SELECT c FROM Course c WHERE c.semester = :semester AND c.academicYear = :year")
    List<Course> findBySemesterAndYear(@Param("semester") Course.Semester semester, @Param("year") Integer year);

    @Query("SELECT SUM(c.costPerStudent * c.projectedEnrollment) FROM Course c WHERE c.department.id = :departmentId")
    BigDecimal calculateTotalProjectedCostByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT c FROM Course c WHERE c.courseType = :type")
    List<Course> findByCourseType(@Param("type") Course.CourseType type);

    @Query("SELECT c FROM Course c ORDER BY (c.costPerStudent * c.projectedEnrollment) DESC")
    List<Course> findAllOrderByTotalCostDesc();

    @Query("SELECT AVG(c.currentEnrollment) FROM Course c WHERE c.department.id = :departmentId")
    Double getAverageEnrollmentByDepartment(@Param("departmentId") UUID departmentId);
}
