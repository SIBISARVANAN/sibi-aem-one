package com.sibi.aem.one.core.learnings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class EmployeeModel {

    @ValueMapValue
    private String name;

    @ValueMapValue
    private String department;

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }
}
