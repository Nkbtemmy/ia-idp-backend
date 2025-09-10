package com.urutare.sso.repository;

import com.urutare.sso.entity.BudgetAllocation;
import com.urutare.sso.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, UUID> {

    List<BudgetAllocation> findByDepartment(Department department);

    List<BudgetAllocation> findByDepartmentId(UUID departmentId);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.status = :status")
    List<BudgetAllocation> findByStatus(@Param("status") BudgetAllocation.AllocationStatus status);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.category = :category")
    List<BudgetAllocation> findByCategory(@Param("category") BudgetAllocation.AllocationCategory category);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.fiscalYear = :year")
    List<BudgetAllocation> findByFiscalYear(@Param("year") Integer year);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.department.id = :departmentId AND ba.fiscalYear = :year")
    List<BudgetAllocation> findByDepartmentAndFiscalYear(@Param("departmentId") UUID departmentId, @Param("year") Integer year);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.spentAmount > ba.allocatedAmount")
    List<BudgetAllocation> findOverBudgetAllocations();

    @Query("SELECT SUM(ba.allocatedAmount) FROM BudgetAllocation ba WHERE ba.department.id = :departmentId AND ba.fiscalYear = :year")
    BigDecimal getTotalAllocatedByDepartmentAndYear(@Param("departmentId") UUID departmentId, @Param("year") Integer year);

    @Query("SELECT SUM(ba.spentAmount) FROM BudgetAllocation ba WHERE ba.department.id = :departmentId AND ba.fiscalYear = :year")
    BigDecimal getTotalSpentByDepartmentAndYear(@Param("departmentId") UUID departmentId, @Param("year") Integer year);

    @Query("SELECT ba.category, SUM(ba.allocatedAmount) FROM BudgetAllocation ba WHERE ba.fiscalYear = :year GROUP BY ba.category")
    List<Object[]> getAllocationSummaryByCategory(@Param("year") Integer year);

    @Query("SELECT ba FROM BudgetAllocation ba WHERE ba.department.id = :departmentId AND ba.category = :category AND ba.fiscalYear = :year")
    List<BudgetAllocation> findByDepartmentCategoryAndYear(@Param("departmentId") UUID departmentId, @Param("category") BudgetAllocation.AllocationCategory category, @Param("year") Integer year);
}
