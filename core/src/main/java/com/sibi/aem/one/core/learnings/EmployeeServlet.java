package com.sibi.aem.one.core.learnings;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/employee/details")
public class EmployeeServlet extends SlingSafeMethodsServlet {

    @Reference
    EmployeeService employeeService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String name = StringUtils.isNotBlank(request.getParameter("name")) ? request.getParameter("name") : "Guest";
        String formattedName = employeeService.formatName(name);
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("formattedName", formattedName);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}