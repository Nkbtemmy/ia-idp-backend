package com.urutare.sso.enums;

import java.util.Set;
import java.util.HashSet;

public enum Role {
    ROLE_USER(1, "User"),
    ROLE_TEACHER(2, "Teacher"), 
    ROLE_DEPARTMENT_HEAD(3, "Department Head"),
    ROLE_MANAGER(4, "Manager"),
    ROLE_DEAN(5, "Dean"),
    ROLE_ADMIN(6, "Administrator");

    private final int hierarchyLevel;
    private final String displayName;

    Role(int hierarchyLevel, String displayName) {
        this.hierarchyLevel = hierarchyLevel;
        this.displayName = displayName;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasPermissionOver(Role otherRole) {
        return this.hierarchyLevel >= otherRole.hierarchyLevel;
    }

    public Set<Role> getSubordinateRoles() {
        Set<Role> subordinates = new HashSet<>();
        for (Role role : Role.values()) {
            if (this.hierarchyLevel > role.hierarchyLevel) {
                subordinates.add(role);
            }
        }
        return subordinates;
    }

    public static Set<Role> getEducationRoles() {
        return Set.of(ROLE_TEACHER, ROLE_DEPARTMENT_HEAD, ROLE_DEAN);
    }

    public static Set<Role> getManagementRoles() {
        return Set.of(ROLE_DEPARTMENT_HEAD, ROLE_MANAGER, ROLE_DEAN, ROLE_ADMIN);
    }
}
