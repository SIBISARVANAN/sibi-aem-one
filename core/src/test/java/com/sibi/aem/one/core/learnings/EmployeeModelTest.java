package com.sibi.aem.one.core.learnings;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(AemContextExtension.class)
public class EmployeeModelTest {

    final AemContext context = new AemContext();

    @BeforeEach
    void setup() {

        context.addModelsForClasses(EmployeeModel.class);

        context.create().resource(
                "/content/employee",
                "name", "Sibi",
                "department", "Engineering");
    }

    @Test
    void getName() {
        Resource resource =
                context.resourceResolver()
                        .getResource("/content/employee");
        EmployeeModel model =
                resource.adaptTo(EmployeeModel.class);
        assertNotNull(model);
        assertEquals("Sibi", model.getName());
    }

    @Test
    void getDepartment() {
        Resource resource =
                context.resourceResolver()
                        .getResource("/content/employee");
        EmployeeModel model = resource.adaptTo(EmployeeModel.class);
        assertNotNull(model);
        assertEquals("Engineering", model.getDepartment());
    }
}
