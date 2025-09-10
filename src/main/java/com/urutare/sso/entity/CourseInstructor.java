package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_instructors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CourseInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(nullable = false)
    @Builder.Default
    private Integer hoursPerWeek = 3;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPerHour = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.PRIMARY;

    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public BigDecimal calculateWeeklyCost() {
        return costPerHour.multiply(new BigDecimal(hoursPerWeek));
    }

    public BigDecimal calculateSemesterCost(int weeksInSemester) {
        return calculateWeeklyCost().multiply(new BigDecimal(weeksInSemester));
    }

    public BigDecimal calculateAnnualCost() {
        return calculateWeeklyCost().multiply(new BigDecimal(36)); // 36 weeks academic year
    }

    public enum AssignmentType {
        PRIMARY, ASSISTANT, SUBSTITUTE, CO_TEACHER
    }
}
