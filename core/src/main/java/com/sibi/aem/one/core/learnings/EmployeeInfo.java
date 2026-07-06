package com.sibi.aem.one.core.learnings;

public class EmployeeInfo {

    private String name;
    private String department;
    private String employeeId;


    public EmployeeInfo(String name, String department, String employeeId) {
        this.name = name;
        this.department = department;
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}