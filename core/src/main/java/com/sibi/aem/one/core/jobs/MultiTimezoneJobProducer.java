package com.sibi.aem.one.core.jobs;

import com.sibi.aem.one.core.configs.MultiTimezoneJobConfig;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component(service = MultiTimezoneJobProducer.class, immediate = true)
@Designate(ocd = MultiTimezoneJobConfig.class)
public class MultiTimezoneJobProducer {

    private static final Logger log = LoggerFactory.getLogger(MultiTimezoneJobProducer.class);

    private static final String TOPIC = MultiTimezoneJobConsumer.JOB_TOPIC;

    // Unique name per timezone job — used to track and unschedule individually
    private static final String TZ1_SCHEDULER_NAME = "multitz.job.tz1";
    private static final String TZ2_SCHEDULER_NAME = "multitz.job.tz2";
    private static final String TZ3_SCHEDULER_NAME = "multitz.job.tz3";

    @Reference
    private JobManager jobManager;

    private MultiTimezoneJobConfig config;

    @Activate
    protected void activate(MultiTimezoneJobConfig config) {
        this.config = config;
        scheduleAllJobs();
    }

    @Modified
    protected void modified(MultiTimezoneJobConfig config) {
        this.config = config;
        unscheduleAllJobs();
        scheduleAllJobs();
    }

    @Deactivate
    protected void deactivate() {
        unscheduleAllJobs();
    }

    // ----------------------------------------------------------

    private void scheduleAllJobs() {
        if (!config.enabled()) {
            log.info("Multi-timezone job is disabled. Skipping.");
            return;
        }

        scheduleTimezoneJob(
                TZ1_SCHEDULER_NAME,
                config.timezone1_id(),
                config.timezone1_cron()
        );
        scheduleTimezoneJob(
                TZ2_SCHEDULER_NAME,
                config.timezone2_id(),
                config.timezone2_cron()
        );
        scheduleTimezoneJob(
                TZ3_SCHEDULER_NAME,
                config.timezone3_id(),
                config.timezone3_cron()
        );
    }

    private void scheduleTimezoneJob(String schedulerName, String timezoneId, String cronExpr) {

        // Duplicate guard — filter by scheduler name property
        Collection<ScheduledJobInfo> existing = jobManager.getScheduledJobs(TOPIC, -1, null);
        for (ScheduledJobInfo info : existing) {
            Object existingName = info.getJobProperties().get("schedulerName");
            if (schedulerName.equals(existingName)) {
                log.info("Job already scheduled for [{}]. Skipping.", timezoneId);
                return;
            }
        }

        Map<String, Object> props = new HashMap<>();
        props.put("timezoneId", timezoneId);
        props.put("schedulerName", schedulerName); // used for duplicate detection above

        JobBuilder.ScheduleBuilder sb = jobManager
                .createJob(TOPIC)
                .properties(props)
                .schedule();

        sb.cron(cronExpr);

        ScheduledJobInfo result = sb.add();

        if (result == null) {
            log.error("Failed to schedule job for timezone [{}] with cron [{}]", timezoneId, cronExpr);
        } else {
            log.info("Scheduled job for timezone [{}] with cron [{}]", timezoneId, cronExpr);
        }
    }

    private void unscheduleAllJobs() {
        Collection<ScheduledJobInfo> existing = jobManager.getScheduledJobs(TOPIC, -1, null);
        for (ScheduledJobInfo info : existing) {
            info.unschedule();
            log.info("Unscheduled job: {}", info.getJobProperties().get("schedulerName"));
        }
    }
}