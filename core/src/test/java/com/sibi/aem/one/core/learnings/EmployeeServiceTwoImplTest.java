package com.sibi.aem.one.core.learnings;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class EmployeeServiceTwoImplTest {

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Resource resource;

    @Mock
    ValueMap valueMap;

    @InjectMocks
    EmployeeServiceTwoImpl employeeService;

    @Test
    void getDepartment() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("department", String.class)).thenReturn("Engineering");
        assertEquals("Engineering", employeeService.getDepartment("/content/department"));
        verify(resourceResolver).close();
    }

    @Test
    void getResourceDoesNotExist() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource(anyString())).thenReturn(null);
        assertNull(employeeService.getDepartment("/content/department"));
    }

    @Test
    void getLoginException() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        assertNull(employeeService.getDepartment("/content/department"));
    }
}
