package com.sibi.aem.one.core.handlers;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = EventHandler.class, immediate = true, property = {EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC})
public class ReplicationEventHandler implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicationEventHandler.class.getName());

    @Reference
    JobManager jobManager;

    @Override
    public void handleEvent(Event event) {
        String actionType = (String) event.getProperty(ReplicationAction.PN_ACTION_TYPE);
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        LOG.info("Replication event: type={}, path={}", actionType, path);

        // fire a job to do whatever you want
        if (ReplicationActionType.ACTIVATE.name().equals(actionType)) {
            Map<String, Object> props = new HashMap<>();
            props.put("path", path);
            jobManager.addJob("sibi-aem-one.replicationeventhandler.job", props);
        }
    }
}
