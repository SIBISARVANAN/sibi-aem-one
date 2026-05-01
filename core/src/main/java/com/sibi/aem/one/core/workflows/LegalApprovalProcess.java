package com.sibi.aem.one.core.workflows;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Map;

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Legal Approval Process"})
public class LegalApprovalProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LegalApprovalProcess.class.getName());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String SUBSERVICE_NAME = "workflowService";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        Map<String, Object> params = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);
        try(ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(params)){
            Session session = resolver.adaptTo(Session.class);
            Resource assetResource = resolver.getResource(payloadPath + "/" + JcrConstants.JCR_CONTENT);
            if(assetResource != null){
                Node node = assetResource.adaptTo(Node.class);
                if(node != null){
                    node.setProperty("legalApproved", true);
                    LOG.debug("Asset {} marked as legally approved", payloadPath);
                }
                String destinationPath = "/content/dam/certified-assets/" + assetResource.getParent().getName();
                session.move(payloadPath, destinationPath);
                session.save();
                LOG.debug("Asset moved from {} to {}", payloadPath, destinationPath);
            }
        } catch (Exception e) {
            LOG.error("Error in Legal Approval Process", e);
            throw new WorkflowException(e);
        }
    }
}
