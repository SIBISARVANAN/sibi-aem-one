package com.sibi.aem.one.core.learnings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class EmployeeModelTwo {

    @ValueMapValue
    private String name;

    @OSGiService
    private EmployeeService employeeService;

    public String getFormattedName() {
        return employeeService.formatName(name);
    }
}