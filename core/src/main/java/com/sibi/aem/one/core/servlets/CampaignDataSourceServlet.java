package com.sibi.aem.one.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.framework.Constants;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Dynamic Campaign Category Datasource",
                "sling.servlet.resourceTypes=sibi-aem-one/datasource/campaigns",
                "sling.servlet.methods=GET"
        }
)
public class CampaignDataSourceServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> dropDownOptions = new ArrayList<>();

        Map<String, String> campaignData = new LinkedHashMap<>();
        campaignData.put("summer_sale", "Summer Flash Sale");
        campaignData.put("winter_clearance", "Winter Clearance");
        campaignData.put("b2b_expo", "B2B Annual Expo");

        for (Map.Entry<String, String> entry : campaignData.entrySet()) {
            ValueMap vm = new ValueMapDecorator(new HashMap<>());
            vm.put("value", entry.getKey());
            vm.put("text", entry.getValue());

            dropDownOptions.add(new ValueMapResource(
                    resolver,
                    new ResourceMetadata(),
                    "nt:unstructured",
                    vm
            ));
        }

        DataSource dataSource = new SimpleDataSource(dropDownOptions.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }
}