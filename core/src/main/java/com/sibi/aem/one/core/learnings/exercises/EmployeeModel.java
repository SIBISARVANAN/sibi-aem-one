package com.sibi.aem.one.core.learnings.exercises;

import com.day.cq.wcm.api.Page;
import com.sibi.aem.one.core.learnings.EmployeeService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.List;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class EmployeeModel {

    @ValueMapValue
    private String name;

    @ValueMapValue
    private String department;

    @ValueMapValue
    private String employeeId;

    @ValueMapValue
    private String designation;

    @ChildResource
    private ManagerModel manager;

    @ChildResource
    private List<CertificationModel> certifications;

    @OSGiService
    private EmployeeService employeeService;

    @ScriptVariable
    Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @PostConstruct
    protected void init() {
        if(designation == null){
            designation = "Developer";
        }
    }

    public String getName() {
        return name;
    }

    public String getFormattedName() {
        return employeeService.formatName(name);
    }

    public String getDepartment() {
        return department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDesignation() {
        return designation;
    }

    public ManagerModel getManager() {
        return manager;
    }

    public List<CertificationModel> getCertifications() {
        return certifications;
    }

    public String getPageTitle() {
        return currentPage.getTitle();
    }

    public String getSiteName() {
        Resource resource = resourceResolver.getResource("/content/sibi-aem-one");
        if (resource == null) {
            return null;
        }
        Page page = resource.adaptTo(Page.class);
        return page != null ? page.getTitle() : null;
    }
}
