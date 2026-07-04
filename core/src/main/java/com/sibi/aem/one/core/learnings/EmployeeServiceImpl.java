package com.sibi.aem.one.core.learnings;

import org.osgi.service.component.annotations.Component;

@Component(service = EmployeeService.class)
public class EmployeeServiceImpl implements EmployeeService {

    @Override
    public String formatName(String name) {
        return "Employee : " + name;
    }
}