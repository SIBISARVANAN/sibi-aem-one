package com.sibi.aem.one.core.workflows;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.sibi.aem.one.core.services.ExternalApiService;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Product Sync Process"})
public class ProductSyncProcess implements WorkflowProcess {

    @Reference
    private ExternalApiService externalApiService;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    private static final Logger LOG = LoggerFactory.getLogger(ProductSyncProcess.class);

    private static final String SERVICE_USER = "workflow-process-service";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args){
        MetaDataMap wfdMetaData = workItem.getWorkflowData().getMetaDataMap();
        int retryCount = wfdMetaData.get("retryCount", 0);
        String payloadPath = workItem.getWorkflowData().getPayload().toString();

        try(ResourceResolver resolver = getServiceResolver()) {
            Resource payloadResource = resolver.getResource(payloadPath + "/" + JcrConstants.JCR_CONTENT);
            if (payloadResource == null) {
                return;
            }
            ValueMap properties = payloadResource.getValueMap();
            String sku = properties.get("productSku", String.class);
            String jsonData = externalApiService.fetchProductData(sku);
            if(StringUtils.isBlank(jsonData)){
                // Trigger the retry logic
                handleRetryLogic(workItem, retryCount);
            } else {
                // Process the data and finish
                workItem.getWorkflowData().getMetaDataMap().put("syncStatus", "SUCCESS");
            }
        } catch (Exception e) {
            LOG.error("Error while fetching product", e);
        }
    }

    private ResourceResolver getServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }

    private void handleRetryLogic(WorkItem workItem, int retryCount) throws WorkflowException {
        int maxRetries = 3;
        if (retryCount < maxRetries) {
            workItem.getWorkflowData().getMetaDataMap().put("retryCount", retryCount + 1);
            workItem.getWorkflowData().getMetaDataMap().put("syncStatus", "RETRY");

            // LOGIC: To actually "loop" in AEM, you use a 'Goto Step' in the model
            // which checks this "syncStatus" variable.
//            In your Workflow Model (Touch UI), place a Goto Step immediately after this Process Step.
//            Condition: Use an ECMA script or a Rule: metaData.get("syncStatus") == "RETRY".
//                    Target: Point the Goto step back to the Product Data Harmonizer step.
//                    This creates a programmatic loop that is visible and manageable in the UI.
            LOG.warn("Marking for retry. Attempt: {}", retryCount + 1);
        } else {
            workItem.getWorkflowData().getMetaDataMap().put("syncStatus", "FAILED");
            throw new WorkflowException("Product sync failed after " + maxRetries + " attempts.");
        }
    }
}
