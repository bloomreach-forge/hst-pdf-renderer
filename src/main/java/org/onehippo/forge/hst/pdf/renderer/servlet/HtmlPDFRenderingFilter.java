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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.onehippo.forge.hst.pdf.renderer.HtmlPDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;

/**
 * HtmlPDFRenderingFilter
 */
public class HtmlPDFRenderingFilter implements Filter {

    public static final String CSS_URI_PARAM = "css.uri";
    public static final String BUFFER_SIZE_PARAM = "buffer.size";
    public static final String USER_AGENT_CALLBACK_CLASS = "user.agent.callback.class";

    private static Logger log = LoggerFactory.getLogger(HtmlPDFRenderingFilter.class);

    private HtmlPDFRenderer pdfRenderer;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        pdfRenderer = new HtmlPDFRenderer();

        String param = StringUtils.trim(filterConfig.getInitParameter(CSS_URI_PARAM));

        if (!StringUtils.isEmpty(param)) {
            if (StringUtils.startsWith(param, "file:") || StringUtils.startsWith(param, "http:") || StringUtils.startsWith(param, "https:") || StringUtils.startsWith(param, "ftp:") || StringUtils.startsWith(param, "sftp:")) {
                pdfRenderer.setCssURI(URI.create(param));
            } else {
                File cssFile = null;

                if (StringUtils.startsWith(param, "/")) {
                    cssFile = new File(filterConfig.getServletContext().getRealPath(param));
                } else {
                    cssFile = new File(param);
                }

                if (!cssFile.isFile()) {
                    log.error("Cannot find the css file: {}", cssFile);
                } else {
                    pdfRenderer.setCssURI(cssFile.toURI());
                }
            }
        }

        param = StringUtils.trim(filterConfig.getInitParameter(BUFFER_SIZE_PARAM));

        if (!StringUtils.isEmpty(param)) {
            pdfRenderer.setBufferSize(Math.max(512, NumberUtils.toInt(param, 4096)));
        }

        param = StringUtils.trim(filterConfig.getInitParameter(USER_AGENT_CALLBACK_CLASS));

        if (!StringUtils.isEmpty(param)) {
            try {
                Class<?> userAgentCallBackClass = Thread.currentThread().getContextClassLoader().loadClass(param);

                if (!UserAgentCallback.class.isAssignableFrom(userAgentCallBackClass)) {
                    log.error("The class, '{}' is not an type of '{}'.", param, UserAgentCallback.class);
                } else {
                    pdfRenderer.setUserAgentCallback((UserAgentCallback) userAgentCallBackClass.newInstance());
                }
            } catch (Exception e) {
                log.error("Failed to set userAgentClassCallback object", e);
            }
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (!(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        if (response.isCommitted()) {
            log.warn("The servlet response is already committed for the request: '{}'.", ((HttpServletRequest) request).getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        ContentCapturingHttpServletResponse capturingResponse = null;
        InputStream htmlInputStream = null;
        Reader htmlReader = null;
        OutputStream pdfOutputStream = null;

        try {
            capturingResponse = new ContentCapturingHttpServletResponse((HttpServletResponse) response);
            chain.doFilter(request, capturingResponse);

            if (capturingResponse.isWrittenToPrintWriter()) {
                htmlReader = new CharArrayReader(capturingResponse.toCharArray());
            } else {
                htmlInputStream = new ByteArrayInputStream(capturingResponse.toByteArray());
                String characterEncoding = StringUtils.defaultIfBlank(capturingResponse.getCharacterEncoding(), "UTF-8");
                htmlReader = new InputStreamReader(htmlInputStream, characterEncoding);
            }

            capturingResponse.close();
            capturingResponse = null;

            pdfOutputStream = response.getOutputStream();
            pdfRenderer.renderHtmlToPDF(htmlReader, true, pdfOutputStream, getBaseURL((HttpServletRequest) request));
        } catch (Exception e) {
            
        } finally {
            if (capturingResponse != null) {
                capturingResponse.close();
            }

            IOUtils.closeQuietly(pdfOutputStream);
            IOUtils.closeQuietly(htmlReader);
            IOUtils.closeQuietly(htmlInputStream);
        }
    }

    private String getBaseURL(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder(40);

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();

        if ("https".equals(scheme)) {
            if (port == 443) {
                sb.append(scheme).append("//").append(serverName).append('/');
            } else {
                sb.append(scheme).append("//").append(serverName).append(':').append(port).append('/');
            }
        } else {
            if (port == 80) {
                sb.append(scheme).append("//").append(serverName).append('/');
            } else {
                sb.append(scheme).append("//").append(serverName).append(':').append(port).append('/');
            }
        }

        return sb.toString();
    }
}
