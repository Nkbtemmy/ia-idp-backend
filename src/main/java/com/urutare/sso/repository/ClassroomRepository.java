package com.urutare.sso.repository;

import com.urutare.sso.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

    Optional<Classroom> findByRoomNumber(String roomNumber);

    List<Classroom> findByBuilding(String building);

    @Query("SELECT c FROM Classroom c WHERE c.type = :type")
    List<Classroom> findByType(@Param("type") Classroom.ClassroomType type);

    @Query("SELECT c FROM Classroom c WHERE c.capacity >= :minCapacity")
    List<Classroom> findByMinCapacity(@Param("minCapacity") Integer minCapacity);

    @Query("SELECT c FROM Classroom c WHERE c.isAvailable = true")
    List<Classroom> findAvailableClassrooms();

    @Query("SELECT c FROM Classroom c WHERE c.hasProjector = true")
    List<Classroom> findClassroomsWithProjector();

    @Query("SELECT c FROM Classroom c WHERE c.hasComputers = true")
    List<Classroom> findClassroomsWithComputers();

    @Query("SELECT c FROM Classroom c WHERE c.hasLab = true")
    List<Classroom> findLabClassrooms();

    @Query("SELECT SUM(c.maintenanceCostPerHour + c.utilityCostPerHour) FROM Classroom c")
    BigDecimal getTotalHourlyCosts();

    @Query("SELECT c FROM Classroom c ORDER BY c.capacity DESC")
    List<Classroom> findAllOrderByCapacityDesc();

    @Query("SELECT c.building, COUNT(c) FROM Classroom c GROUP BY c.building")
    List<Object[]> getClassroomCountByBuilding();
}
