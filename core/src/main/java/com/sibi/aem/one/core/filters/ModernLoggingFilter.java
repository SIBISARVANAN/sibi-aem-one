package com.sibi.aem.one.core.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

@Component(property = {
        "service.ranking:Integer=-800"   // runs early — higher priority
})
@SlingServletFilter(
        scope         = { SlingServletFilterScope.REQUEST },  // filter scope
        pattern       = "/content/sibi-aem-one/.*",                 // regex path pattern
        resourceTypes = { "sibi-aem-one/components/page" },         // resource type match
        selectors     = { "print", "mobile" },                // selector match
        extensions    = { "html", "json" },                   // extension match
        methods       = { "GET", "POST", "HEAD" }             // HTTP method match
)
public class ModernLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // runs once when filter is registered
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        SlingHttpServletRequest  slingRequest  = (SlingHttpServletRequest)  request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        String path     = slingRequest.getRequestPathInfo().getResourcePath();
        String selector = slingRequest.getRequestPathInfo().getSelectorString();

        log.info("Incoming request | path={} | selector={}", path, selector);

        // PRE-processing — runs BEFORE the request reaches the servlet/component

        chain.doFilter(request, response); // ← MUST call this or request is blocked

        // POST-processing — runs AFTER the response is generated
        log.info("Response status: {}", slingResponse.getStatus());
    }

    @Override
    public void destroy() {
        // cleanup on filter deregistration
    }
}