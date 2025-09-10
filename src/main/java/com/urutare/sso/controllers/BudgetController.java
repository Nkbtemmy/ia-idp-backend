package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.service.BudgetManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sso-service/budget")
@RequiredArgsConstructor
@Tag(name = "Budget Management", description = "Advanced budget calculation and optimization endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetManagementService budgetManagementService;

    @Operation(summary = "Calculate optimal resource distribution", 
               description = "Calculate optimal resource allocation based on enrollment projections")
    @GetMapping("/departments/{departmentId}/optimal-distribution")
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getOptimalResourceDistribution(@PathVariable UUID departmentId,
                                                          @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Map<String, Object> distribution = budgetManagementService.calculateOptimalResourceDistribution(departmentId);
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate optimal distribution: " + e.getMessage()));
        }
    }

    @Operation(summary = "Calculate facility utilization", 
               description = "Get comprehensive facility utilization analysis with optimization suggestions")
    @GetMapping("/facility-utilization")
    @PreAuthorize("hasRole('MANAGER') or hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getFacilityUtilization() {
        try {
            Map<String, Object> utilization = budgetManagementService.calculateFacilityUtilization();
            return ResponseEntity.ok(utilization);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate facility utilization: " + e.getMessage()));
        }
    }

    @Operation(summary = "Budget variance analysis", 
               description = "Advanced budget variance tracking with predictive analysis")
    @GetMapping("/departments/{departmentId}/variance-analysis")
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getBudgetVarianceAnalysis(@PathVariable UUID departmentId,
                                                     @RequestParam Integer fiscalYear,
                                                     @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Map<String, Object> analysis = budgetManagementService.calculateBudgetVarianceAnalysis(departmentId, fiscalYear);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to calculate variance analysis: " + e.getMessage()));
        }
    }
}
