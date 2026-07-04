package com.sibi.aem.one.core.learnings;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
public class EmployeeModelTwoTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setup() {
        context.create().resource("/content/employee", "name", "Sibi");
        context.addModelsForClasses(EmployeeModelTwo.class);
    }

    @Test
    void getFormattedName(){
        EmployeeService employeeService = mock(EmployeeService.class);
        context.registerService(EmployeeService.class, employeeService);
        when(employeeService.formatName("Sibi")).thenReturn("Employee : Sibi");
        Resource resource  =
                context.resourceResolver()
                        .getResource("/content/employee");
        EmployeeModelTwo employeeModelTwo = resource.adaptTo(EmployeeModelTwo.class);
        assertEquals("Employee : Sibi", employeeModelTwo.getFormattedName());
        verify(employeeService).formatName(eq("Sibi"));
    }
}
