package com.sibi.aem.one.core.servlets;

import com.sibi.aem.one.core.services.GoogleRecaptchaConfigService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.caconfig.annotation.Property;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/sibi-aem-one/services/getDetails")
public class PathRegistrationServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 2L;

    private static final Logger LOG = LoggerFactory.getLogger(PathRegistrationServlet.class);

    @Reference(target = "implb")
    GoogleRecaptchaConfigService gcs;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response){
        doPost(request, response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,  SlingHttpServletResponse response){
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write("{\"status\":\"ok\"}");
        } catch (IOException e) {
            LOG.error("inside exception when writing to response - {}", e);
        }
    }
}
