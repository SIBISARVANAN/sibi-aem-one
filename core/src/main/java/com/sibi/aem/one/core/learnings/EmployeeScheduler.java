package com.sibi.aem.one.core.learnings;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Runnable.class)
@Designate(ocd = EmployeeSchedulerConfig.class)
public class EmployeeScheduler implements Runnable {

    @Reference
    EmployeeCleanupService  employeeCleanupService;

    private String schedulerName;

    private boolean enabled;

    public static final Logger LOG = LoggerFactory.getLogger(EmployeeScheduler.class);

    @Activate
    public void activate(EmployeeSchedulerConfig config) {
        this.schedulerName = config.schedulerName();
        this.enabled = config.enabled();
    }

    @Override
    public void run() {
        try {
            if(enabled) {
                employeeCleanupService.cleanup();
            }
        } catch (Exception e) {
            LOG.error("Scheduler failed");
        }
    }
}
