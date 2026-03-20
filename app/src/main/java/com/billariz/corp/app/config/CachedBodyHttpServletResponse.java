/**
 * Copyright (C) 2025 Uppli SAS — Billariz
 *
 * This file is part of Billariz, licensed under the GNU Affero General
 * Public License v3.0 (AGPL-3.0). You may use, modify and distribute
 * this software under the terms of the AGPL-3.0.
 *
 * For commercial use without AGPL obligations, contact:
 * contact@billariz.com | contact@uppli.fr
 * https://billariz.com
 */

package com.billariz.corp.app.config;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private PrintWriter writer;
    private boolean writerUsed = false;
    private boolean outputStreamUsed = false;
    private int capturedStatus = SC_OK;

    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.capturedStatus = sc; // Capture the status code
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);
        this.capturedStatus = sc; // Capture the status code
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);
        this.capturedStatus = sc; // Capture the status code
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writerUsed) {
            throw new IllegalStateException("getWriter() has already been called for this response");
        }
        outputStreamUsed = true;
        return new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(javax.servlet.WriteListener listener) {
            }
        };
    }

    @Override
    public PrintWriter getWriter() {
        if (outputStreamUsed) {
            throw new IllegalStateException("getOutputStream() has already been called for this response");
        }
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        }
        writerUsed = true;
        return writer;
    }

    public String getCapturedBody() {
        if (writer != null) {
            writer.flush();
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public int getCapturedStatus(){
        return capturedStatus;
    }

    public void copyBodyToResponse() throws IOException {
        if (writerUsed) {
            PrintWriter responseWriter = super.getWriter();
            responseWriter.write(getCapturedBody());
            responseWriter.flush();
        } else if (outputStreamUsed) {
            ServletOutputStream responseOutputStream = super.getOutputStream();
            buffer.writeTo(responseOutputStream);
            responseOutputStream.flush();
        }
    }
}