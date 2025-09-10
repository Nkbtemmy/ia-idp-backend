package com.urutare.sso.service;

import com.urutare.sso.entity.*;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BudgetDataSeederService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final ClassroomRepository classroomRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 1) { // Skip if data already exists
            return;
        }

        seedUsers();
        seedClassrooms();
        seedDepartments();
        seedInstructors();
        seedCourses();
        seedBudgetAllocations();
        seedSchedules();
    }

    private void seedUsers() {
        // Create Admin
        UserAccount admin = UserAccount.builder()
            .email("admin@highschool.edu")
            .password(passwordEncoder.encode("admin123"))
            .name("System Administrator")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_ADMIN))
            .build();
        userRepository.save(admin);

        // Create Dean
        UserAccount dean = UserAccount.builder()
            .email("dean@highschool.edu")
            .password(passwordEncoder.encode("dean123"))
            .name("Academic Dean")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_DEAN))
            .build();
        userRepository.save(dean);

        // Create Department Heads
        UserAccount mathHead = UserAccount.builder()
            .email("mathhead@highschool.edu")
            .password(passwordEncoder.encode("math123"))
            .name("Dr. Sarah Johnson")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_DEPARTMENT_HEAD))
            .build();
        userRepository.save(mathHead);

        UserAccount scienceHead = UserAccount.builder()
            .email("sciencehead@highschool.edu")
            .password(passwordEncoder.encode("science123"))
            .name("Dr. Michael Chen")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_DEPARTMENT_HEAD))
            .build();
        userRepository.save(scienceHead);

        // Create Teachers
        UserAccount teacher1 = UserAccount.builder()
            .email("teacher1@highschool.edu")
            .password(passwordEncoder.encode("teacher123"))
            .name("Prof. Emily Davis")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_TEACHER))
            .build();
        userRepository.save(teacher1);

        UserAccount teacher2 = UserAccount.builder()
            .email("teacher2@highschool.edu")
            .password(passwordEncoder.encode("teacher123"))
            .name("Prof. Robert Wilson")
            .emailVerified(true)
            .roles(Set.of(Role.ROLE_TEACHER))
            .build();
        userRepository.save(teacher2);
    }

    private void seedClassrooms() {
        // Math Building Classrooms
        Classroom mathRoom1 = Classroom.builder()
            .roomNumber("101")
            .building("Math Building")
            .capacity(30)
            .type(Classroom.ClassroomType.STANDARD)
            .maintenanceCostPerHour(new BigDecimal("15.00"))
            .utilityCostPerHour(new BigDecimal("8.50"))
            .hasProjector(true)
            .build();
        classroomRepository.save(mathRoom1);

        Classroom mathRoom2 = Classroom.builder()
            .roomNumber("102")
            .building("Math Building")
            .capacity(25)
            .type(Classroom.ClassroomType.COMPUTER_LAB)
            .maintenanceCostPerHour(new BigDecimal("25.00"))
            .utilityCostPerHour(new BigDecimal("12.00"))
            .hasProjector(true)
            .hasComputers(true)
            .build();
        classroomRepository.save(mathRoom2);

        // Science Building Classrooms
        Classroom sciLab1 = Classroom.builder()
            .roomNumber("201")
            .building("Science Building")
            .capacity(24)
            .type(Classroom.ClassroomType.SCIENCE_LAB)
            .maintenanceCostPerHour(new BigDecimal("35.00"))
            .utilityCostPerHour(new BigDecimal("18.00"))
            .hasProjector(true)
            .hasLab(true)
            .build();
        classroomRepository.save(sciLab1);
    }

    private void seedDepartments() {
        UserAccount mathHead = userRepository.findByEmailIgnoreCase("mathhead@highschool.edu").orElse(null);
        UserAccount scienceHead = userRepository.findByEmailIgnoreCase("sciencehead@highschool.edu").orElse(null);

        // Mathematics Department
        Department mathDept = Department.builder()
            .name("Mathematics Department")
            .code("MATH")
            .description("High school mathematics education")
            .departmentHead(mathHead)
            .totalBudget(new BigDecimal("500000.00"))
            .allocatedBudget(new BigDecimal("450000.00"))
            .spentBudget(new BigDecimal("320000.00"))
            .currentEnrollment(450)
            .enrollmentProjection(480)
            .build();
        departmentRepository.save(mathDept);

        // Science Department
        Department scienceDept = Department.builder()
            .name("Science Department")
            .code("SCI")
            .description("High school science education including Biology, Chemistry, and Physics")
            .departmentHead(scienceHead)
            .totalBudget(new BigDecimal("750000.00"))
            .allocatedBudget(new BigDecimal("700000.00"))
            .spentBudget(new BigDecimal("580000.00"))
            .currentEnrollment(380)
            .enrollmentProjection(420)
            .build();
        departmentRepository.save(scienceDept);
    }

    private void seedInstructors() {
        Department mathDept = departmentRepository.findByCode("MATH").orElse(null);
        Department scienceDept = departmentRepository.findByCode("SCI").orElse(null);
        UserAccount teacher1 = userRepository.findByEmailIgnoreCase("teacher1@highschool.edu").orElse(null);
        UserAccount teacher2 = userRepository.findByEmailIgnoreCase("teacher2@highschool.edu").orElse(null);

        // Math Instructor
        Instructor mathInstructor = Instructor.builder()
            .user(teacher1)
            .department(mathDept)
            .employeeId("EMP001")
            .firstName("Emily")
            .lastName("Davis")
            .instructorType(Instructor.InstructorType.FULL_TIME)
            .qualification(Instructor.QualificationLevel.MASTER)
            .annualSalary(new BigDecimal("65000.00"))
            .maxTeachingHours(25)
            .currentTeachingHours(20)
            .yearsOfExperience(8)
            .build();
        instructorRepository.save(mathInstructor);

        // Science Instructor
        Instructor scienceInstructor = Instructor.builder()
            .user(teacher2)
            .department(scienceDept)
            .employeeId("EMP002")
            .firstName("Robert")
            .lastName("Wilson")
            .instructorType(Instructor.InstructorType.FULL_TIME)
            .qualification(Instructor.QualificationLevel.DOCTORATE)
            .annualSalary(new BigDecimal("75000.00"))
            .maxTeachingHours(22)
            .currentTeachingHours(18)
            .yearsOfExperience(12)
            .build();
        instructorRepository.save(scienceInstructor);
    }

    private void seedCourses() {
        Department mathDept = departmentRepository.findByCode("MATH").orElse(null);
        Department scienceDept = departmentRepository.findByCode("SCI").orElse(null);

        // Math Courses
        Course algebra = Course.builder()
            .name("Algebra II")
            .courseCode("MATH201")
            .description("Advanced algebraic concepts and problem solving")
            .department(mathDept)
            .creditHours(4)
            .maxStudents(30)
            .currentEnrollment(28)
            .projectedEnrollment(30)
            .costPerStudent(new BigDecimal("150.00"))
            .equipmentCost(new BigDecimal("2000.00"))
            .materialsCost(new BigDecimal("800.00"))
            .courseType(Course.CourseType.REGULAR)
            .semester(Course.Semester.FALL)
            .academicYear(2024)
            .build();
        courseRepository.save(algebra);

        Course calculus = Course.builder()
            .name("AP Calculus")
            .courseCode("MATH301")
            .description("Advanced Placement Calculus")
            .department(mathDept)
            .creditHours(5)
            .maxStudents(25)
            .currentEnrollment(22)
            .projectedEnrollment(25)
            .costPerStudent(new BigDecimal("200.00"))
            .equipmentCost(new BigDecimal("1500.00"))
            .materialsCost(new BigDecimal("600.00"))
            .courseType(Course.CourseType.AP)
            .semester(Course.Semester.FALL)
            .academicYear(2024)
            .build();
        courseRepository.save(calculus);

        // Science Courses
        Course biology = Course.builder()
            .name("Biology I")
            .courseCode("SCI101")
            .description("Introduction to biological sciences")
            .department(scienceDept)
            .creditHours(4)
            .maxStudents(24)
            .currentEnrollment(23)
            .projectedEnrollment(24)
            .costPerStudent(new BigDecimal("180.00"))
            .equipmentCost(new BigDecimal("5000.00"))
            .materialsCost(new BigDecimal("1200.00"))
            .courseType(Course.CourseType.REGULAR)
            .semester(Course.Semester.FALL)
            .academicYear(2024)
            .build();
        courseRepository.save(biology);

        Course chemistry = Course.builder()
            .name("AP Chemistry")
            .courseCode("SCI201")
            .description("Advanced Placement Chemistry")
            .department(scienceDept)
            .creditHours(5)
            .maxStudents(20)
            .currentEnrollment(18)
            .projectedEnrollment(20)
            .costPerStudent(new BigDecimal("250.00"))
            .equipmentCost(new BigDecimal("8000.00"))
            .materialsCost(new BigDecimal("2000.00"))
            .courseType(Course.CourseType.AP)
            .semester(Course.Semester.FALL)
            .academicYear(2024)
            .build();
        courseRepository.save(chemistry);
    }

    private void seedBudgetAllocations() {
        Department mathDept = departmentRepository.findByCode("MATH").orElse(null);
        Department scienceDept = departmentRepository.findByCode("SCI").orElse(null);
        UserAccount dean = userRepository.findByEmailIgnoreCase("dean@highschool.edu").orElse(null);

        // Math Department Allocations
        BudgetAllocation mathSalaries = BudgetAllocation.builder()
            .department(mathDept)
            .category(BudgetAllocation.AllocationCategory.SALARIES_AND_BENEFITS)
            .description("Faculty salaries and benefits for Math Department")
            .allocatedAmount(new BigDecimal("300000.00"))
            .spentAmount(new BigDecimal("250000.00"))
            .fiscalYear(2024)
            .status(BudgetAllocation.AllocationStatus.APPROVED)
            .approvedBy(dean)
            .build();
        budgetAllocationRepository.save(mathSalaries);

        BudgetAllocation mathEquipment = BudgetAllocation.builder()
            .department(mathDept)
            .category(BudgetAllocation.AllocationCategory.EQUIPMENT_AND_TECHNOLOGY)
            .description("Calculators, computers, and educational technology")
            .allocatedAmount(new BigDecimal("50000.00"))
            .spentAmount(new BigDecimal("35000.00"))
            .fiscalYear(2024)
            .status(BudgetAllocation.AllocationStatus.APPROVED)
            .approvedBy(dean)
            .build();
        budgetAllocationRepository.save(mathEquipment);

        // Science Department Allocations
        BudgetAllocation scienceSalaries = BudgetAllocation.builder()
            .department(scienceDept)
            .category(BudgetAllocation.AllocationCategory.SALARIES_AND_BENEFITS)
            .description("Faculty salaries and benefits for Science Department")
            .allocatedAmount(new BigDecimal("400000.00"))
            .spentAmount(new BigDecimal("380000.00"))
            .fiscalYear(2024)
            .status(BudgetAllocation.AllocationStatus.APPROVED)
            .approvedBy(dean)
            .build();
        budgetAllocationRepository.save(scienceSalaries);

        BudgetAllocation scienceEquipment = BudgetAllocation.builder()
            .department(scienceDept)
            .category(BudgetAllocation.AllocationCategory.EQUIPMENT_AND_TECHNOLOGY)
            .description("Laboratory equipment and scientific instruments")
            .allocatedAmount(new BigDecimal("150000.00"))
            .spentAmount(new BigDecimal("120000.00"))
            .fiscalYear(2024)
            .status(BudgetAllocation.AllocationStatus.APPROVED)
            .approvedBy(dean)
            .build();
        budgetAllocationRepository.save(scienceEquipment);
    }

    private void seedSchedules() {
        Course algebra = courseRepository.findByCourseCode("MATH201").orElse(null);
        Course biology = courseRepository.findByCourseCode("SCI101").orElse(null);
        Classroom mathRoom1 = classroomRepository.findByRoomNumber("101").orElse(null);
        Classroom sciLab1 = classroomRepository.findByRoomNumber("201").orElse(null);

        if (algebra != null && mathRoom1 != null) {
            CourseSchedule algebraSchedule = CourseSchedule.builder()
                .course(algebra)
                .classroom(mathRoom1)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
            courseScheduleRepository.save(algebraSchedule);
        }

        if (biology != null && sciLab1 != null) {
            CourseSchedule biologySchedule = CourseSchedule.builder()
                .course(biology)
                .classroom(sciLab1)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .build();
            courseScheduleRepository.save(biologySchedule);
        }
    }
}
