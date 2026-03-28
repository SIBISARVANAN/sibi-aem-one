package com.sibi.aem.one.core.filters;


import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class BufferedHttpResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final PrintWriter           printWriter           = new PrintWriter(byteArrayOutputStream);
    private ServletOutputStream         servletOutputStream;

    public BufferedHttpResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    // -------------------------------------------------------
    // Override getWriter() — intercepts text-based responses
    // -------------------------------------------------------

    @Override
    public PrintWriter getWriter() {
        return printWriter;
    }

    // -------------------------------------------------------
    // Override getOutputStream() — intercepts binary responses
    // -------------------------------------------------------

    @Override
    public ServletOutputStream getOutputStream() {
        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStream() {

                @Override
                public void write(int b) {
                    byteArrayOutputStream.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    // no-op for synchronous filters
                }
            };
        }
        return servletOutputStream;
    }

    // -------------------------------------------------------
    // Flush both writer and output stream before reading
    // -------------------------------------------------------

    @Override
    public void flushBuffer() throws IOException {
        printWriter.flush();
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        super.flushBuffer();
    }

    // -------------------------------------------------------
    // The method you call in your filter to get the content
    // -------------------------------------------------------

    public String getBufferedContent() throws IOException {
        printWriter.flush();
        return byteArrayOutputStream.toString(getCharacterEncoding());
    }
}