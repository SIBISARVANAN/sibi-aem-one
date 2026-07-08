package com.sibi.aem.one.core.learnings;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeCleanupSchedulerTest {

    @Mock
    Scheduler scheduler;

    @Mock
    ScheduleOptions scheduleOptions;

    @Mock
    EmployeeService employeeService;

    @Mock
    EmployeeCleanupSchedulerConfig employeeCleanupSchedulerConfig;

    @InjectMocks
    EmployeeCleanupScheduler employeeCleanupScheduler;

    @Test
    void testRunEnabled() throws NoSuchFieldException, IllegalAccessException {
        when(employeeCleanupSchedulerConfig.schedulerName()).thenReturn("Employee Cleanup Scheduler");
        when(employeeCleanupSchedulerConfig.enabled()).thenReturn(true);
        when(employeeCleanupSchedulerConfig.schedulerCron()).thenReturn("0 0/5 * * * ?");
        when(scheduler.EXPR("0 0/5 * * * ?")).thenReturn(scheduleOptions);
        employeeCleanupScheduler.activate(employeeCleanupSchedulerConfig);
        employeeCleanupScheduler.run();
        verify(scheduler).EXPR("0 0/5 * * * ?");
        verify(scheduleOptions).name("Employee Cleanup Scheduler");
        verify(scheduler).schedule(employeeCleanupScheduler, scheduleOptions);
        verify(employeeService).cleanupEmployees();
    }

    @Test
    void testRunDisabled() throws NoSuchFieldException, IllegalAccessException {
        when(employeeCleanupSchedulerConfig.schedulerName()).thenReturn("Employee Cleanup Scheduler");
        when(employeeCleanupSchedulerConfig.enabled()).thenReturn(false);
        when(employeeCleanupSchedulerConfig.schedulerCron()).thenReturn("0 0/5 * * * ?");
        when(scheduler.EXPR("0 0/5 * * * ?")).thenReturn(scheduleOptions);
        employeeCleanupScheduler.activate(employeeCleanupSchedulerConfig);
        verify(scheduler, never()).schedule(employeeCleanupScheduler, scheduleOptions);
        verifyNoInteractions(employeeService);
        verify(scheduler).EXPR("0 0/5 * * * ?");
        verify(scheduleOptions).name("Employee Cleanup Scheduler");
    }

    @Test
    void testRunEnabledDeactivate() throws NoSuchFieldException, IllegalAccessException {
        when(employeeCleanupSchedulerConfig.schedulerName()).thenReturn("Employee Cleanup Scheduler");
        when(employeeCleanupSchedulerConfig.enabled()).thenReturn(true);
        when(employeeCleanupSchedulerConfig.schedulerCron()).thenReturn("0 0/5 * * * ?");
        when(scheduler.EXPR("0 0/5 * * * ?")).thenReturn(scheduleOptions);
        employeeCleanupScheduler.activate(employeeCleanupSchedulerConfig);
        employeeCleanupScheduler.run();
        employeeCleanupScheduler.deactivate();
        verify(scheduler).EXPR("0 0/5 * * * ?");
        verify(scheduleOptions).name("Employee Cleanup Scheduler");
        verify(employeeService).cleanupEmployees();
        verify(scheduler, times(2)).unschedule("Employee Cleanup Scheduler");
    }

    @Test
    void testRunDisabledDeactivate() throws NoSuchFieldException, IllegalAccessException {
        when(employeeCleanupSchedulerConfig.schedulerName()).thenReturn("Employee Cleanup Scheduler");
        when(employeeCleanupSchedulerConfig.enabled()).thenReturn(false);
        when(employeeCleanupSchedulerConfig.schedulerCron()).thenReturn("0 0/5 * * * ?");
        when(scheduler.EXPR("0 0/5 * * * ?")).thenReturn(scheduleOptions);
        employeeCleanupScheduler.activate(employeeCleanupSchedulerConfig);
        verify(scheduler, never()).schedule(employeeCleanupScheduler, scheduleOptions);
        verifyNoInteractions(employeeService);
        verify(scheduler).EXPR("0 0/5 * * * ?");
        verify(scheduleOptions).name("Employee Cleanup Scheduler");
    }

}
