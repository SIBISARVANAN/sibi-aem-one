package com.sibi.aem.one.core.workflows;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//The Scenario: "The Product Data Harmonizer"
//When a marketing asset is approved, the workflow must:
//
//Read Dynamic Metadata: Fetch a Product ID from the asset.
//
//External API Integration: Call a PIM (Product Information Management) system to validate the ID.
//
//Workflow Variables: Store the PIM response in a workflow variable (introduced in AEM 6.5) to pass data to subsequent steps.
//
//Error Handling & Retries: Implement custom logic to handle API timeouts without failing the entire workflow.
//



@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Advanced Product Data Harmonizer"})
public class ProductDataHarmonizer implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ProductDataHarmonizer.class);

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    private static final String VAR_PIM_STATUS = "pimStatus";
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {

        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        MetaDataMap workflowData = workItem.getWorkflowData().getMetaDataMap();
        try(ResourceResolver resolver = getServiceResolver()){
            String productId = getMetadata(resolver, payloadPath, "product-id");
            if(productId == null){
                LOG.error("Missing product id for {}", payloadPath);
                throw new WorkflowException("Mandatory Metadata 'product-id' is missing.");
            }
            String status = "validated"; // use an OSGI service with retry capability to validate PIM.
            workflowData.put(VAR_PIM_STATUS, status); // this allows next step to route based on this status.
            LOG.debug("Workflow variable {} updated to {} for {}", VAR_PIM_STATUS, status, payloadPath);
        } catch (Exception e) {
            LOG.error("Inside error in Harmonizer Process", e);
            throw new WorkflowException(e);
        }
    }

    private ResourceResolver getServiceResolver() throws Exception{
        Map<String, Object> props = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "pim-integration-service");
        return resourceResolverFactory.getServiceResourceResolver(props);
    }

    private String getMetadata(ResourceResolver resolver, String payloadPath, String key) throws Exception{
        return resolver.getResource(payloadPath + "/" + JcrConstants.JCR_CONTENT + "/metadata").getValueMap().get(key, String.class);
    }
}
