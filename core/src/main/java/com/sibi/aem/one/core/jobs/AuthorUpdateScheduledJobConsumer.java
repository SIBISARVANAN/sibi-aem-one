package com.sibi.aem.one.core.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = JobConsumer.class, immediate = true, property = {JobConsumer.PROPERTY_TOPICS + "=" + AuthorUpdateScheduledJobConsumer.JOB_TOPIC})
public class AuthorUpdateScheduledJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorUpdateScheduledJobConsumer.class);

    public static final String JOB_TOPIC = "sibi-aem-one.author.update.scheduled";

    @Override
    public JobResult process(Job job){
        try{
            LOG.info("Processing job {}", job.getId());

            String configParam = (String) job.getProperty("configParam");
            LOG.info("configParam {}", configParam);

            doWork(configParam);

            LOG.info("Finished processing job {}", job.getId());
            return JobResult.OK;
        } catch (Exception e) {
            LOG.error("Job failed - {}", e);
            return JobResult.FAILED; // Sling will retry automatically
        }
    }

    private void doWork(String configParam){
        LOG.info("doWork {}", configParam);
        // do whatever you want here
    }
}
