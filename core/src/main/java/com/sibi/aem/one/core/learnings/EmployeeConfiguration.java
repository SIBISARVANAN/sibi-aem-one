package com.sibi.aem.one.core.learnings;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Employee Configuration")
public @interface EmployeeConfiguration {

    @AttributeDefinition(
            name = "Company Name")
    String companyName() default "Adobe";

    @AttributeDefinition(
            name = "Max Employees")
    int maxEmployees() default 100;

    @AttributeDefinition(
            name = "Country")
    String country() default "India";

}