package com.sibi.aem.one.core.learnings;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeePageServiceImplTest {

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    PageManager pageManager;

    @Mock
    Page page;

    @Mock
    Page sibiPage;

    @Mock
    Page johnPage;

    @Mock
    Page alicePage;

    @Mock
    Resource sibiPageResource;

    @Mock
    ValueMap sibiPageValueMap;

    @Mock
    Resource johnPageResource;

    @Mock
    ValueMap johnPageValueMap;

    @Mock
    Resource alicePageResource;

    @Mock
    ValueMap alicePageValueMap;

    @InjectMocks
    EmployeePageServiceImpl employeePageServiceImpl;

    @BeforeEach
    void setup() throws LoginException {
        lenient().when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        lenient().when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
    }

    @Test
    void getEmployeeTitle() throws LoginException {
        when(pageManager.getPage("/content/company/employees/sibi")).thenReturn(page);
        when(page.getTitle()).thenReturn("Sibi");
        assertEquals("Sibi", employeePageServiceImpl.getEmployeeTitle("/content/company/employees/sibi"));
    }

    @Test
    void getEmptyEmployeeTitle() throws LoginException {
        when(pageManager.getPage("/content/company/employees/sibi")).thenReturn(page);
        when(page.getTitle()).thenReturn("");
        assertEquals("", employeePageServiceImpl.getEmployeeTitle("/content/company/employees/sibi"));
    }

    @Test
    void testPageManagerNull() throws LoginException {
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(null);
        assertNull(employeePageServiceImpl.getEmployeeTitle(anyString()));
    }

    @Test
    void testPageNull() throws LoginException {
        when(pageManager.getPage(anyString())).thenReturn(null);
        assertNull(employeePageServiceImpl.getEmployeeTitle(anyString()));
    }

    @Test
    void testLoginException() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        assertNull(employeePageServiceImpl.getEmployeeTitle(anyString()));
    }

    @Test
    void getChildPageTitles() throws LoginException {
        when(pageManager.getPage("/content/company/employees")).thenReturn(page);
        when(sibiPage.getTitle()).thenReturn("Sibi");
        when(johnPage.getTitle()).thenReturn("John");
        when(page.listChildren()).thenReturn(Arrays.asList(sibiPage, johnPage).iterator());
        assertEquals(List.of("Sibi", "John"), employeePageServiceImpl.getChildPageTitles("/content/company/employees"));
    }

    @Test
    void getEmployeeDepartments() throws LoginException {
        when(pageManager.getPage("/content/company/employees")).thenReturn(page);
        when(sibiPage.getTitle()).thenReturn("Sibi");
        when(sibiPage.getContentResource()).thenReturn(sibiPageResource);
        when(sibiPageResource.getValueMap()).thenReturn(sibiPageValueMap);
        when(sibiPageValueMap.get("department", String.class)).thenReturn("Engineering");
        when(johnPage.getTitle()).thenReturn("John");
        when(johnPage.getContentResource()).thenReturn(johnPageResource);
        when(johnPageResource.getValueMap()).thenReturn(johnPageValueMap);
        when(johnPageValueMap.get("department", String.class)).thenReturn("HR");
        when(alicePage.getTitle()).thenReturn("Alice");
        when(alicePage.getContentResource()).thenReturn(alicePageResource);
        when(alicePageResource.getValueMap()).thenReturn(alicePageValueMap);
        when(alicePageValueMap.get("department", String.class)).thenReturn("Finance");
        when(page.listChildren()).thenReturn(Arrays.asList(sibiPage, johnPage, alicePage).iterator());
        assertEquals(Map.of("Sibi", "Engineering", "John", "HR", "Alice", "Finance"), employeePageServiceImpl.getEmployeeDepartments());
    }

    @Test
    void shouldReturnEmptyMap() throws LoginException {
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(null);
        Map<String,String> departments =
                employeePageServiceImpl.getEmployeeDepartments();
        assertTrue(departments.isEmpty());
    }

    @Test
    void nullPageShouldReturnEmptyMap() throws LoginException {
        when(pageManager.getPage("/content/company/employees")).thenReturn(null);
        Map<String,String> departments =
                employeePageServiceImpl.getEmployeeDepartments();
        assertTrue(departments.isEmpty());
    }

    @Test
    void shouldHandleMissingContentResource() throws LoginException {
        when(pageManager.getPage("/content/company/employees")).thenReturn(page);
        when(sibiPage.getTitle()).thenReturn("Sibi");
        when(sibiPage.getContentResource()).thenReturn(null);
        when(johnPage.getTitle()).thenReturn("John");
        when(johnPage.getContentResource()).thenReturn(johnPageResource);
        when(johnPageResource.getValueMap()).thenReturn(johnPageValueMap);
        when(johnPageValueMap.get("department", String.class)).thenReturn("HR");
        when(alicePage.getTitle()).thenReturn("Alice");
        when(alicePage.getContentResource()).thenReturn(alicePageResource);
        when(alicePageResource.getValueMap()).thenReturn(alicePageValueMap);
        when(alicePageValueMap.get("department", String.class)).thenReturn("Finance");
        when(page.listChildren()).thenReturn(Arrays.asList(sibiPage, johnPage, alicePage).iterator());
        Map<String,String> departments =
                employeePageServiceImpl.getEmployeeDepartments();
        assertEquals(3, departments.size());
        assertNull(departments.get("Sibi"));
        verify(resourceResolver).close();
    }

    @Test
    void loginExceptionShouldReturnEmptyMap() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        Map<String,String> departments =
                employeePageServiceImpl.getEmployeeDepartments();
        assertTrue(departments.isEmpty());
    }

}
