package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "classrooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String roomNumber;

    @Column(nullable = false)
    private String building;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClassroomType type = ClassroomType.STANDARD;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maintenanceCostPerHour = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal utilityCostPerHour = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasProjector = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasComputers = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasLab = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @OneToMany(mappedBy = "classroom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CourseSchedule> schedules = new HashSet<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods
    public String getFullRoomIdentifier() {
        return building + "-" + roomNumber;
    }

    public BigDecimal calculateHourlyCost() {
        return maintenanceCostPerHour.add(utilityCostPerHour);
    }

    public BigDecimal calculateDailyCost(int hoursUsed) {
        return calculateHourlyCost().multiply(new BigDecimal(hoursUsed));
    }

    public double getUtilizationRate(int hoursUsedPerWeek) {
        int maxHoursPerWeek = 8 * 5; // 8 hours per day, 5 days per week
        if (maxHoursPerWeek == 0) {
            return 0.0;
        }
        return ((double) hoursUsedPerWeek / maxHoursPerWeek) * 100;
    }

    public boolean isCompatibleWith(Course course) {
        return switch (course.getCourseType()) {
            case REGULAR, HONORS, AP -> type != ClassroomType.SPECIALIZED_LAB;
            case ADVANCED -> hasProjector || hasComputers;
            case REMEDIAL, ELECTIVE -> true;
        };
    }

    public enum ClassroomType {
        STANDARD, COMPUTER_LAB, SCIENCE_LAB, SPECIALIZED_LAB, AUDITORIUM, GYMNASIUM
    }
}
