package com.maiphuhai.model;

public class Role {
    private int RoleId;
    private String RoleName;

    public Role (){}

    public Role(int roleId, String roleName) {
        RoleId = roleId;
        RoleName = roleName;
    }

    public int getRoleId() {
        return RoleId;
    }

    public void setRoleId(int roleId) {
        RoleId = roleId;
    }

    public String getRoleName() {
        return RoleName;
    }

    public void setRoleName(String roleName) {
        RoleName = roleName;
    }
}
