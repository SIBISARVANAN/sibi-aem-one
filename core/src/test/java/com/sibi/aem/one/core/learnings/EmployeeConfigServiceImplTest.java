package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmployeeConfigServiceImplTest {

    @Mock
    EmployeeConfiguration employeeConfiguration;

    @InjectMocks
    EmployeeConfigServiceImpl employeeConfigServiceImpl;

    @Test
    public void testEmployeeConfigService1() {
        when(employeeConfiguration.companyName()).thenReturn("Microsoft");
        when(employeeConfiguration.maxEmployees()).thenReturn(500);
        when(employeeConfiguration.country()).thenReturn("USA");
        employeeConfigServiceImpl.activate(employeeConfiguration);
        assertEquals("Microsoft", employeeConfigServiceImpl.getCompanyName());
        assertEquals(500, employeeConfigServiceImpl.getMaxEmployees());
        assertEquals("USA", employeeConfigServiceImpl.getCountry());
    }

    @Test
    public void testEmployeeConfigService2() {
        when(employeeConfiguration.companyName()).thenReturn("Adobe");
        when(employeeConfiguration.maxEmployees()).thenReturn(100);
        when(employeeConfiguration.country()).thenReturn("India");
        employeeConfigServiceImpl.activate(employeeConfiguration);
        assertEquals("Adobe", employeeConfigServiceImpl.getCompanyName());
        assertEquals(100, employeeConfigServiceImpl.getMaxEmployees());
        assertEquals("India", employeeConfigServiceImpl.getCountry());
    }

    @Test
    public void testEmployeeConfigService3() throws NoSuchFieldException, IllegalAccessException {
        when(employeeConfiguration.companyName()).thenReturn("Adobe");
        when(employeeConfiguration.maxEmployees()).thenReturn(100);
        when(employeeConfiguration.country()).thenReturn("India");
        employeeConfigServiceImpl.activate(employeeConfiguration);
        Field companyName = EmployeeConfigServiceImpl.class.getDeclaredField("companyName");
        companyName.setAccessible(true);
        companyName.set(employeeConfigServiceImpl, "Google");
        assertEquals("Google", employeeConfigServiceImpl.getCompanyName());
        assertEquals(100, employeeConfigServiceImpl.getMaxEmployees());
        assertEquals("India", employeeConfigServiceImpl.getCountry());
    }
}
