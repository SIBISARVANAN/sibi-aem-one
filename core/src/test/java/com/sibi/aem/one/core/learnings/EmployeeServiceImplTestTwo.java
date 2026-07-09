package com.sibi.aem.one.core.learnings;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
public class EmployeeServiceImplTestTwo {

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

    @InjectMocks
    EmployeeServiceImpl employeeService;

    private final AemContext aemContext = new AemContext();

    @Test
    void testGetEmployeeInfoWithMockito() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.getChild("jcr:content")).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("name", String.class)).thenReturn("Sibi");
        when(valueMap.get("department", String.class)).thenReturn("Engineering");
        when(valueMap.get("employeeId", String.class)).thenReturn("7");
        EmployeeInfo employeeInfo = employeeService.getEmployeeInfo("/content/company/employees/sibi");
        assertEquals("Sibi", employeeInfo.getName());
        assertEquals("Engineering", employeeInfo.getDepartment());
        assertEquals("7", employeeInfo.getEmployeeId());
        InOrder inOrder = inOrder(resourceResolverFactory, resourceResolver, resource, contentResource, valueMap);
        inOrder.verify(resourceResolverFactory).getServiceResourceResolver(anyMap());
        inOrder.verify(resourceResolver).getResource("/content/company/employees/sibi");
        inOrder.verify(resource).getChild("jcr:content");
        inOrder.verify(contentResource).getValueMap();
        inOrder.verify(valueMap).get("name", String.class);
        inOrder.verify(valueMap).get("department", String.class);
        inOrder.verify(valueMap).get("employeeId", String.class);
    }

    @Test
    void testGetEmployeeInfoWithAemContext() throws LoginException, NoSuchFieldException, IllegalAccessException {
        EmployeeServiceImpl service = new EmployeeServiceImpl();
        Field field =
                EmployeeServiceImpl.class.getDeclaredField("resourceResolverFactory");
        field.setAccessible(true);
        field.set(service, resourceResolverFactory);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(aemContext.resourceResolver());
        aemContext.registerInjectActivateService(service);
        aemContext.create().resource("/content/company/employees/sibi/jcr:content","name","Sibi",
                "department","Engineering",
                "employeeId","7");
        EmployeeInfo employeeInfo = service.getEmployeeInfo("/content/company/employees/sibi");
        assertEquals("Sibi", employeeInfo.getName());
        assertEquals("Engineering", employeeInfo.getDepartment());
        assertEquals("7", employeeInfo.getEmployeeId());

    }


}
