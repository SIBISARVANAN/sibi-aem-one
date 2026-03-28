package com.sibi.aem.one.core.schedulers;

import com.sibi.aem.one.core.configs.AuthorUpdateSchedulerConfig;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Runnable.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = AuthorUpdateSchedulerConfig.class)
public class AuthorUpdateScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorUpdateScheduler.class);

    private boolean enabled;

    private String schedulerName;

    @Reference
    private Scheduler scheduler;

    @Activate
    protected void activate(AuthorUpdateSchedulerConfig config) {
        this.enabled = config.enabled();
        schedulerName = "sibi-aem-one.author.update";
        if (enabled) {
            ScheduleOptions scheduleOptions = scheduler.EXPR(config.cronExpression());
            scheduleOptions.name(schedulerName);
            scheduleOptions.canRunConcurrently(false);
            scheduler.schedule(this, scheduleOptions);
            LOG.debug(schedulerName + " scheduled");
        }

    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(schedulerName);
        LOG.debug(schedulerName + " unscheduled");
    }

    @Override
    public void run() {
        LOG.debug(schedulerName + " started");
    }
    
}
