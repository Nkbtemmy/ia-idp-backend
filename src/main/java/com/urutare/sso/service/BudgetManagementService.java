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
@Transactional
public class BudgetManagementService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final ClassroomRepository classroomRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    // Core Budget Calculation Algorithms

    /**
     * Calculate optimal resource distribution based on enrollment projections
     */
    public Map<String, Object> calculateOptimalResourceDistribution(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Course> courses = courseRepository.findByDepartmentId(departmentId);
        List<Instructor> instructors = instructorRepository.findByDepartmentId(departmentId);

        Map<String, Object> distribution = new HashMap<>();
        
        // Calculate total projected enrollment
        int totalProjectedEnrollment = courses.stream()
            .mapToInt(Course::getProjectedEnrollment)
            .sum();

        // Calculate cost per student across all courses
        BigDecimal totalCourseCosts = courses.stream()
            .map(Course::calculateTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costPerStudent = totalProjectedEnrollment > 0 
            ? totalCourseCosts.divide(new BigDecimal(totalProjectedEnrollment), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Calculate instructor utilization and costs
        BigDecimal totalInstructorCosts = instructors.stream()
            .map(Instructor::calculateAnnualCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Optimal class size calculation based on cost efficiency
        Map<UUID, Integer> optimalClassSizes = calculateOptimalClassSizes(courses);

        distribution.put("totalProjectedEnrollment", totalProjectedEnrollment);
        distribution.put("costPerStudent", costPerStudent);
        distribution.put("totalInstructorCosts", totalInstructorCosts);
        distribution.put("optimalClassSizes", optimalClassSizes);
        distribution.put("resourceEfficiencyScore", calculateResourceEfficiencyScore(department));

        return distribution;
    }

    /**
     * Calculate cost-per-credit-hour with advanced algorithms
     */
    public BigDecimal calculateAdvancedCostPerCreditHour(UUID courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        // Base cost calculation
        BigDecimal baseCost = course.calculateCostPerCreditHour();

        // Factor in instructor costs
        List<CourseInstructor> courseInstructors = courseInstructorRepository.findByCourseId(courseId);
        BigDecimal instructorCosts = courseInstructors.stream()
            .map(ci -> ci.getCostPerHour().multiply(new BigDecimal(ci.getHoursPerWeek() * 36))) // 36 weeks
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Factor in classroom utilization costs
        BigDecimal classroomCosts = calculateClassroomCosts(course);

        // Apply efficiency multipliers based on enrollment
        BigDecimal efficiencyMultiplier = calculateEnrollmentEfficiencyMultiplier(course);

        BigDecimal totalCost = baseCost.add(instructorCosts).add(classroomCosts);
        return totalCost.multiply(efficiencyMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate facility utilization rates with optimization suggestions
     */
    public Map<String, Object> calculateFacilityUtilization() {
        List<Classroom> classrooms = classroomRepository.findAll();
        Map<String, Object> utilization = new HashMap<>();

        for (Classroom classroom : classrooms) {
            Map<String, Object> classroomData = new HashMap<>();
            
            // Calculate weekly usage hours
            int weeklyUsageHours = classroom.getSchedules().stream()
                .mapToInt(schedule -> schedule.getDurationMinutes() / 60)
                .sum();

            double utilizationRate = classroom.getUtilizationRate(weeklyUsageHours);
            BigDecimal weeklyCost = classroom.calculateDailyCost(weeklyUsageHours / 5).multiply(new BigDecimal(5));
            
            classroomData.put("utilizationRate", utilizationRate);
            classroomData.put("weeklyCost", weeklyCost);
            classroomData.put("weeklyUsageHours", weeklyUsageHours);
            classroomData.put("capacity", classroom.getCapacity());
            classroomData.put("costPerHour", classroom.calculateHourlyCost());
            
            // Optimization suggestions
            List<String> suggestions = generateOptimizationSuggestions(classroom, utilizationRate);
            classroomData.put("optimizationSuggestions", suggestions);

            utilization.put(classroom.getFullRoomIdentifier(), classroomData);
        }

        return utilization;
    }

    /**
     * Advanced budget variance tracking with predictive analysis
     */
    public Map<String, Object> calculateBudgetVarianceAnalysis(UUID departmentId, Integer fiscalYear) {
        List<BudgetAllocation> allocations = budgetAllocationRepository
            .findByDepartmentAndFiscalYear(departmentId, fiscalYear);

        Map<String, Object> analysis = new HashMap<>();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        Map<BudgetAllocation.AllocationCategory, BigDecimal> categoryVariances = new HashMap<>();

        for (BudgetAllocation allocation : allocations) {
            totalAllocated = totalAllocated.add(allocation.getAllocatedAmount());
            totalSpent = totalSpent.add(allocation.getSpentAmount());

            BigDecimal variance = allocation.getSpentAmount().subtract(allocation.getAllocatedAmount());
            categoryVariances.merge(allocation.getCategory(), variance, BigDecimal::add);
        }

        BigDecimal overallVariance = totalSpent.subtract(totalAllocated);
        double variancePercentage = totalAllocated.compareTo(BigDecimal.ZERO) != 0 
            ? overallVariance.divide(totalAllocated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
            : 0.0;

        analysis.put("totalAllocated", totalAllocated);
        analysis.put("totalSpent", totalSpent);
        analysis.put("overallVariance", overallVariance);
        analysis.put("variancePercentage", variancePercentage);
        analysis.put("categoryVariances", categoryVariances);
        analysis.put("projectedYearEndSpending", projectYearEndSpending(allocations));
        analysis.put("riskAssessment", assessBudgetRisk(variancePercentage));

        return analysis;
    }

    // Helper Methods for Advanced Calculations

    private Map<UUID, Integer> calculateOptimalClassSizes(List<Course> courses) {
        Map<UUID, Integer> optimalSizes = new HashMap<>();
        
        for (Course course : courses) {
            // Optimal class size based on cost efficiency curve
            int currentEnrollment = course.getCurrentEnrollment();
            int maxStudents = course.getMaxStudents();
            BigDecimal costPerStudent = course.getCostPerStudent();

            // Sweet spot is typically 75-85% of maximum capacity for cost efficiency
            int optimalSize = (int) (maxStudents * 0.8);
            
            // Adjust based on course type
            switch (course.getCourseType()) {
                case ADVANCED, HONORS, AP -> optimalSize = Math.min(optimalSize, 25);
                case REMEDIAL -> optimalSize = Math.min(optimalSize, 15);
                default -> optimalSize = Math.min(optimalSize, 30);
            }

            optimalSizes.put(course.getId(), optimalSize);
        }

        return optimalSizes;
    }

    private BigDecimal calculateResourceEfficiencyScore(Department department) {
        // Complex algorithm combining multiple efficiency metrics
        double budgetUtilization = department.getBudgetUtilizationRate();
        double enrollmentGrowth = department.getEnrollmentGrowthRate();
        
        List<Course> courses = courseRepository.findByDepartmentId(department.getId());
        double avgClassUtilization = courses.stream()
            .mapToDouble(Course::getEnrollmentUtilizationRate)
            .average()
            .orElse(0.0);

        List<Instructor> instructors = instructorRepository.findByDepartmentId(department.getId());
        double avgInstructorUtilization = instructors.stream()
            .mapToDouble(Instructor::getWorkloadUtilization)
            .average()
            .orElse(0.0);

        // Weighted efficiency score (0-100)
        double efficiencyScore = (budgetUtilization * 0.3) + 
                                (avgClassUtilization * 0.3) + 
                                (avgInstructorUtilization * 0.25) + 
                                (Math.max(0, enrollmentGrowth) * 0.15);

        return new BigDecimal(Math.min(100, efficiencyScore)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateClassroomCosts(Course course) {
        return course.getSchedules().stream()
            .map(schedule -> {
                Classroom classroom = schedule.getClassroom();
                int durationHours = schedule.getDurationMinutes() / 60;
                return classroom.calculateHourlyCost().multiply(new BigDecimal(durationHours * 36)); // 36 weeks
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateEnrollmentEfficiencyMultiplier(Course course) {
        double utilizationRate = course.getEnrollmentUtilizationRate();
        
        // Efficiency curve: penalty for under/over enrollment
        if (utilizationRate < 50) {
            return new BigDecimal("1.3"); // 30% penalty for underutilization
        } else if (utilizationRate > 100) {
            return new BigDecimal("1.2"); // 20% penalty for overcrowding
        } else if (utilizationRate >= 75 && utilizationRate <= 90) {
            return new BigDecimal("0.9"); // 10% bonus for optimal range
        }
        
        return BigDecimal.ONE; // No adjustment
    }

    private List<String> generateOptimizationSuggestions(Classroom classroom, double utilizationRate) {
        List<String> suggestions = new ArrayList<>();

        if (utilizationRate < 40) {
            suggestions.add("Consider consolidating classes or reducing maintenance frequency");
            suggestions.add("Evaluate potential for alternative use or rental");
        } else if (utilizationRate > 90) {
            suggestions.add("High utilization - consider expanding capacity or adding sessions");
            suggestions.add("Monitor for overcrowding and student satisfaction");
        }

        if (classroom.calculateHourlyCost().compareTo(new BigDecimal("50")) > 0) {
            suggestions.add("High operational costs - review utility efficiency");
        }

        return suggestions;
    }

    private BigDecimal projectYearEndSpending(List<BudgetAllocation> allocations) {
        // Simple linear projection based on current spending rate
        // In a real system, this would use more sophisticated forecasting
        return allocations.stream()
            .map(allocation -> {
                double utilizationRate = allocation.getUtilizationRate() / 100.0;
                if (utilizationRate > 0) {
                    // Project based on current burn rate
                    return allocation.getAllocatedAmount().multiply(new BigDecimal(Math.min(1.2, utilizationRate * 1.1)));
                }
                return allocation.getAllocatedAmount();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String assessBudgetRisk(double variancePercentage) {
        if (Math.abs(variancePercentage) <= 5) {
            return "LOW - Budget variance within acceptable range";
        } else if (Math.abs(variancePercentage) <= 15) {
            return "MEDIUM - Monitor spending closely";
        } else {
            return "HIGH - Immediate budget review required";
        }
    }
}
