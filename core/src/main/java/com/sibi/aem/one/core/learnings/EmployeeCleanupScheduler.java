package com.sibi.aem.one.core.learnings;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = EmployeeCleanupSchedulerConfig.class)
public class EmployeeCleanupScheduler implements Runnable {

    @Reference
    private Scheduler scheduler;

    @Reference
    private EmployeeService employeeService;

    private String schedulerName;

    private boolean enabled;

    private String schedulerCron;

    @Activate
    @Modified
    protected void activate(EmployeeCleanupSchedulerConfig config) {
        this.schedulerName = config.schedulerName();
        this.enabled = config.enabled();
        this.schedulerCron = config.schedulerCron();
        ScheduleOptions scheduleOptions = scheduler.EXPR(schedulerCron);
        scheduleOptions.name(schedulerName);
        scheduler.unschedule(schedulerName);
        if (enabled) {
            scheduler.schedule(this, scheduleOptions);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (enabled) {
            scheduler.unschedule(schedulerName);
        }
    }

    @Override
    public void run() {
        employeeService.cleanupEmployees();
    }
}
