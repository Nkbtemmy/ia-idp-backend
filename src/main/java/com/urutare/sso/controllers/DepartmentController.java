package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.service.DepartmentService;
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
@RequestMapping("/api/v1/sso-service/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "Department budget and administration endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Get all departments", description = "Retrieve all departments (Dean/Admin only)")
    @GetMapping
    public ResponseEntity<?> getAllDepartments(@AuthenticationPrincipal UserAccount currentUser) {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve departments: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get department by ID", description = "Retrieve specific department details")
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable UUID id, 
                                             @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Department department = departmentService.getDepartmentById(id, currentUser);
            return ResponseEntity.ok(department);
        } catch (SecurityException e) {
            return ResponseEntity.status(403)
                .body(new ApiResponse<>(false, "Access denied: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve department: " + e.getMessage()));
        }
    }

    @Operation(summary = "Create new department", description = "Create a new department (Dean/Admin only)")
    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department,
                                            @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Department created = departmentService.createDepartment(department);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to create department: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update department", description = "Update department information (Dean/Admin only)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable UUID id,
                                            @RequestBody Department department,
                                            @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Department updated = departmentService.updateDepartment(id, department, currentUser);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to update department: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get user's departments", description = "Get departments accessible to current user")
    @GetMapping("/my-departments")
    public ResponseEntity<?> getMyDepartments(@AuthenticationPrincipal UserAccount currentUser) {
        try {
            List<Department> departments = departmentService.getDepartmentsByUser(currentUser);
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve departments: " + e.getMessage()));
        }
    }

    @Operation(summary = "Transfer budget", description = "Transfer budget between departments (Dean/Admin only)")
    @PostMapping("/transfer-budget")
    public ResponseEntity<?> transferBudget(@RequestParam UUID fromDepartmentId,
                                          @RequestParam UUID toDepartmentId,
                                          @RequestParam BigDecimal amount,
                                          @AuthenticationPrincipal UserAccount currentUser) {
        try {
            departmentService.transferBudget(fromDepartmentId, toDepartmentId, amount, currentUser);
            return ResponseEntity.ok(new ApiResponse<>(true, "Budget transfer completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Budget transfer failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get over-budget departments", description = "Retrieve departments exceeding their budget")
    @GetMapping("/over-budget")
    public ResponseEntity<?> getOverBudgetDepartments() {
        try {
            List<Department> departments = departmentService.getOverBudgetDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to retrieve over-budget departments: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get total institution budget", description = "Get total budget across all departments")
    @GetMapping("/total-budget")
    public ResponseEntity<?> getTotalInstitutionBudget() {
        try {
            BigDecimal totalBudget = departmentService.getTotalInstitutionBudget();
            return ResponseEntity.ok(Map.of("totalBudget", totalBudget));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate total budget: " + e.getMessage()));
        }
    }
}
