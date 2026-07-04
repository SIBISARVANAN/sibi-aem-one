package com.sibi.aem.one.core.learnings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
public class EmployeeServletTest {

    private final AemContext context = new AemContext();

    private EmployeeServlet employeeServlet;

    private EmployeeService employeeService;

    @BeforeEach
    void setup() {
        employeeServlet = new EmployeeServlet();
        employeeService = mock(EmployeeService.class);
    }

    @Test
    void doGet1() throws IOException, NoSuchFieldException, IllegalAccessException {
        context.request().addRequestParameter("name", "Sibi");
        context.registerService(EmployeeService.class, employeeService);

        Field field = EmployeeServlet.class.getDeclaredField("employeeService");
        field.setAccessible(true);
        field.set(employeeServlet, employeeService);
        employeeServlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
        assertTrue(context.response().getOutputAsString().contains("Sibi"));
        assertFalse(context.response().getOutputAsString().contains("Guest"));
    }

    @Test
    void doGet2() throws IOException, NoSuchFieldException, IllegalAccessException {
        context.registerService(EmployeeService.class, employeeService);

        Field field = EmployeeServlet.class.getDeclaredField("employeeService");
        field.setAccessible(true);
        field.set(employeeServlet, employeeService);
        employeeServlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
        assertTrue(context.response().getOutputAsString().contains("Guest"));
    }

    @Test
    void shouldReturnRequestedName() throws IOException, NoSuchFieldException, IllegalAccessException {

        context.request().addRequestParameter("name", "Sibi");
        context.registerService(EmployeeService.class, employeeService);

        Field field = EmployeeServlet.class.getDeclaredField("employeeService");
        field.setAccessible(true);
        field.set(employeeServlet, employeeService);
        employeeServlet.doGet(context.request(), context.response());

        assertEquals("application/json", context.response().getContentType());

        String response = context.response().getOutputAsString();

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("Sibi", json.get("name").getAsString());
    }

    @Test
    void shouldReturnRequestedFormattedName() throws IOException, NoSuchFieldException, IllegalAccessException {
        context.request().addRequestParameter("name", "Sibi");
        when(employeeService.formatName("Sibi")).thenReturn("Employee : Sibi");
        context.registerService(EmployeeService.class, employeeService);

        Field field = EmployeeServlet.class.getDeclaredField("employeeService");
        field.setAccessible(true);
        field.set(employeeServlet, employeeService);

        employeeServlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
        String response = context.response().getOutputAsString();
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertEquals("Sibi", json.get("name").getAsString());
        assertEquals("Employee : Sibi", json.get("formattedName").getAsString());
    }
}
