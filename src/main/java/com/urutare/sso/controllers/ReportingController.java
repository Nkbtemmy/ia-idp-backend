package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.service.ReportingService;
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
@RequestMapping("/api/v1/sso-service/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting & Analytics", description = "Comprehensive reporting and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(summary = "Cost per student report", 
               description = "Generate comprehensive cost-per-student metrics for a department")
    @GetMapping("/departments/{departmentId}/cost-per-student")
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getCostPerStudentReport(@PathVariable UUID departmentId,
                                                   @AuthenticationPrincipal UserAccount currentUser) {
        try {
            Map<String, Object> report = reportingService.generateCostPerStudentReport(departmentId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to generate cost per student report: " + e.getMessage()));
        }
    }

    @Operation(summary = "Resource utilization report", 
               description = "Generate comprehensive resource utilization analysis")
    @GetMapping("/resource-utilization")
    @PreAuthorize("hasRole('MANAGER') or hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getResourceUtilizationReport() {
        try {
            Map<String, Object> report = reportingService.generateResourceUtilizationReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to generate resource utilization report: " + e.getMessage()));
        }
    }

    @Operation(summary = "Budget variance report", 
               description = "Generate budget variance tracking report for fiscal year")
    @GetMapping("/budget-variance")
    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getBudgetVarianceReport(@RequestParam Integer fiscalYear) {
        try {
            Map<String, Object> report = reportingService.generateBudgetVarianceReport(fiscalYear);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to generate budget variance report: " + e.getMessage()));
        }
    }

    @Operation(summary = "Cross-departmental summary", 
               description = "Generate cross-departmental summary for deans (Dean/Admin only)")
    @GetMapping("/cross-departmental-summary")
    @PreAuthorize("hasRole('DEAN') or hasRole('ADMIN')")
    public ResponseEntity<?> getCrossDepartmentalSummary() {
        try {
            Map<String, Object> summary = reportingService.generateCrossDepartmentalSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to generate cross-departmental summary: " + e.getMessage()));
        }
    }
}
