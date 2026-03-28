package com.sibi.aem.one.core.listeners;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = ResourceChangeListener.class, immediate = true, property = {
        // which paths to listen on
        ResourceChangeListener.PATHS + "=/content/sibi-aem-one",
        // listen only to .html resources anywhere under mysite
        ResourceChangeListener.PATHS + "=glob:/content/sibi-aem-one/**/*.html",
        // listen to anything under /products/ at any depth
        ResourceChangeListener.PATHS + "=glob:/content/sibi-aem-one/**/products/**",
        // listen to jcr:content nodes only (page properties changes)
        ResourceChangeListener.PATHS + "=glob:/content/sibi-aem-one/**/jcr:content",
        // which change types to listen for
        ResourceChangeListener.CHANGES + "=ADDED",
        ResourceChangeListener.CHANGES + "=CHANGED",
        ResourceChangeListener.CHANGES + "=REMOVED"})
public class ContentChangeListener implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ContentChangeListener.class.getName());

    @Reference
    JobManager jobManager;

    @Override
    public void onChange(List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            String path = change.getPath();
            ResourceChange.ChangeType changeType = change.getType();

            // filter to only pages, not every node
            if (!path.contains("jcr:content")) {
                Map<String, Object> props = new HashMap<>();
                props.put("path", path);
                props.put("changeType", changeType.name());
                jobManager.addJob("sibi-aem-one.contentchangelistener.job", props);
            }
        }
    }
}
