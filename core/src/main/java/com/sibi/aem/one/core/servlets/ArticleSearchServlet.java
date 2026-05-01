package com.sibi.aem.one.core.servlets;


import com.google.gson.Gson;
import com.sibi.aem.one.core.services.search.ArticleSearchRequest;
import com.sibi.aem.one.core.services.search.ArticleSearchResult;
import com.sibi.aem.one.core.services.search.ArticleSearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP endpoint that exposes ArticleSearchService.
 *
 * GET /bin/mysite/articles/search.json
 *   ?category=technology,sports
 *   &author=alice,bob
 *   &tag=mysite:topic/aem,mysite:topic/cloud
 *   &publishedAfter=2024-01-01T00:00:00.000%2B05:30
 *   &publishedBefore=2024-12-31T23:59:59.000%2B05:30
 *   &q=query+builder+api
 *   &featured=true
 *   &page=0
 *   &pageSize=10
 *   &sortBy=jcr:content/publishDate
 *   &sortOrder=desc
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/mysite/articles/search",
                "sling.servlet.extensions=json",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
public class ArticleSearchServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    private transient ArticleSearchService articleSearchService;

    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws ServletException, IOException {

        // --- Parse request params ---
        List<String> categories  = parseList(request.getParameter("category"));
        List<String> authors     = parseList(request.getParameter("author"));
        List<String> tags        = parseList(request.getParameter("tag"));
        String publishedAfter    = request.getParameter("publishedAfter");
        String publishedBefore   = request.getParameter("publishedBefore");
        String fullText          = request.getParameter("q");
        boolean featuredOnly     = "true".equalsIgnoreCase(request.getParameter("featured"));
        int pageNum              = parseIntOrDefault(request.getParameter("page"),     0);
        int pageSize             = parseIntOrDefault(request.getParameter("pageSize"), 10);
        String sortBy            = StringUtils.defaultIfBlank(
                request.getParameter("sortBy"),
                "jcr:content/publishDate");
        String sortOrder         = StringUtils.defaultIfBlank(
                request.getParameter("sortOrder"), "desc");

        // --- Build request DTO ---
        ArticleSearchRequest searchRequest = ArticleSearchRequest.builder()
                .rootPath("/content/mysite/en")
                .categories(categories)
                .authors(authors)
                .tags(tags)
                .publishedAfter(publishedAfter)
                .publishedBefore(publishedBefore)
                .fullTextQuery(fullText)
                .featuredOnly(featuredOnly)
                .excludeArchived(true)
                .page(pageNum, pageSize)
                .sortBy(sortBy, sortOrder)
                .build();

        // --- Execute search ---
        ArticleSearchResult result = articleSearchService.search(
                searchRequest, request.getResourceResolver());

        // --- Write JSON response ---
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(SlingHttpServletResponse.SC_OK);
        response.getWriter().write(GSON.toJson(result));
    }

    // --- Helpers ---

    private List<String> parseList(String param) {
        if (StringUtils.isBlank(param)) return null;
        return Arrays.asList(param.split(","));
    }

    private int parseIntOrDefault(String param, int defaultVal) {
        try {
            return StringUtils.isNotBlank(param)
                    ? Integer.parseInt(param.trim())
                    : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
