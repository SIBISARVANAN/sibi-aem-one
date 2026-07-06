package com.sibi.aem.one.core.learnings;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.Collections;

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Employee Workflow"})
public class EmployeeWorkflowProcess implements WorkflowProcess {

    public static final Logger LOG = LoggerFactory.getLogger(EmployeeWorkflowProcess.class);

    @Reference
    EmployeeService employeeService;

    @Reference
    EmployeeValidationService employeeValidationService;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String path = workItem.getWorkflowData().getPayload().toString();
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            Resource resource = resolver.getResource(path);
            if(resource == null) {
                LOG.error("Resource not found: {}", path);
                return;
            }
            Resource contentResource = resource.getChild("jcr:content");
            if(contentResource == null) {
                LOG.error("Resource not found: {}", path);
                return;
            }
            ValueMap values = contentResource.getValueMap();
            String name =  values.get("name", String.class);
            String department = values.get("department", String.class);
            String employeeId  = values.get("employeeId", String.class);
            EmployeeInfo employeeInfo = new EmployeeInfo(name, department, employeeId);
            if(employeeValidationService.validateEmployee(employeeInfo)) {
                workItem.getWorkflowData().getMetaDataMap().put("validated", true);
            } else {
                workItem.getWorkflowData().getMetaDataMap().put("validated", false);
                LOG.error("Employee validation failed");
            }
        } catch (LoginException e){
            LOG.error("Login Exception", e);
        }
    }
}
