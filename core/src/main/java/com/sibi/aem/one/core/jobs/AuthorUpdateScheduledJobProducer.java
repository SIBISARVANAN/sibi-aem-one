package com.sibi.aem.one.core.jobs;

import com.sibi.aem.one.core.configs.AuthorUpdateScheduledJobConfig;
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

@Component(service = AuthorUpdateScheduledJobProducer.class, immediate = true)
@Designate(ocd = AuthorUpdateScheduledJobConfig.class)
public class AuthorUpdateScheduledJobProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorUpdateScheduledJobProducer.class.getName());

    private static final String SCHEDULER_NAME = "sibi-aem-one.author.update.scheduler";

            @Reference
    private JobManager jobManager;

    private AuthorUpdateScheduledJobConfig config;

    @Activate
    protected void activate(final AuthorUpdateScheduledJobConfig config) {
        this.config = config;
        scheduleJob();
    }

    @Modified
    protected void modified(final AuthorUpdateScheduledJobConfig config) {
        this.config = config;
        unscheduleJob();
        scheduleJob();
    }

    @Deactivate
    protected void deactivate() {
        unscheduleJob();
    }

    private void scheduleJob() {
        if(!config.isEnabled()){
            LOG.info("Scheduled Job is disabled via Configuration; skipping registration");
            return;
        }

        Collection<ScheduledJobInfo> existingJobs = jobManager.getScheduledJobs(AuthorUpdateScheduledJobConsumer.JOB_TOPIC, 1, null);

        if(!existingJobs.isEmpty()){
            LOG.info("Scheduled Jobs already exist; skipping duplicate registration");
            return;
        }
        Map<String, Object> jobProperties = new HashMap<>();
        jobProperties.put("configParam", config.configParam());
        jobProperties.put("schedulerName", SCHEDULER_NAME);

        JobBuilder.ScheduleBuilder scheduleBuilder = jobManager.createJob(AuthorUpdateScheduledJobConsumer.JOB_TOPIC).properties(jobProperties).schedule();

        scheduleBuilder.cron(config.cronExpression());
        ScheduledJobInfo scheduledJobInfo = scheduleBuilder.add();

        if(scheduledJobInfo == null){
            LOG.error("Scheduled Job is null");
        } else{
            LOG.info("Scheduled Job Info: " + scheduledJobInfo.toString());
        }
    }

    private void unscheduleJob() {
        Collection<ScheduledJobInfo> existingJobs = jobManager.getScheduledJobs(AuthorUpdateScheduledJobConsumer.JOB_TOPIC, 1, null);
        for(ScheduledJobInfo scheduledJobInfo : existingJobs){
            scheduledJobInfo.unschedule();
            LOG.info("Unscheduled job: " + scheduledJobInfo.toString());
        }
    }
}

// How It All Fits Together
/*
@Activate / @Modified
       │
               ▼
MyScheduledJobRegistrar
  └── jobManager.createJob(TOPIC)
           .properties({...})
        .schedule()
           .cron("0 0 2 * * ?")
           .add()
                │
                        │  persisted at /var/eventing/scheduled-jobs
                │
                        ▼  (when cron fires)
JobManager creates a Job
                │
                        │  persisted at /var/eventing/jobs
                │
                        ▼
                        MyScheduledJobConsumer.process(job)
          └── returns OK / FAILED / CANCEL
*/