package com.sibi.aem.one.core.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = JobConsumer.class, immediate = true, property = {JobConsumer.PROPERTY_TOPICS + "=sibi-aem-one.author.update"})
public class AuthorUpdateJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorUpdateJobConsumer.class);
    @Override
    public JobResult process(Job job){
        try {
            String path = (String) job.getProperty("payloadPath");
            LOG.info("Received Job Payload: " + path);
            // do whatever you want here
            return JobResult.OK;
        } catch (Exception e){
            LOG.error("Job failed - {}", e);
            return JobResult.FAILED;
        }
    }
}
