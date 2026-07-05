package com.sibi.aem.one.core.learnings;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

@Component(service = EmployeeSearchService.class)
public class EmployeeSearchServiceImpl
        implements EmployeeSearchService {

    @Reference
    QueryBuilder queryBuilder;

    @Reference
    ResourceResolverFactory resolverFactory;

    public static final Logger logger =  LoggerFactory.getLogger(EmployeeSearchServiceImpl.class);

    @Override
    public List<String> getEmployeeNames() {

        List<String> names = new ArrayList<>();

        try(ResourceResolver resolver =
                    resolverFactory.getServiceResourceResolver(Collections.emptyMap())) {

            Session session =
                    resolver.adaptTo(Session.class);

            Map<String,String> map =
                    new HashMap<>();

            map.put("path",
                    "/content/company/employees");

            map.put("type",
                    "nt:unstructured");

            Query query =
                    queryBuilder.createQuery(
                            PredicateGroup.create(map),
                            session);

            SearchResult result =
                    query.getResult();

            for(Hit hit : result.getHits()) {

                Resource resource =
                        hit.getResource();
                ValueMap vm = resource.getValueMap();

                String name = vm.get("name", String.class);

                if(StringUtils.isNotBlank(name)){
                    names.add(name);
                }
            }

        } catch(Exception e) {
        }

        return names;
    }

    @Override
    public List<Employee> getEmployees() {
        List<Employee> names = new ArrayList<>();
        try(ResourceResolver resolver =
                    resolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            Session session = resolver.adaptTo(Session.class);
            Map<String,String> map = new HashMap<>();
            map.put("path", "/content/company/employees");
            map.put("type", "nt:unstructured");
            Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();
            for(Hit hit : result.getHits()) {
                Resource resource = hit.getResource();
                ValueMap vm = resource.getValueMap();
                String name = vm.containsKey("name") ? vm.get("name", String.class) : null;
                String department = vm.containsKey("department") ? vm.get("department", String.class) : null;
                if(StringUtils.isNotBlank(name)){
                    names.add(new Employee(name, department));
                }
            }
        } catch(LoginException | RepositoryException e) {
            logger.error("inside exception for getEmployees");
        }
        return names;
    }
}