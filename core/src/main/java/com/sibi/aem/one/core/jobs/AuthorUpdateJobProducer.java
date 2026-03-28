package com.sibi.aem.one.core.jobs;

import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.Map;

@Component(service = AuthorUpdateJobProducer.class, immediate = true)
public class AuthorUpdateJobProducer {

    private static final String JOB_TOPIC = "sibi-aem-one.author.update";

    @Reference
    private JobManager jobManager;

    public void triggerJob(String payload){
        Map<String,Object> props = new HashMap<>();
        props.put("payloadPath", payload);
        jobManager.addJob(JOB_TOPIC, props);
    }
}
