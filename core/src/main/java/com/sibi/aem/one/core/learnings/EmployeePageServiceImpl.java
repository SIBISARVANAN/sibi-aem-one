package com.sibi.aem.one.core.learnings;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = EmployeePageService.class)
public class EmployeePageServiceImpl implements EmployeePageService {

    @Reference
    ResourceResolverFactory resolverFactory;

    public static final Logger LOG = LoggerFactory.getLogger(EmployeePageServiceImpl.class);

    @Override
    public String getEmployeeTitle(String path) {
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                return null;
            }
            Page page = pageManager.getPage(path);
            if (page == null) {
                return null;
            }
            return page.getTitle();
        } catch (LoginException e) {
            return null;
        }
    }

    @Override
    public List<String> getChildPageTitles(String parentPath) {
        List<String> list = new ArrayList<>();
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                return list;
            }
            Page page = pageManager.getPage(parentPath);
            if (page == null) {
                return list;
            }
            Iterator<Page> children = page.listChildren();
            while (children.hasNext()) {
                Page childPage = children.next();
                list.add(childPage.getTitle());
            }
            return list;
        } catch (LoginException e) {
            LOG.error("Login Exception", e);
        }
        return list;
    }

    @Override
    public Map<String, String> getEmployeeDepartments() {
        Map<String, String> details = new HashMap<>();
        String parentPath = "/content/company/employees";
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(Collections.emptyMap())) {
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                return details;
            }
            Page page = pageManager.getPage(parentPath);
            if (page == null) {
                return details;
            }
            Iterator<Page> children = page.listChildren();
            while (children.hasNext()) {
                Page childPage = children.next();
                Resource pageContent = childPage.getContentResource();
                if (pageContent == null) {
                    details.put(childPage.getTitle(), null);
                } else {
                    ValueMap valueMap = pageContent.getValueMap();
                    details.put(childPage.getTitle(), valueMap.get("department", String.class));
                }
            }
            return details;
        } catch (LoginException e) {
            LOG.error("Login Exception", e);
        }
        return details;
    }
}