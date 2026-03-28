package com.sibi.aem.one.core.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

@Component
@SlingServletFilter(
        scope   = { SlingServletFilterScope.ERROR },
        pattern = "/content/sibi-aem-one/.*"
)
public class GlobalErrorFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Uncaught error for path: {}",
                    slingRequest.getRequestPathInfo().getResourcePath(), e);
            // forward to custom error page or handle gracefully
        }
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}