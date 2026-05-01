package com.sibi.aem.one.core.listeners;

import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Component(service = ResourceChangeListener.class, property = {
        ResourceChangeListener.PATHS + "=/content/dam/sibi-aem-one",
        ResourceChangeListener.CHANGES + "=ADDED",
        ResourceChangeListener.CHANGES + "=CHANGED"
})
public class CustomWorkflowTrigger implements ResourceChangeListener{

    private static final Logger LOG = LoggerFactory.getLogger(CustomWorkflowTrigger.class.getName());

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public void onChange(List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "workflow-service"))) {
                // 1. Perform Complex Logic Check here
                if (shouldTriggerWorkflow(change, resolver)) {
                    // 2. Start Workflow Programmatically
                    WorkflowSession wfSession = resolver.adaptTo(WorkflowSession.class);
                    WorkflowModel model = wfSession.getModel("/var/workflow/models/my-custom-model");
                    WorkflowData data = wfSession.newWorkflowData("JCR_PATH", change.getPath());

                    wfSession.startWorkflow(model, data);
                    LOG.info("Started workflow for path: {}", change.getPath());
                }

            } catch (Exception e) {
                LOG.error("Error triggering workflow for {}", change.getPath(), e);
            }
        }
    }

    private boolean shouldTriggerWorkflow(ResourceChange change, ResourceResolver resolver) {
        // Your complex Java logic goes here
        return true;
    }

}
