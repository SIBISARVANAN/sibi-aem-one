package com.sibi.aem.one.core.filters;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;

import javax.servlet.*;
import java.io.IOException;

@Component
@SlingServletFilter(
        scope   = { SlingServletFilterScope.REQUEST },
        pattern = "/content/.*",
        methods = { "GET", "HEAD" }
)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);

        // add security headers AFTER response is generated
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        slingResponse.setHeader("X-Frame-Options",        "SAMEORIGIN");
        slingResponse.setHeader("X-Content-Type-Options", "nosniff");
        slingResponse.setHeader("X-XSS-Protection",       "1; mode=block");
        slingResponse.setHeader("Referrer-Policy",         "strict-origin-when-cross-origin");
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}