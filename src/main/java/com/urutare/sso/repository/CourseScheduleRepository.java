package com.urutare.sso.repository;

import com.urutare.sso.entity.Classroom;
import com.urutare.sso.entity.Course;
import com.urutare.sso.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, UUID> {

    List<CourseSchedule> findByCourse(Course course);

    List<CourseSchedule> findByClassroom(Classroom classroom);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.dayOfWeek = :dayOfWeek")
    List<CourseSchedule> findByDayOfWeek(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.classroom.id = :classroomId AND cs.dayOfWeek = :dayOfWeek")
    List<CourseSchedule> findByClassroomAndDay(@Param("classroomId") UUID classroomId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.classroom.id = :classroomId AND cs.dayOfWeek = :dayOfWeek AND " +
           "((cs.startTime <= :endTime AND cs.endTime >= :startTime))")
    List<CourseSchedule> findConflictingSchedules(@Param("classroomId") UUID classroomId, 
                                                 @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                 @Param("startTime") LocalTime startTime, 
                                                 @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(cs) FROM CourseSchedule cs WHERE cs.classroom.id = :classroomId")
    Long countSchedulesByClassroom(@Param("classroomId") UUID classroomId);

    @Query("SELECT cs.dayOfWeek, COUNT(cs) FROM CourseSchedule cs GROUP BY cs.dayOfWeek ORDER BY cs.dayOfWeek")
    List<Object[]> getScheduleCountByDay();
}
