package com.sibi.aem.one.core.learnings;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(service = Servlet.class)
public class EmployeeFetchServlet extends SlingSafeMethodsServlet {

    @Reference
    EmployeeService employeeService;

    @Reference
    EmployeeValidationService employeeValidationService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String pathParam = request.getParameter("path");
        if(StringUtils.isNotBlank(pathParam)){
            EmployeeInfo employeeInfo = employeeService.getEmployeeInfo(pathParam);
            if(employeeInfo != null && employeeValidationService.validateEmployee(employeeInfo)){
                response.setStatus(HttpServletResponse.SC_OK);
                JsonObject json = new JsonObject();
                json.addProperty("name", employeeInfo.getName());
                json.addProperty("department", employeeInfo.getDepartment());
                json.addProperty("employeeId", employeeInfo.getEmployeeId());
                response.setContentType("application/json");
                response.getWriter().write(json.toString());
            } else{
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
