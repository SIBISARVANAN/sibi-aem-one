package com.sibi.aem.one.core.learnings;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Employee Scheduler Configuration")
public @interface EmployeeSchedulerConfig {

    @AttributeDefinition(
            name = "Scheduler Name")
    String schedulerName() default "Employee Cleanup Scheduler";

    @AttributeDefinition(
            name = "Max Employees")
    boolean enabled() default false;

}