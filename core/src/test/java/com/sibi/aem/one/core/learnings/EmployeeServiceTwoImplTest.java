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

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @Test
    void getLocalField() throws NoSuchFieldException, IllegalAccessException {
        Field localField = EmployeeServiceTwoImpl.class.getDeclaredField("localField");
        localField.setAccessible(true);
        localField.set(employeeService, "Sarvanan");
        assertEquals("Sarvanan", employeeService.getLocalField());
    }

    @Test
    void getModifiedLocalField() throws NoSuchFieldException, IllegalAccessException {
        Field localField = EmployeeServiceTwoImpl.class.getDeclaredField("localField");
        localField.setAccessible(true);
        localField.set(employeeService, "Shravan");
        assertEquals("Shravan", employeeService.getLocalField());
    }

    @Test
    void getEmployeeDetails() {
        EmployeeServiceTwoImpl employeeServiceSpy = spy(new EmployeeServiceTwoImpl());
        doReturn("HR").when(employeeServiceSpy).getEngineeringDepartment();
        assertEquals("Department : HR", employeeServiceSpy.getEmployeeDetails());
    }

    @Test
    void getFormattedEmployeeDetails() {
        EmployeeServiceTwoImpl employeeServiceSpy = spy(new EmployeeServiceTwoImpl());
        doReturn("HR").when(employeeServiceSpy).getFormattedEngineeringDepartment();
        assertEquals("Department : HR", employeeServiceSpy.getFormattedEmployeeDetails());
    }
}
