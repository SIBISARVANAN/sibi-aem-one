package com.sibi.aem.one.core.listeners;

import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
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

@Component(
        service = {ResourceChangeListener.class},
        immediate = true,
        property = {
                ResourceChangeListener.PATHS + "=/content/mysite",
                ResourceChangeListener.CHANGES + "=CHANGED"
        }
)
public class ClusterAwareListener
        implements ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareListener.class.getName());

    @Reference
    JobManager jobManager;

    @Override
    public void onChange(List<ResourceChange> changes) {
        changes.stream()
                // ignore jcr:content child property noise
                .filter(c -> !c.getPath().contains("jcr:content/"))
                // only care about page-level nodes
                .filter(c -> c.getPath().endsWith("jcr:content")
                        || !c.getPath().contains("jcr:"))
                // only process ADDED and CHANGED, not REMOVED
                .filter(c -> c.getType() != ResourceChange.ChangeType.REMOVED)
                .forEach(change -> {
                    // tells you if it came from THIS node or another cluster node
                    boolean isExternal = change.isExternal();
                    LOG.info("Change: {} | path: {} | external: {}",
                            change.getType(), change.getPath(), isExternal);

                    LOG.info("Processing: {} at {}", change.getType(), change.getPath());

                    if (!isExternal) {
                        // handle local change
                        Map<String, Object> props = new HashMap<>();
                        props.put("path", change.getPath());
                        props.put("changeType", change.getType().name());
                        jobManager.addJob("sibi-aem-one.clusterawarelistener.job.internal", props);
                    } else {
                        // handle change that came from another cluster node
                        Map<String, Object> props = new HashMap<>();
                        props.put("path", change.getPath());
                        props.put("changeType", change.getType().name());
                        jobManager.addJob("sibi-aem-one.clusterawarelistener.job.external", props);
                    }

                });
    }
}