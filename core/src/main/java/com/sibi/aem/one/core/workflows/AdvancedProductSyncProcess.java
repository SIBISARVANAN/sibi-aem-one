package com.sibi.aem.one.core.workflows;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

//The Scenario: Multi-Stage External Content Synchronizer
//Imagine a workflow that:
//
//Fetches Product Data from an external REST API based on a SKU property on the payload.
//
//Handles Transient Failures by implementing a "Retry" mechanism using Workflow Variables (new in later 6.5 SPs).
//
//Updates JCR Metadata but does so using Resource API (more modern than JCR Session).
//
//Transitions the Workflow State based on the API response (e.g., if the product is "Discontinued," it triggers a specific branch).

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Advanced Product Sync Process"})
public class AdvancedProductSyncProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedProductSyncProcess.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String SERVICE_USER = "workflow-process-service";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        // Accessing Workflow Variables
        // Variables are stored in the workflow data metadata map
        MetaDataMap wfdMetaData = workItem.getWorkflowData().getMetaDataMap();
        int retryCount = wfdMetaData.get("retryCount", 0);
        String payloadPath = workItem.getWorkflowData().getPayload().toString();

        try(ResourceResolver resolver = getServiceResolver()){
            Resource payloadResource = resolver.getResource(payloadPath + "/" + JcrConstants.JCR_CONTENT);
            if(payloadResource == null){
                return;
            }
            ValueMap properties = payloadResource.getValueMap();
            String sku = properties.get("productSku", String.class);
            if(StringUtils.isNotBlank(sku)){
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet("https://api.external.com/products/" + sku);
                    try (CloseableHttpResponse response = httpClient.execute(request)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == 200) {
                            processSuccess(response, payloadPath, workItem, workflowSession);
                        } else {
                            handleRetryLogic(workItem, retryCount);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Network failure during product sync", e);
                    handleRetryLogic(workItem, retryCount);
                }
            }
            if(resolver.hasChanges()){
                resolver.commit();
            }
        } catch (Exception e) {
            LOG.error("Error occured in AdvancedProductSyncProcess for payload {}", payloadPath, e);
            throw new WorkflowException("Failed to sync product data", e);
        }
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

    private ResourceResolver getServiceResolver() throws LoginException {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }

    private void processSuccess(CloseableHttpResponse response, String payloadPath, WorkItem workItem, WorkflowSession workflowSession) throws IOException {
        HttpEntity entity = response.getEntity();
        String json = EntityUtils.toString(entity);
        workItem.getWorkflowData().getMetaDataMap().put("syncStatus", "SUCCESS");
    }

}
