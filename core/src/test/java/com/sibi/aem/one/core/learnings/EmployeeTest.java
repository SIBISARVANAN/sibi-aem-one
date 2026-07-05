package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmployeeTest {

    @Test
    void employeeTest() {
        Employee e1 = new Employee("Sibi", "Engineering");
        Employee e2 = new Employee("John", "HR");
        assertEquals("Sibi", e1.getName());
        assertEquals("Engineering", e1.getDepartment());
        assertEquals("John", e2.getName());
        assertEquals("HR", e2.getDepartment());
    }
}
