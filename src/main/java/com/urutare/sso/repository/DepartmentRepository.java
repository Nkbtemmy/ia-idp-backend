package com.urutare.sso.repository;

import com.urutare.sso.entity.Department;
import com.urutare.sso.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    List<Department> findByDepartmentHead(UserAccount departmentHead);

    @Query("SELECT d FROM Department d WHERE d.totalBudget > :minBudget")
    List<Department> findDepartmentsWithBudgetGreaterThan(@Param("minBudget") BigDecimal minBudget);

    @Query("SELECT d FROM Department d WHERE d.spentBudget > d.totalBudget")
    List<Department> findOverBudgetDepartments();

    @Query("SELECT d FROM Department d WHERE (d.spentBudget / d.totalBudget) > :utilizationRate")
    List<Department> findDepartmentsByBudgetUtilization(@Param("utilizationRate") Double utilizationRate);

    @Query("SELECT d FROM Department d WHERE d.enrollmentProjection > d.currentEnrollment")
    List<Department> findDepartmentsWithGrowthProjection();

    @Query("SELECT SUM(d.totalBudget) FROM Department d")
    BigDecimal getTotalInstitutionBudget();

    @Query("SELECT SUM(d.spentBudget) FROM Department d")
    BigDecimal getTotalInstitutionSpending();

    @Query("SELECT d FROM Department d ORDER BY d.totalBudget DESC")
    List<Department> findAllOrderByBudgetDesc();

    @Query("SELECT d FROM Department d WHERE d.departmentHead.id = :headId")
    List<Department> findByDepartmentHeadId(@Param("headId") UUID headId);
}
