package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmployeeServiceImplTest {

    EmployeeServiceImpl employeeService;

    @BeforeEach
    void setupTest() {
        employeeService = new EmployeeServiceImpl();
    }

    @Test
    void formatName(){
        assertEquals("Employee : Sarvanan", employeeService.formatName("Sarvanan"));
    }
}
