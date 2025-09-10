package com.urutare.sso.service;

import com.urutare.sso.entity.*;
import com.urutare.sso.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final ClassroomRepository classroomRepository;
    private final BudgetManagementService budgetManagementService;

    /**
     * Generate comprehensive cost-per-student metrics
     */
    public Map<String, Object> generateCostPerStudentReport(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Course> courses = courseRepository.findByDepartmentId(departmentId);
        Map<String, Object> report = new HashMap<>();

        // Overall department metrics
        int totalEnrollment = courses.stream().mapToInt(Course::getCurrentEnrollment).sum();
        int projectedEnrollment = courses.stream().mapToInt(Course::getProjectedEnrollment).sum();
        BigDecimal totalDepartmentCosts = department.getSpentBudget();

        BigDecimal costPerCurrentStudent = totalEnrollment > 0 
            ? totalDepartmentCosts.divide(new BigDecimal(totalEnrollment), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal costPerProjectedStudent = projectedEnrollment > 0 
            ? totalDepartmentCosts.divide(new BigDecimal(projectedEnrollment), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Course-level breakdown
        List<Map<String, Object>> courseBreakdown = courses.stream()
            .map(course -> {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("courseCode", course.getCourseCode());
                courseData.put("courseName", course.getName());
                courseData.put("currentEnrollment", course.getCurrentEnrollment());
                courseData.put("projectedEnrollment", course.getProjectedEnrollment());
                courseData.put("costPerCreditHour", budgetManagementService.calculateAdvancedCostPerCreditHour(course.getId()));
                courseData.put("totalCourseCost", course.calculateTotalCost());
                courseData.put("utilizationRate", course.getEnrollmentUtilizationRate());
                return courseData;
            })
            .collect(Collectors.toList());

        report.put("departmentName", department.getName());
        report.put("totalCurrentEnrollment", totalEnrollment);
        report.put("totalProjectedEnrollment", projectedEnrollment);
        report.put("costPerCurrentStudent", costPerCurrentStudent);
        report.put("costPerProjectedStudent", costPerProjectedStudent);
        report.put("courseBreakdown", courseBreakdown);
        report.put("enrollmentGrowthRate", department.getEnrollmentGrowthRate());

        return report;
    }

    /**
     * Generate resource utilization analysis
     */
    public Map<String, Object> generateResourceUtilizationReport() {
        Map<String, Object> report = new HashMap<>();

        // Facility utilization
        Map<String, Object> facilityUtilization = budgetManagementService.calculateFacilityUtilization();
        
        // Instructor utilization
        List<Instructor> instructors = instructorRepository.findAll();
        Map<String, Object> instructorUtilization = new HashMap<>();
        
        double avgInstructorUtilization = instructors.stream()
            .mapToDouble(Instructor::getWorkloadUtilization)
            .average()
            .orElse(0.0);

        List<Instructor> overloaded = instructorRepository.findOverloadedInstructors();
        List<Instructor> underutilized = instructorRepository.findUnderutilizedInstructors();

        instructorUtilization.put("averageUtilization", avgInstructorUtilization);
        instructorUtilization.put("overloadedCount", overloaded.size());
        instructorUtilization.put("underutilizedCount", underutilized.size());
        instructorUtilization.put("totalInstructors", instructors.size());

        // Department efficiency scores
        List<Department> departments = departmentRepository.findAll();
        Map<String, BigDecimal> departmentEfficiency = departments.stream()
            .collect(Collectors.toMap(
                Department::getName,
                dept -> budgetManagementService.calculateOptimalResourceDistribution(dept.getId())
                    .get("resourceEfficiencyScore") instanceof BigDecimal 
                    ? (BigDecimal) budgetManagementService.calculateOptimalResourceDistribution(dept.getId()).get("resourceEfficiencyScore")
                    : BigDecimal.ZERO
            ));

        report.put("facilityUtilization", facilityUtilization);
        report.put("instructorUtilization", instructorUtilization);
        report.put("departmentEfficiencyScores", departmentEfficiency);
        report.put("generatedAt", new Date());

        return report;
    }

    /**
     * Generate budget variance tracking report
     */
    public Map<String, Object> generateBudgetVarianceReport(Integer fiscalYear) {
        Map<String, Object> report = new HashMap<>();
        List<Department> departments = departmentRepository.findAll();

        Map<String, Object> departmentVariances = new HashMap<>();
        BigDecimal totalInstitutionVariance = BigDecimal.ZERO;

        for (Department department : departments) {
            Map<String, Object> deptAnalysis = budgetManagementService
                .calculateBudgetVarianceAnalysis(department.getId(), fiscalYear);
            departmentVariances.put(department.getName(), deptAnalysis);
            
            BigDecimal deptVariance = (BigDecimal) deptAnalysis.get("overallVariance");
            totalInstitutionVariance = totalInstitutionVariance.add(deptVariance);
        }

        // Institution-wide metrics
        BigDecimal totalBudget = departmentRepository.getTotalInstitutionBudget();
        BigDecimal totalSpent = departmentRepository.getTotalInstitutionSpending();
        double institutionVariancePercentage = totalBudget.compareTo(BigDecimal.ZERO) != 0
            ? totalInstitutionVariance.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
            : 0.0;

        // Category-wise analysis
        List<Object[]> categoryData = budgetAllocationRepository.getAllocationSummaryByCategory(fiscalYear);
        Map<String, BigDecimal> categoryAllocations = categoryData.stream()
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> (BigDecimal) row[1]
            ));

        report.put("fiscalYear", fiscalYear);
        report.put("departmentVariances", departmentVariances);
        report.put("totalInstitutionBudget", totalBudget);
        report.put("totalInstitutionSpent", totalSpent);
        report.put("institutionVariancePercentage", institutionVariancePercentage);
        report.put("categoryAllocations", categoryAllocations);
        report.put("riskLevel", assessInstitutionRisk(institutionVariancePercentage));

        return report;
    }

    /**
     * Generate cross-departmental summary for deans
     */
    public Map<String, Object> generateCrossDepartmentalSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<Department> departments = departmentRepository.findAll();

        List<Map<String, Object>> departmentSummaries = departments.stream()
            .map(dept -> {
                Map<String, Object> deptSummary = new HashMap<>();
                deptSummary.put("name", dept.getName());
                deptSummary.put("code", dept.getCode());
                deptSummary.put("totalBudget", dept.getTotalBudget());
                deptSummary.put("spentBudget", dept.getSpentBudget());
                deptSummary.put("utilizationRate", dept.getBudgetUtilizationRate());
                deptSummary.put("currentEnrollment", dept.getCurrentEnrollment());
                deptSummary.put("projectedEnrollment", dept.getEnrollmentProjection());
                deptSummary.put("enrollmentGrowth", dept.getEnrollmentGrowthRate());
                
                // Course and instructor counts
                List<Course> courses = courseRepository.findByDepartmentId(dept.getId());
                List<Instructor> instructors = instructorRepository.findByDepartmentId(dept.getId());
                deptSummary.put("courseCount", courses.size());
                deptSummary.put("instructorCount", instructors.size());
                
                return deptSummary;
            })
            .collect(Collectors.toList());

        // Institution totals
        BigDecimal totalBudget = departmentRepository.getTotalInstitutionBudget();
        BigDecimal totalSpent = departmentRepository.getTotalInstitutionSpending();
        int totalEnrollment = departments.stream().mapToInt(Department::getCurrentEnrollment).sum();
        int totalProjectedEnrollment = departments.stream().mapToInt(Department::getEnrollmentProjection).sum();

        summary.put("departments", departmentSummaries);
        summary.put("institutionTotals", Map.of(
            "totalBudget", totalBudget,
            "totalSpent", totalSpent,
            "totalEnrollment", totalEnrollment,
            "totalProjectedEnrollment", totalProjectedEnrollment,
            "overallUtilizationRate", totalBudget.compareTo(BigDecimal.ZERO) != 0 
                ? totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
                : 0.0
        ));

        return summary;
    }

    private String assessInstitutionRisk(double variancePercentage) {
        if (Math.abs(variancePercentage) <= 3) {
            return "LOW - Institution-wide budget on track";
        } else if (Math.abs(variancePercentage) <= 10) {
            return "MEDIUM - Some departments require attention";
        } else {
            return "HIGH - Institution-wide budget review needed";
        }
    }
}
