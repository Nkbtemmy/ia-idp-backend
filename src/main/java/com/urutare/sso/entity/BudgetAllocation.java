package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_allocations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class BudgetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationCategory category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer fiscalYear = 2024;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserAccount approvedBy;

    private Instant approvedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount);
    }

    public double getUtilizationRate() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spentAmount.divide(allocatedAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }

    public boolean isOverBudget() {
        return spentAmount.compareTo(allocatedAmount) > 0;
    }

    public BigDecimal getOverageAmount() {
        if (isOverBudget()) {
            return spentAmount.subtract(allocatedAmount);
        }
        return BigDecimal.ZERO;
    }

    public void approve(UserAccount approver) {
        this.status = AllocationStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = Instant.now();
    }

    public enum AllocationCategory {
        SALARIES_AND_BENEFITS,
        INSTRUCTIONAL_MATERIALS,
        EQUIPMENT_AND_TECHNOLOGY,
        FACILITIES_AND_MAINTENANCE,
        PROFESSIONAL_DEVELOPMENT,
        STUDENT_ACTIVITIES,
        ADMINISTRATIVE_COSTS,
        UTILITIES,
        TRANSPORTATION,
        OTHER
    }

    public enum AllocationStatus {
        PENDING, APPROVED, REJECTED, TRANSFERRED, EXPIRED
    }
}
