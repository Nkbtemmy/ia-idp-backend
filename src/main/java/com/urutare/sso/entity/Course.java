package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String courseCode; // e.g., "MATH101", "ENG201"

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    @Builder.Default
    private Integer creditHours = 3;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxStudents = 30;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentEnrollment = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer projectedEnrollment = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPerStudent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal equipmentCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal materialsCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseType courseType = CourseType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Semester semester = Semester.FALL;

    @Column(nullable = false)
    @Builder.Default
    private Integer academicYear = 2024;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CourseSchedule> schedules = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CourseInstructor> courseInstructors = new HashSet<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public BigDecimal calculateTotalCost() {
        BigDecimal studentCosts = costPerStudent.multiply(new BigDecimal(projectedEnrollment));
        return studentCosts.add(equipmentCost).add(materialsCost);
    }

    public BigDecimal calculateCostPerCreditHour() {
        BigDecimal totalCost = calculateTotalCost();
        if (projectedEnrollment == 0 || creditHours == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(
            new BigDecimal(projectedEnrollment * creditHours), 
            2, 
            java.math.RoundingMode.HALF_UP
        );
    }

    public double getEnrollmentUtilizationRate() {
        if (maxStudents == 0) {
            return 0.0;
        }
        return ((double) currentEnrollment / maxStudents) * 100;
    }

    public boolean isOverEnrolled() {
        return currentEnrollment > maxStudents;
    }

    public Integer getAvailableSeats() {
        return Math.max(0, maxStudents - currentEnrollment);
    }

    public enum CourseType {
        REGULAR, ADVANCED, HONORS, AP, REMEDIAL, ELECTIVE
    }

    public enum Semester {
        FALL, SPRING, SUMMER
    }
}
