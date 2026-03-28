package com.sibi.aem.one.core.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(property = { "service.ranking:Integer=-200" })
@SlingServletFilter(
        scope   = { SlingServletFilterScope.REQUEST },
        pattern = "/content/sibi-aem-one/secure/.*",
        methods = { "GET", "POST" }
)
public class AuthTokenFilter implements Filter {

//    @Reference
//    private TokenValidationService tokenService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        SlingHttpServletRequest slingRequest  = (SlingHttpServletRequest)  request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        String token = slingRequest.getHeader("X-Auth-Token");

        if (token == null || !isValid(token)) {
            slingResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid token");
            return; // ← do NOT call chain.doFilter — block the request
        }

        chain.doFilter(request, response); // token valid, proceed
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}

    private boolean isValid(String token) {
        // write code for token validation or write the validation logic in a service and call that service here
        return true;
    }
}