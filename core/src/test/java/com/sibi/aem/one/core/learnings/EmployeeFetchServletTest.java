package com.sibi.aem.one.core.learnings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeFetchServletTest {

    @Mock
    EmployeeService employeeService;

    @Mock
    EmployeeValidationService employeeValidationService;

    @Mock
    SlingHttpServletRequest request;

    @Mock
    SlingHttpServletResponse response;

    @Mock
    PrintWriter printWriter;

    @InjectMocks
    EmployeeFetchServlet employeeFetchServlet;

    @Test
    void doGetTest() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo("/content/company/employees/sibi")).thenReturn(new EmployeeInfo("Sibi", "Engineering", "7"));
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(true);
        when(response.getWriter()).thenReturn(printWriter);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);
        verify(printWriter).write(captor.capture());
        String json = captor.getValue();
        assertTrue(json.contains("\"name\":\"Sibi\""));
        assertTrue(json.contains("\"department\":\"Engineering\""));
        assertTrue(json.contains("\"employeeId\":\"7\""));
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void doGetTestWithoutArgCaptor() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo("/content/company/employees/sibi")).thenReturn(new EmployeeInfo("Sibi", "Engineering", "7"));
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(true);
        when(response.getWriter()).thenReturn(printWriter);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(printWriter).write(
                "{\"name\":\"Sibi\",\"department\":\"Engineering\",\"employeeId\":\"7\"}"
        );
    }

    @Test
    void doGetTestInOrder() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo("/content/company/employees/sibi")).thenReturn(new EmployeeInfo("Sibi", "Engineering", "7"));
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(true);
        when(response.getWriter()).thenReturn(printWriter);
        employeeFetchServlet.doGet(request, response);
        InOrder inOrder = inOrder(employeeService, response, printWriter);
        inOrder.verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        inOrder.verify(response).setStatus(HttpServletResponse.SC_OK);
        inOrder.verify(response).setContentType("application/json");
        inOrder.verify(response).getWriter();
        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);
        verify(printWriter).write(captor.capture());
        inOrder.verify(printWriter).write(
                "{\"name\":\"Sibi\",\"department\":\"Engineering\",\"employeeId\":\"7\"}"
        );
        verify(response, never()).sendError(anyInt());
    }

    @Test
    void doGetTestWithArgMatcher() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo("/content/company/employees/sibi")).thenReturn(new EmployeeInfo("Sibi", "Engineering", "7"));
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenReturn(true);
        when(response.getWriter()).thenReturn(printWriter);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        verify(employeeValidationService)
                .validateEmployee(argThat(
                        employee -> employee.getEmployeeId() != null
                                && !employee.getEmployeeId().isEmpty()
                ));
        verify(employeeValidationService)
                .validateEmployee(argThat(
                        employee -> employee.getName().equals("Sibi") && employee.getDepartment().equals("Engineering")
                ));
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(printWriter).write(
                "{\"name\":\"Sibi\",\"department\":\"Engineering\",\"employeeId\":\"7\"}"
        );
    }

    @Test
    void doGetTestWithDoAnswer() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo(anyString()))
                .thenAnswer(invocation -> {
                    String path = invocation.getArgument(0);
                    if(path.endsWith("sibi")) {
                        return new EmployeeInfo("Sibi","Engineering","7");
                    }
                    return new EmployeeInfo("John","HR","10");
                });
        when(employeeValidationService.validateEmployee(any(EmployeeInfo.class))).thenAnswer(invocation -> {
            EmployeeInfo employeeInfo = invocation.getArgument(0);
            if(employeeInfo.getName().equals("Sibi")){
                return true;
            } else {
                return false;
            }
        });
        when(response.getWriter()).thenReturn(printWriter);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(printWriter).write(
                "{\"name\":\"Sibi\",\"department\":\"Engineering\",\"employeeId\":\"7\"}"
        );
    }

    @Test
    void doGetTestBlankPath() throws IOException {
        when(request.getParameter("path")).thenReturn("");
        employeeFetchServlet.doGet(request, response);
        verify(employeeService, never()).getEmployeeInfo(anyString());
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verifyNoInteractions(printWriter);
        verify(response, never()).setContentType(anyString());
        verify(response, never()).getWriter();
    }

    @Test
    void doGetTestSpacePath() throws IOException {
        when(request.getParameter("path")).thenReturn("  ");
        employeeFetchServlet.doGet(request, response);
        verify(employeeService, never()).getEmployeeInfo(anyString());
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verifyNoInteractions(printWriter);
    }

    @Test
    void doGetTestNullPath() throws IOException {
        when(request.getParameter("path")).thenReturn(null);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService, never()).getEmployeeInfo(anyString());
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verifyNoInteractions(printWriter);
    }

    @Test
    void doGetTestWithoutResource() throws IOException {
        when(request.getParameter("path")).thenReturn("/content/company/employees/sibi");
        when(employeeService.getEmployeeInfo("/content/company/employees/sibi")).thenReturn(null);
        employeeFetchServlet.doGet(request, response);
        verify(employeeService).getEmployeeInfo("/content/company/employees/sibi");
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verifyNoInteractions(printWriter);
    }

}
