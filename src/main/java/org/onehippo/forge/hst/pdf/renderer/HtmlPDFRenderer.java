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
package org.onehippo.forge.hst.pdf.renderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;

/**
 * HtmlPDFRenderer
 * <P>
 * This class is designed to be used as singleton object.
 * So, {@link #renderHtmlToPDF(InputStream, String, boolean, OutputStream)} and {@link #renderHtmlToPDF(Reader, boolean, OutputStream)} should be thread-safe.
 * </P>
 */
public class HtmlPDFRenderer {

    private static Logger log = LoggerFactory.getLogger(HtmlPDFRenderer.class);

    private boolean removeExistingCssLinks = true;
    private URI [] cssURIs;
    private int bufferSize = 4096;
    private UserAgentCallback userAgentCallback;
    private String [] fontPaths;
    private boolean useFullyQualifiedLinks = true;

    public HtmlPDFRenderer() {
    }

    public boolean isRemoveExistingCssLinks() {
        return removeExistingCssLinks;
    }

    public void setRemoveExistingCssLinks(boolean removeExistingCssLinks) {
        this.removeExistingCssLinks = removeExistingCssLinks;
    }

    public URI [] getCssURIs() {
        return cssURIs;
    }

    public void setCssURIs(URI [] cssURIs) {
        this.cssURIs = cssURIs;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public UserAgentCallback getUserAgentCallback() {
        return userAgentCallback;
    }

    public void setUserAgentCallback(UserAgentCallback userAgentCallback) {
        this.userAgentCallback = userAgentCallback;
    }

    public String [] getFontPaths() {
        return fontPaths;
    }

    public void setFontPaths(String [] fontPaths) {
        this.fontPaths = fontPaths;
    }

    public boolean isUseFullyQualifiedLinks() {
        return useFullyQualifiedLinks;
    }

    public void setUseFullyQualifiedLinks(boolean useFullyQualifiedLinks) {
        this.useFullyQualifiedLinks = useFullyQualifiedLinks;
    }

    public void renderHtmlToPDF(InputStream htmlInput, String inputHtmlEncoding, boolean convertToXHTML, OutputStream pdfOutput, String baseURL) throws IOException {
        InputStreamReader htmlReader = new InputStreamReader(htmlInput, inputHtmlEncoding);
        renderHtmlToPDF(htmlReader, convertToXHTML, pdfOutput, baseURL);
    }

    public void renderHtmlToPDF(Reader htmlInput, boolean convertToXHTML, OutputStream pdfOutput, String documentURL) throws IOException {
        Reader xhtmlReader = null;

        try {
            if (convertToXHTML) {
                xhtmlReader = convertHtmlReaderToXhtmlReader(htmlInput);
            } else {
                xhtmlReader = htmlInput;
            }

            ITextRenderer renderer = new ITextRenderer();

            if (fontPaths != null && fontPaths.length > 0) {
                ITextFontResolver fontResolver = renderer.getFontResolver();

                for (String fontPath : fontPaths) {
                    fontResolver.addFont(fontPath, true);
                }
            }

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(xhtmlReader));

            if (removeExistingCssLinks) {
                removeExistingCssLinks(document);
            }

            if (cssURIs != null && cssURIs.length > 0) {
                appendCssLinkElementToXhtmlDocument(document, cssURIs);
            }

            if (useFullyQualifiedLinks && !StringUtils.isEmpty(documentURL)) {
                replaceLinksByFullyQualifiedURLs(document, documentURL, "a");
                replaceLinksByFullyQualifiedURLs(document, documentURL, "A");
            }

            if (userAgentCallback != null) {
                renderer.getSharedContext().setUserAgentCallback(userAgentCallback);
            }

            renderer.setDocument(document, documentURL);
            renderer.layout();
            renderer.createPDF(pdfOutput);
        } catch (ParserConfigurationException e) {
            log.error("Parse configuration exception.", e);
        } catch (SAXException e) {
            log.error("XML parsing exception.", e);
        } catch (DocumentException e) {
            log.error("pdf generation exception.", e);
        } finally {
            if (xhtmlReader != htmlInput) {
                IOUtils.closeQuietly(xhtmlReader);
            }
        }
    }

    private Reader convertHtmlReaderToXhtmlReader(Reader htmlReader) throws IOException {
        Tidy tidy = new Tidy();

        tidy.setMakeClean(true);
        tidy.setXHTML(true);
        tidy.setDocType("omit");
        tidy.setNumEntities(true);
        tidy.setInputEncoding("UTF-8");
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);

        ByteArrayOutputStream tidyOut = null;
        OutputStreamWriter osw = null;
        byte [] bytes = null;

        try {
            tidyOut = new ByteArrayOutputStream(bufferSize);
            osw = new OutputStreamWriter(tidyOut, "UTF-8");
            tidy.parse(htmlReader, osw);
            osw.flush();
            bytes = tidyOut.toByteArray();
        } finally {
            IOUtils.closeQuietly(osw);
            IOUtils.closeQuietly(tidyOut);
        }

        return new InputStreamReader(new ByteArrayInputStream(bytes), "UTF-8");
    }

    private static Element getFirstChildElement(Element base, String nodeName) {
        NodeList childNodeList = base.getChildNodes();

        if (childNodeList != null) {
            int length = childNodeList.getLength();

            for (int i = 0; i < length; i++) {
                Node childNode = childNodeList.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (nodeName == null) {
                        return (Element) childNode;
                    } else if (StringUtils.equalsIgnoreCase(childNode.getNodeName(), nodeName)) {
                        return (Element) childNode;
                    }
                }
            }
        }

        return null;
    }

    private static void removeExistingCssLinks(Document document) {
        Element headElem = getFirstChildElement(document.getDocumentElement(), "head");

        if (headElem == null) {
            return;
        }

        NodeList nodeList = headElem.getChildNodes();

        if (nodeList != null) {
            int length = nodeList.getLength();

            for (int i = length - 1; i >= 0; i--) {
                Node childNode = nodeList.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElem = (Element) childNode;

                    if (StringUtils.equalsIgnoreCase("link", childElem.getNodeName())) {
                        if (StringUtils.equalsIgnoreCase("text/css", childElem.getAttribute("type"))) {
                            headElem.removeChild(childElem);
                        }
                    }
                }
            }
        }
    }

    private static void appendCssLinkElementToXhtmlDocument(Document document, URI [] cssURIs) {
        Element headElem = getFirstChildElement(document.getDocumentElement(), "head");

        if (headElem == null) {
            return;
        }

        for (URI cssURI : cssURIs) {
            Element linkElem = document.createElement("link");
            linkElem.setAttribute("type", "text/css");
            linkElem.setAttribute("rel", "stylesheet");
            linkElem.setAttribute("href", cssURI.toString());
            linkElem.setAttribute("media", "print");
            headElem.appendChild(linkElem);
        }
    }

    private static String getBaseServerURL(String documentURL) {
        URI documentURI = URI.create(documentURL);
        StringBuilder sb = new StringBuilder(40);
        String scheme = documentURI.getScheme();
        int port = documentURI.getPort();
        sb.append(scheme).append("://");
        sb.append(documentURI.getHost());

        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            sb.append(':').append(port);
        }

        return sb.toString();
    }

    private static void replaceLinksByFullyQualifiedURLs(Document document, String documentURL, String linkTagName) {
        String baseServerURL = getBaseServerURL(documentURL);

        NodeList linkList = document.getElementsByTagName(linkTagName);

        if (linkList != null) {
            int length = linkList.getLength();

            for (int i = 0; i < length; i++) {
                Node linkNode = linkList.item(i);

                if (linkNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element linkElem = (Element) linkNode;
                String href = StringUtils.trim(linkElem.getAttribute("href"));

                if (StringUtils.isEmpty(href)) {
                    href = StringUtils.trim(linkElem.getAttribute("HREF"));
                }

                if (StringUtils.isEmpty(href)) {
                    continue;
                }

                if (StringUtils.startsWith(href, "http:") || StringUtils.startsWith(href, "https:")) {
                    continue;
                }

                if (StringUtils.startsWith(href, "/")) {
                    linkElem.setAttribute("href", baseServerURL + href);
                } else {
                    String basePath = StringUtils.substringBeforeLast(documentURL, "/");
                    linkElem.setAttribute("href", basePath + "/" + href);
                }
            }
        }
    }
}
