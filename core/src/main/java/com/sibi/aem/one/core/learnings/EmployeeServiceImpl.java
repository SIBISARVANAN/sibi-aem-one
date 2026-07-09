package com.sibi.aem.one.core.learnings;

import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Component(service = EmployeeService.class)
public class EmployeeServiceImpl implements EmployeeService {

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    public static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Override
    public String formatName(String name) {
        return "Employee : " + name;
    }

    @Override
    public void cleanupEmployees() {
    }

    @Override
    public EmployeeInfo getEmployeeInfo(String path) {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            Resource resource = resolver.getResource(path);
            if(resource == null) {
                LOG.error("Resource not found: {}", path);
                return null;
            }
            Resource contentResource = resource.getChild("jcr:content");
            if(contentResource == null) {
                LOG.error("Resource not found: {}", path);
                return null;
            }
            ValueMap values = contentResource.getValueMap();
            String name =  values.get("name", String.class);
            String department = values.get("department", String.class);
            String employeeId  = values.get("employeeId", String.class);
            return new EmployeeInfo(name, department, employeeId);
        } catch (LoginException e){
            LOG.error("Login Exception", e);
        }
        return null;
    }
}