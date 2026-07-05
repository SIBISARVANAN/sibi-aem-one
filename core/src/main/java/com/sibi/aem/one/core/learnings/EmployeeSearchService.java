package com.sibi.aem.one.core.learnings;

import java.util.List;

public interface EmployeeSearchService {

    List<String> getEmployeeNames();

    List<Employee> getEmployees();
}