package com.sibi.aem.one.core.servlets;

import com.google.gson.Gson;
import com.sibi.aem.one.core.services.search.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Property Search JSON endpoint.
 *
 * URL pattern: GET /content/sibi-aem-one/en/search.search.json
 *
 * This servlet wires the HTTP layer to PropertySearchService.
 * All index selection happens transparently inside the service.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "sibi-aem-one/components/page/searchpage",
        methods       = HttpConstants.METHOD_GET,
        selectors     = "search",
        extensions    = "json"
)
public class PropertySearchServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private PropertySearchService searchService;

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) throws IOException {

        PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                .fullText(request.getParameter("q"))
                .propertyTypes(splitParam(request.getParameter("types")))
                .statuses(splitParam(request.getParameter("statuses")))
                .priceMin(parseDouble(request.getParameter("priceMin")))
                .priceMax(parseDouble(request.getParameter("priceMax")))
                .amenityTags(splitParam(request.getParameter("tags")))
                .featuredOnly("true".equals(request.getParameter("featured")))
                .sortBy(
                    StringUtils.defaultIfBlank(
                        request.getParameter("sortBy"), "jcr:content/price"),
                    StringUtils.defaultIfBlank(
                        request.getParameter("sortOrder"), "asc")
                )
                .page(
                    parseIntOrDefault(request.getParameter("page"), 0),
                    parseIntOrDefault(request.getParameter("pageSize"), 12)
                )
                .build();

        PropertySearchResult result = searchService.searchProperties(
                searchRequest, request.getResourceResolver());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new Gson().toJson(result));
    }

    private List<String> splitParam(String param) {
        if (StringUtils.isBlank(param)) return Collections.emptyList();
        return Arrays.asList(param.split(","));
    }

    private Double parseDouble(String val) {
        try { return val != null ? Double.parseDouble(val) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private int parseIntOrDefault(String val, int def) {
        try { return val != null ? Integer.parseInt(val) : def; }
        catch (NumberFormatException e) { return def; }
    }
}
