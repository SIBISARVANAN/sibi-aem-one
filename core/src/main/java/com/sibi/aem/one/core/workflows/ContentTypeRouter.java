package com.sibi.aem.one.core.workflows;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;

//In an OR Split, you can use a Java-based process to determine the branch.
//This step sets a metadata flag that the routing rule then evaluates.
//In the Workflow Model: Use an OR Split. For "Branch 1" (Asset), use the rule: ${workflowData.metaData.route == 'asset'}.

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Content Type Router"})
public class ContentTypeRouter implements WorkflowProcess {

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        MetaDataMap wfd = workItem.getWorkflowData().getMetaDataMap();
        // Logic: Check if the payload is under /content/dam or /content/site
        if (payloadPath.startsWith("/content/dam")) {
            wfd.put("route", "asset");
        } else {
            wfd.put("route", "page");
        }
    }
}
