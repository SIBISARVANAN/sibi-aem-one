package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeSchedulerTest {

    @Mock
    EmployeeCleanupService employeeCleanupService;

    @Mock
    EmployeeSchedulerConfig  config;

    @InjectMocks
    EmployeeScheduler employeeScheduler;

    @Test
    void testRun()
    {
        when(config.schedulerName()).thenReturn("Test");
        when(config.enabled()).thenReturn(true);
        employeeScheduler.activate(config);
        employeeScheduler.run();
        verify(employeeCleanupService).cleanup();
    }

    @Test
    void testDontRun()
    {
        when(config.schedulerName()).thenReturn("Test");
        when(config.enabled()).thenReturn(false);
        employeeScheduler.activate(config);
        employeeScheduler.run();
        verify(employeeCleanupService, never()).cleanup();
    }

    @Test
    void testRunThrowsException() throws NoSuchFieldException, IllegalAccessException {
        when(config.schedulerName()).thenReturn("Test");
        when(config.enabled()).thenReturn(true);
        employeeScheduler.activate(config);
        doThrow(new RuntimeException("Failure"))
                .when(employeeCleanupService)
                .cleanup();
        assertDoesNotThrow(() -> employeeScheduler.run());
        verify(employeeCleanupService).cleanup();
    }
}
