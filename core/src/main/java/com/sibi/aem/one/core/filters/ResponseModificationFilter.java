package com.sibi.aem.one.core.filters;

import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@SlingServletFilter(
        scope      = { SlingServletFilterScope.REQUEST },
        pattern    = "/content/sibi-aem-one/.*",
        extensions = { "html" },
        methods    = { "GET" }
)
public class ResponseModificationFilter implements Filter {

    private static final Logger log =
            LoggerFactory.getLogger(ResponseModificationFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // wrap the real response to buffer the output
        BufferedHttpResponseWrapper wrappedResponse =
                new BufferedHttpResponseWrapper((HttpServletResponse) response);

        // let the servlet/component render into the buffer
        chain.doFilter(request, wrappedResponse);

        // read the buffered HTML
        String html = wrappedResponse.getBufferedContent();

        if (html == null || html.isEmpty()) {
            log.warn("Buffered response is empty — writing original response");
            return;
        }

        // modify — inject a script tag before </body>
        String modified = html.replace("</body>",
                "<script src='/etc/clientlibs/sibi-aem-one/tracking.js'></script></body>");

        // set correct content length after modification
        response.setContentLength(modified.getBytes(response.getCharacterEncoding()).length);

        // write modified HTML to the REAL response
        response.getWriter().write(modified);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}