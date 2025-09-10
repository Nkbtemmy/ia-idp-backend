package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.entity.Course;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.service.CourseService;
import com.urutare.sso.service.BudgetManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sso-service/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Course scheduling and cost management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;
    private final BudgetManagementService budgetManagementService;

    @Operation(summary = "Get courses by department", description = "Retrieve courses for a specific department")
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<?> getCoursesByDepartment(@PathVariable UUID departmentId,
                                                   @AuthenticationPrincipal UserAccount currentUser) {
        try {
            List<Course> courses = courseService.getCoursesByDepartment(departmentId, currentUser);
            return ResponseEntity.ok(courses);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                .body(new ApiResponse<>(false, "Access denied: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve courses: " + e.getMessage()));
        }
    }

    @Operation(summary = "Create new course", description = "Create a new course (Department Head/Dean/Admin only)")
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course,
                                        @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Course created = courseService.createCourse(course, currentUser);
            return ResponseEntity.ok(created);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                .body(new ApiResponse<>(false, "Access denied: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to create course: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update course", description = "Update course information")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable UUID id,
                                        @RequestBody Course course,
                                        @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Course updated = courseService.updateCourse(id, course, currentUser);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                .body(new ApiResponse<>(false, "Access denied: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to update course: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get over-enrolled courses", description = "Retrieve courses exceeding capacity")
    @GetMapping("/over-enrolled")
    public ResponseEntity<?> getOverEnrolledCourses() {
        try {
            List<Course> courses = courseService.getOverEnrolledCourses();
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve over-enrolled courses: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get under-enrolled courses", description = "Retrieve courses with low enrollment")
    @GetMapping("/under-enrolled")
    public ResponseEntity<?> getUnderEnrolledCourses() {
        try {
            List<Course> courses = courseService.getUnderEnrolledCourses();
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve under-enrolled courses: " + e.getMessage()));
        }
    }

    @Operation(summary = "Calculate cost per credit hour", description = "Get advanced cost calculation for a course")
    @GetMapping("/{id}/cost-per-credit-hour")
    public ResponseEntity<?> getCostPerCreditHour(@PathVariable UUID id) {
        try {
            BigDecimal cost = budgetManagementService.calculateAdvancedCostPerCreditHour(id);
            return ResponseEntity.ok(Map.of("courseId", id, "costPerCreditHour", cost));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate cost per credit hour: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get department projected costs", description = "Calculate total projected costs for department")
    @GetMapping("/department/{departmentId}/projected-costs")
    public ResponseEntity<?> getDepartmentProjectedCosts(@PathVariable UUID departmentId) {
        try {
            BigDecimal projectedCosts = courseService.calculateDepartmentProjectedCosts(departmentId);
            return ResponseEntity.ok(Map.of("departmentId", departmentId, "projectedCosts", projectedCosts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate projected costs: " + e.getMessage()));
        }
    }
}
