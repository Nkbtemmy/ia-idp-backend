package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "instructors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InstructorType instructorType = InstructorType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QualificationLevel qualification = QualificationLevel.BACHELOR;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal annualSalary = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxTeachingHours = 40;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentTeachingHours = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer yearsOfExperience = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CourseInstructor> courseAssignments = new HashSet<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public BigDecimal calculateMonthlyCost() {
        if (instructorType == InstructorType.FULL_TIME) {
            return annualSalary.divide(new BigDecimal(12), 2, java.math.RoundingMode.HALF_UP);
        } else {
            return hourlyRate.multiply(new BigDecimal(currentTeachingHours * 4)); // 4 weeks per month
        }
    }

    public BigDecimal calculateAnnualCost() {
        if (instructorType == InstructorType.FULL_TIME) {
            return annualSalary;
        } else {
            return hourlyRate.multiply(new BigDecimal(currentTeachingHours * 52)); // 52 weeks per year
        }
    }

    public Integer getAvailableHours() {
        return Math.max(0, maxTeachingHours - currentTeachingHours);
    }

    public double getWorkloadUtilization() {
        if (maxTeachingHours == 0) {
            return 0.0;
        }
        return ((double) currentTeachingHours / maxTeachingHours) * 100;
    }

    public boolean canTakeAdditionalHours(int hours) {
        return (currentTeachingHours + hours) <= maxTeachingHours && isAvailable;
    }

    public BigDecimal getQualificationMultiplier() {
        return switch (qualification) {
            case BACHELOR -> new BigDecimal("1.0");
            case MASTER -> new BigDecimal("1.15");
            case DOCTORATE -> new BigDecimal("1.30");
            case PROFESSIONAL -> new BigDecimal("1.10");
        };
    }

    public enum InstructorType {
        FULL_TIME, PART_TIME, ADJUNCT, SUBSTITUTE
    }

    public enum QualificationLevel {
        BACHELOR, MASTER, DOCTORATE, PROFESSIONAL
    }
}
