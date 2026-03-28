package com.sibi.aem.one.core.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=" + MultiTimezoneJobConsumer.JOB_TOPIC
        }
)
public class MultiTimezoneJobConsumer implements JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(MultiTimezoneJobConsumer.class);

    public static final String JOB_TOPIC = "com/example/myapp/multitimezonejob";

    @Override
    public JobResult process(Job job) {
        String timezoneId = (String) job.getProperty("timezoneId");

        try {
            ZonedDateTime triggeredAt = ZonedDateTime.now(ZoneId.of(timezoneId));
            log.info("Job triggered for timezone: {} | Local time: {}", timezoneId, triggeredAt);

            // Run your timezone-specific business logic
            doWork(timezoneId);

            log.info("Job completed for timezone: {}", timezoneId);
            return JobResult.OK;

        } catch (Exception e) {
            log.error("Job failed for timezone: {}. Will retry.", timezoneId, e);
            return JobResult.FAILED;
        }
    }

    private void doWork(String timezoneId) {
        // Your actual business logic here
        // e.g. send region-specific emails, trigger replication, sync data
        log.info("Executing business logic for timezone: {}", timezoneId);
    }
}
