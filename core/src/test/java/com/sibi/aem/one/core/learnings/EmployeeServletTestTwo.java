package com.sibi.aem.one.core.learnings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
public class EmployeeServletTestTwo {

    private final AemContext context = new AemContext();

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeServlet employeeServlet;

    @BeforeEach
    public void setUp() {
        lenient().when(employeeService.formatName("Sibi")).thenReturn("Employee : Sibi");
        lenient().when(employeeService.formatName("Guest")).thenReturn("Employee : Guest");
    }

    @Test
    void doGet() throws IOException {
        context.request().addRequestParameter("name", "Sibi");
        employeeServlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
        String response = context.response().getOutputAsString();
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertEquals("Sibi", json.get("name").getAsString());
        assertEquals("Employee : Sibi", json.get("formattedName").getAsString());
        context.request().removeAttribute("name");
    }

    @Test
    void doGet2() throws IOException {
        employeeServlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
        String response = context.response().getOutputAsString();
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertEquals("Guest", json.get("name").getAsString());
        assertEquals("Employee : Guest", json.get("formattedName").getAsString());
    }
}
