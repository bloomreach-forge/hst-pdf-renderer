/**
 * Copyright 2012 Hippo.
 * 
 * This file is part of HST PDF Renderer.
 * 
 * HST PDF Renderer is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation, either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * HST PDF Renderer is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * HST PDF Renderer. If not, see http://www.gnu.org/licenses/.
 */
package org.onehippo.forge.hst.pdf.renderer.servlet;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentCapturingHttpServletResponse
 */
public class ContentCapturingHttpServletResponse extends HttpServletResponseWrapper {

    private static Logger log = LoggerFactory.getLogger(ContentCapturingHttpServletResponse.class);

    private static class CharArrayWriterBuffer extends CharArrayWriter {
        @Override
        public void close() {
        }

        public void closeWriter() {
            super.close();
        }
    }

    private static class ByteArrayOutputStreamBuffer extends ByteArrayOutputStream {
        @Override
        public void close() {
        }

        public void closeOutputStream() {
            try {
                super.close();
            } catch (IOException e) {
                log.error("Failed to close byte array output stream", e);
            }
        }
    }

    private CharArrayWriterBuffer charOutputBuffer;
    private PrintWriter printWriter;

    private ByteArrayOutputStreamBuffer byteOutputBuffer;
    private ServletOutputStream servletOutputStream;

    private String characterEncoding;
    private String contentType;
    private int bufferSize;
    private int contentLength;
    private boolean committed;
    private Locale locale;

    public ContentCapturingHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if (printWriter == null) {
            if (servletOutputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response");
            }

            charOutputBuffer = new CharArrayWriterBuffer();
            printWriter = new PrintWriter(charOutputBuffer);
        }

        return printWriter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        if (servletOutputStream == null) {
            if (printWriter != null) {
                throw new IllegalStateException("getWriter() has already been called on this response");
            }

            byteOutputBuffer = new ByteArrayOutputStreamBuffer();
            servletOutputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    byteOutputBuffer.write(b);
                }
            };
        }

        return servletOutputStream;
    }

    public boolean isWrittenToPrintWriter() {
        return (printWriter != null);
    }

    public char [] toCharArray() {
        if (charOutputBuffer != null) {
            return charOutputBuffer.toCharArray();
        }

        return ArrayUtils.EMPTY_CHAR_ARRAY;
    }

    public byte [] toByteArray() {
        if (byteOutputBuffer != null) {
            return byteOutputBuffer.toByteArray();
        }

        return ArrayUtils.EMPTY_BYTE_ARRAY;
    }

    public void close() {
        IOUtils.closeQuietly(servletOutputStream);

        if (byteOutputBuffer != null) {
            byteOutputBuffer.closeOutputStream();
        }

        IOUtils.closeQuietly(printWriter);

        if (charOutputBuffer != null) {
            charOutputBuffer.closeWriter();
        }
    }

    @Override
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void flushBuffer() throws IOException {
        // TODO
    }

    @Override
    public void resetBuffer() {
        // TODO
    }

    @Override
    public boolean isCommitted() {
        if (getResponse().isCommitted()) {
            return true;
        }

        return committed;
    }

    @Override
    public void reset() {
        // TODO
    }

    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}