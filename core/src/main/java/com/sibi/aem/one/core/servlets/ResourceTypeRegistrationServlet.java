package com.sibi.aem.one.core.servlets;

import com.sibi.aem.one.core.services.GoogleRecaptchaConfigService;
import com.sibi.aem.one.core.services.RunModeService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "sibi-aem-one/services/getDetails", methods = {HttpConstants.METHOD_GET, HttpConstants.METHOD_POST}, selectors = "data", extensions = "json")
public class ResourceTypeRegistrationServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceTypeRegistrationServlet.class);

    @Reference
    RunModeService rms;

    @Reference(target = "impla")
    GoogleRecaptchaConfigService  gcs;

    @Override
    protected void doGet(SlingHttpServletRequest request,  SlingHttpServletResponse response){
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
