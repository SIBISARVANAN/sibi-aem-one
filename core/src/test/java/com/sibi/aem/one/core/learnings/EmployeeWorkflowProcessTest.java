package com.sibi.aem.one.core.learnings;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeWorkflowProcessTest {

    @Mock
    EmployeeValidationService employeeValidationService;

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Resource resource;

    @Mock
    Resource contentResource;

    @Mock
    ValueMap valueMap;

    @Mock
    WorkItem workItem;

    @Mock
    WorkflowSession workflowSession;

    @Mock
    WorkflowData workflowData;

    @Mock
    MetaDataMap metaDataMap;

    @InjectMocks
    EmployeeWorkflowProcess employeeWorkflowProcess;

    @Test
    void executeSuccess() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(workflowData.getMetaDataMap()).thenReturn(metaDataMap);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource("/content/company/employees/sibi")).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("name", String.class)).thenReturn("Sibi");
        when(valueMap.get("department", String.class)).thenReturn("Engineering");
        when(valueMap.get("employeeId", String.class)).thenReturn("7");
        EmployeeInfo employeeInfo = new EmployeeInfo("Sibi", "Engineering", "7");
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(true);
        //in the above line, we are using any instance of EmployeeInfo class as we cant compare EmployeeInfo objects.
        employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap);
        verify(employeeValidationService).validateEmployee(any(EmployeeInfo.class));
        verify(metaDataMap).put("validated", true);
    }

    @Test
    void executeValidationFails() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(workflowData.getMetaDataMap()).thenReturn(metaDataMap);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource("/content/company/employees/sibi")).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("name", String.class)).thenReturn("Sibi");
        when(valueMap.get("department", String.class)).thenReturn("Engineering");
        when(valueMap.get("employeeId", String.class)).thenReturn("");
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(false);
        employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap);
        verify(employeeValidationService).validateEmployee(any(EmployeeInfo.class));
        verify(metaDataMap).put("validated", false);
    }

    @Test
    void executeWithoutPayloadResource() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource("/content/company/employees/sibi")).thenReturn(null);
        employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap);
        verify(employeeValidationService, never()).validateEmployee(any());
    }

    @Test
    void executeWithoutResourceResolver() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        assertDoesNotThrow(() -> employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap));
    }

    @Test
    void executeValidationWithNull() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(workflowData.getMetaDataMap()).thenReturn(metaDataMap);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource("/content/company/employees/sibi")).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("name", String.class)).thenReturn("Sibi");
        when(valueMap.get("department", String.class)).thenReturn("Engineering");
        when(valueMap.get("employeeId", String.class)).thenReturn(null);
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(false);
        employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap);
        verify(employeeValidationService).validateEmployee(any(EmployeeInfo.class));
        verify(metaDataMap).put("validated", false);
    }

    @Test
    void executeValidationEmptyMap() throws LoginException, WorkflowException {
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/company/employees/sibi");
        when(workflowData.getMetaDataMap()).thenReturn(metaDataMap);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource("/content/company/employees/sibi")).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap);
        employeeWorkflowProcess.execute(workItem, workflowSession, metaDataMap);
        verify(employeeValidationService).validateEmployee(any(EmployeeInfo.class));
        verify(metaDataMap).put("validated", false);
    }

}
