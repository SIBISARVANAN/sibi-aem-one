package com.sibi.aem.one.core.learnings;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collections;

@Component(service = EmployeeServiceTwo.class)
public class EmployeeServiceTwoImpl implements EmployeeServiceTwo {

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public String getDepartment(String path) {
        try(ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            Resource resource = resourceResolver.getResource(path);
            if(resource == null){
                return null;
            }
            return resource.getValueMap().get("department", String.class);
        } catch (LoginException e) {
            return null;
        }
    }
}
