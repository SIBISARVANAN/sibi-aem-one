package com.sibi.aem.one.core.learnings;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Employee Cleanup Scheduler Configuration")
public @interface EmployeeCleanupSchedulerConfig {

    @AttributeDefinition(
            name = "Scheduler Name")
    String schedulerName() default "Employee Cleanup Scheduler";

    @AttributeDefinition(
            name = "Scheduler CRON")
    String schedulerCron() default "0 0/5 * * * ?";

    @AttributeDefinition(
            name = "Max Employees")
    boolean enabled() default false;

}