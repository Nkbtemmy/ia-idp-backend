package com.urutare.sso.repository;

import com.urutare.sso.entity.Course;
import com.urutare.sso.entity.CourseInstructor;
import com.urutare.sso.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, UUID> {

    List<CourseInstructor> findByCourse(Course course);

    List<CourseInstructor> findByInstructor(Instructor instructor);

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.assignmentType = :type")
    List<CourseInstructor> findByAssignmentType(@Param("type") CourseInstructor.AssignmentType type);

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.instructor.id = :instructorId")
    List<CourseInstructor> findByInstructorId(@Param("instructorId") UUID instructorId);

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.course.id = :courseId")
    List<CourseInstructor> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT SUM(ci.hoursPerWeek) FROM CourseInstructor ci WHERE ci.instructor.id = :instructorId")
    Integer getTotalHoursByInstructor(@Param("instructorId") UUID instructorId);

    @Query("SELECT SUM(ci.costPerHour * ci.hoursPerWeek) FROM CourseInstructor ci WHERE ci.course.department.id = :departmentId")
    BigDecimal getTotalWeeklyCostByDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.course.department.id = :departmentId")
    List<CourseInstructor> findByDepartmentId(@Param("departmentId") UUID departmentId);
}
