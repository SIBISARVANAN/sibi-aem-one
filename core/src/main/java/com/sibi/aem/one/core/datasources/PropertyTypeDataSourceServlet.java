package com.sibi.aem.one.core.datasources;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom AEM servlet datasource for the "Property Type" select field.
 *
 * <p>Registered against the virtual resource type
 * {@code sibi-aem-one/datasources/propertytype} — referenced directly in the
 * dialog XML's {@code <datasource sling:resourceType="...">} node. No JCR
 * component package is needed for this resourceType; it is resolved purely
 * by Sling's servlet registration.</p>
 *
 * <p>In production, replace the static list with a call to a PIM/CMS API or
 * a JCR query against a taxonomy tree, following the same
 * {@code SimpleDataSource} construction pattern.</p>
 */
@Component(service = javax.servlet.Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "sibi-aem-one/datasources/propertytype",
        methods = HttpConstants.METHOD_GET
)
public class PropertyTypeDataSourceServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        List<Resource> options = buildOptions(request);
        // SimpleDataSource takes an Iterator<Resource> directly — no Enumeration wrapper needed.
        DataSource dataSource = new SimpleDataSource(options.iterator());
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    private List<Resource> buildOptions(SlingHttpServletRequest request) {
        List<Resource> options = new ArrayList<>();
        options.add(toResource(request, "Apartment", "apartment"));
        options.add(toResource(request, "Villa", "villa"));
        options.add(toResource(request, "Independent House", "house"));
        options.add(toResource(request, "Penthouse", "penthouse"));
        options.add(toResource(request, "Plot / Land", "plot"));
        return options;
    }

    private Resource toResource(SlingHttpServletRequest request, String text, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("value", value);
        ValueMap vm = new ValueMapDecorator(map);
        // AbstractResource has existed in org.apache.sling.api since its earliest versions —
        // unlike ValueMapResource (added ~2.16/2.17), there is no dependency-version risk here.
        return new SimpleOptionResource(request.getResourceResolver(), "/sibiaem/propertytype/" + value, vm);
    }

    /**
     * Minimal {@link Resource} implementation for a single datasource option row.
     * Overriding {@code getValueMap()} is enough — {@code AbstractResource} handles
     * {@code adaptTo(ValueMap.class)} by delegating to it automatically.
     */
    private static final class SimpleOptionResource extends AbstractResource {

        private final ResourceResolver resourceResolver;
        private final String path;
        private final ValueMap valueMap;

        SimpleOptionResource(ResourceResolver resourceResolver, String path, ValueMap valueMap) {
            this.resourceResolver = resourceResolver;
            this.path = path;
            this.valueMap = valueMap;
        }

        @Override public String getPath()              { return path; }
        @Override public String getResourceType()      { return "nt:unstructured"; }
        @Override public String getResourceSuperType()  { return null; }
        @Override public ResourceMetadata getResourceMetadata() { return new ResourceMetadata(); }
        @Override public ResourceResolver getResourceResolver()  { return resourceResolver; }
        @Override public ValueMap getValueMap()         { return valueMap; }
    }
}
