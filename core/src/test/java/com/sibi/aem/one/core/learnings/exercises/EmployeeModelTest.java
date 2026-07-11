package com.sibi.aem.one.core.learnings.exercises;

import com.day.cq.wcm.api.Page;
import com.sibi.aem.one.core.learnings.EmployeeService;
import com.sibi.aem.one.core.learnings.EmployeeServiceImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
public class EmployeeModelTest {

    private final AemContext context = new AemContext();

    @Test
    void test() {
        context.addModelsForClasses(EmployeeModel.class);
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7", "designation", "SSE");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        EmployeeModel employeeModel = resource.adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("SSE", employeeModel.getDesignation());
    }

    @Test
    void testWithoutDesignation() {
        context.addModelsForClasses(EmployeeModel.class);
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        EmployeeModel employeeModel = resource.adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("Developer", employeeModel.getDesignation());
    }

    @Test
    void testWithManager() {
        context.addModelsForClasses(EmployeeModel.class);
        context.addModelsForClasses(ManagerModel.class);
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7", "designation", "SSE");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/manager", "name", "John", "department", "Engineering");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        EmployeeModel employeeModel = resource.adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("SSE", employeeModel.getDesignation());
        assertEquals("John", employeeModel.getManager().getName());
        assertEquals("Engineering", employeeModel.getManager().getDepartment());
    }

    @Test
    void testWithManagerCertification() {
        context.addModelsForClasses(EmployeeModel.class);
        context.addModelsForClasses(ManagerModel.class);
        context.addModelsForClasses(CertificationModel.class);
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7", "designation", "SSE");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/manager", "name", "John", "department", "Engineering");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/certifications");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/certifications/item0", "name", "Azure");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/certifications/item1", "name", "Java");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee/certifications/item2", "name", "AEM");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        EmployeeModel employeeModel = resource.adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("SSE", employeeModel.getDesignation());
        assertEquals("John", employeeModel.getManager().getName());
        assertEquals("Engineering", employeeModel.getManager().getDepartment());
        assertEquals("Azure", employeeModel.getCertifications().get(0).getName());
        assertEquals("Java", employeeModel.getCertifications().get(1).getName());
        assertEquals("AEM", employeeModel.getCertifications().get(2).getName());
    }

    @Test
    void testWithServicePage() {
        context.addModelsForClasses(EmployeeModel.class);
        context.registerService(EmployeeService.class, new EmployeeServiceImpl());
        Page page = context.create().page("/content/company/employees/sibi", "/conf/sibi-aem-one/settings/wcm/templates/employee-details", "Sibi Sarvanan");
        context.currentPage(page);
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7", "designation", "SSE");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        context.currentResource(resource);
        EmployeeModel employeeModel = context.request().adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("SSE", employeeModel.getDesignation());
        assertEquals("Employee : Sibi", employeeModel.getFormattedName());
        assertEquals("Sibi Sarvanan", employeeModel.getPageTitle());
    }

    @Test
    void testWithRR() {
        context.addModelsForClasses(EmployeeModel.class);
        Page page = context.create().page(
                "/content/sibi-aem-one",
                "/conf/test/templates/home",
                "SIBI-AEM-ONE");
        context.create().resource("/content/company/employees/sibi/jcr:content/employee", "name", "Sibi", "department", "Engineering", "employeeId", "7", "designation", "SSE");
        Resource resource = context.resourceResolver().getResource("/content/company/employees/sibi/jcr:content/employee");
        EmployeeModel employeeModel = resource.adaptTo(EmployeeModel.class);
        assertEquals("Sibi", employeeModel.getName());
        assertEquals("Engineering", employeeModel.getDepartment());
        assertEquals("7", employeeModel.getEmployeeId());
        assertEquals("SSE", employeeModel.getDesignation());
        assertEquals("SIBI-AEM-ONE", employeeModel.getSiteName());
    }

}
