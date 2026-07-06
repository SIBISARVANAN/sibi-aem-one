package com.sibi.aem.one.core.learnings;

import org.osgi.service.component.annotations.Component;

@Component(service = EmployeeCleanupService.class)
public class EmployeeCleanupServiceImpl
        implements EmployeeCleanupService {

    @Override
    public void cleanup() {
        System.out.println("Cleanup completed");
    }

}