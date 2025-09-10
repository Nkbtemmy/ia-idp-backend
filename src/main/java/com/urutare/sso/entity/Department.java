package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, unique = true)
    private String code; // e.g., "MATH", "ENG", "SCI"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_id")
    private UserAccount departmentHead;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal allocatedBudget = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spentBudget = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer enrollmentProjection = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentEnrollment = 0;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Course> courses = new HashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Instructor> instructors = new HashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<BudgetAllocation> budgetAllocations = new HashSet<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public BigDecimal getRemainingBudget() {
        return totalBudget.subtract(spentBudget);
    }

    public BigDecimal getUnallocatedBudget() {
        return totalBudget.subtract(allocatedBudget);
    }

    public double getBudgetUtilizationRate() {
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spentBudget.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }

    public double getEnrollmentGrowthRate() {
        if (currentEnrollment == 0) {
            return 0.0;
        }
        return ((double) (enrollmentProjection - currentEnrollment) / currentEnrollment) * 100;
    }
}
